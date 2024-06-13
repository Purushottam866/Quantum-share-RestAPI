package com.qp.quantum_share.services;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.dao.LinkedInPageDao;
import com.qp.quantum_share.dao.LinkedInProfileDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class LinkedInProfileService {

	@Value("${linkedin.clientId}")
    private String clientId;

    @Value("${linkedin.clientSecret}")
    private String clientSecret;

    @Value("${linkedin.redirectUri}")
    private String redirectUri;

    @Value("${linkedin.scope}")
    private String scope;
    
    @Autowired
    LinkedInProfileDto linkedInProfileDto;
    
    @Autowired
    LinkedInProfileDao linkedInProfileDao;
    
    @Autowired
    LinkedInPageDto linkedInPageDto;
    
    @Autowired
    LinkedInPageDao linkedInPageDao;
    
     @Autowired
	ResponseStructure<String> structure;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    LinkedInPageServcice linkedInPageServcice;

    HttpHeaders httpHeaders = new HttpHeaders();
   
    @Autowired
	QuantumShareUserDao userDao;
    
    public String generateAuthorizationUrl() {
        return "https://www.linkedin.com/oauth/v2/authorization" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=" + scope;
    }
    
    public String exchangeAuthorizationCodeForAccessToken(String code) throws IOException {
        String accessTokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";
        String params = "grant_type=authorization_code" +
                "&code=" + code + 
                "&redirect_uri=" + redirectUri +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret;

        URL url = new URL(accessTokenUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(params);
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String response = parseResponse(connection);
            return parseAccessToken(response);
        } else {
            return null;
        }
    }
    
    private String parseResponse(HttpURLConnection connection) throws IOException {
        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    private String parseAccessToken(String response) throws IOException {
        return new ObjectMapper().readTree(response).get("access_token").asText();
    }

    public ResponseEntity<ResponseStructure<String>> getUserInfoWithToken(String code,QuantumShareUser user) throws IOException {
        String accessToken = exchangeAuthorizationCodeForAccessToken(code);

        if (accessToken == null) {
            structure.setCode(500);
            structure.setMessage("Failed to retrieve access token");
            structure.setStatus("error");
            structure.setPlatform("LinkedIn");
            structure.setData(null);
            return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ResponseEntity<String> organizationAclsResponse = getProfileInfo(accessToken,user);
        
        

        if (organizationAclsResponse.getStatusCode() == HttpStatus.OK) {
            structure.setCode(200);
            structure.setMessage("Success");
            structure.setStatus("success");
            structure.setPlatform("LinkedIn");
            structure.setData(organizationAclsResponse.getBody());
            return new ResponseEntity<>(structure, HttpStatus.OK);
        } else {
            structure.setCode(organizationAclsResponse.getStatusCode().value());
            structure.setMessage("Failed to retrieve organization info");
            structure.setStatus("error");
            structure.setPlatform("LinkedIn");
            structure.setData(null);
            return new ResponseEntity<>(structure, organizationAclsResponse.getStatusCode());
        }
    }

    

	public ResponseEntity<String> getProfileInfo(String accessToken,QuantumShareUser user) {
        String userInfoUrl = "https://api.linkedin.com/v2/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String responseBody = response.getBody();

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                String localizedFirstName = rootNode.path("localizedFirstName").asText();
                String localizedLastName = rootNode.path("localizedLastName").asText();
                String id = rootNode.path("id").asText();

                String username = localizedFirstName + " " + localizedLastName;
                String customResponse = "{ \"profile_sub\": \"" + id + "\", \"LinkedInUsername\": \"" + username + "\", \"access_token\": \"" + accessToken + "\" }";

                LinkedInProfileDto linkedInProfileDto = new LinkedInProfileDto();
                linkedInProfileDto.setLinkedinProfileURN(id);
                linkedInProfileDto.setLinkedinProfileUserName(username);
                linkedInProfileDto.setLinkedinProfileAccessToken(accessToken);

                //linkedInProfileDao.saveProfile(linkedInProfileDto);
                
                SocialAccounts socialAccounts = user.getSocialAccounts();
                if (socialAccounts == null) {
                    socialAccounts = new SocialAccounts();
                    socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
                    user.setSocialAccounts(socialAccounts);
                } else {
                    if (socialAccounts.getLinkedInProfileDto() == null) {
                        socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
                    }
                } 
                
                userDao.saveUser(user);

                HttpHeaders customHeaders = new HttpHeaders();
                customHeaders.setContentType(MediaType.APPLICATION_JSON);

                return new ResponseEntity<>(customResponse, customHeaders, HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the request: " + e.getMessage());
            }
        } else {
            return response;
        }
    }
	
	
	 public ResponseEntity<ResponseStructure<List<LinkedInPageDto>>> getOrganizationsDetailsByProfile(String code, QuantumShareUser user) throws IOException {
	        String accessToken = exchangeAuthorizationCodeForAccessToken(code);
	        return getOrganizationInfo(accessToken);
	    }
  
	 public ResponseEntity<ResponseStructure<List<LinkedInPageDto>>> getOrganizationInfo(String accessToken) {
		    System.out.println("AccessToken = " + accessToken);
		    HttpHeaders headers = new HttpHeaders();
		    headers.set("X-Restli-Protocol-Version", "2.0.0");
		    headers.set("Authorization", "Bearer " + accessToken);
		    HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

		    ResponseEntity<String> responseEntity;

		    try {
		        responseEntity = restTemplate.exchange(
		            "https://api.linkedin.com/v2/organizationAcls?q=roleAssignee",
		            HttpMethod.GET,
		            requestEntity,
		            String.class
		        );
		    } catch (Exception e) {
		        ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		        structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		        structure.setMessage("Failed to make request");
		        structure.setStatus("error");
		        structure.setPlatform("LinkedIn");
		        structure.setData(null);
		        return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		    }

		    if (responseEntity.getStatusCode() == HttpStatus.OK) {
		        String responseBody = responseEntity.getBody();
		        ObjectMapper objectMapper = new ObjectMapper();
		        try {
		            JsonNode rootNode = objectMapper.readTree(responseBody);
		            JsonNode elementsNode = rootNode.path("elements");

		            if (elementsNode.isEmpty()) {
		                ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		                structure.setCode(HttpStatus.OK.value());
		                structure.setMessage("User does not have associated pages");
		                structure.setStatus("success");
		                structure.setPlatform("LinkedIn");
		                structure.setData(null);
		                return ResponseEntity.ok(structure);
		            } else {
		                List<String> organizationUrns = new ArrayList<>();
		                for (JsonNode pageNode : elementsNode) {
		                    String organizationURN = pageNode.path("organization").asText();
		                    organizationUrns.add(organizationURN);
		                }

		                // Get the organization names
		                List<String> organizationNames = getPageNamesFromLinkedInAPI(accessToken, organizationUrns);

		                // Prepare LinkedInPageDto objects
		                List<LinkedInPageDto> data = new ArrayList<>();
		                for (int i = 0; i < organizationUrns.size(); i++) {
		                    LinkedInPageDto linkedInPageDto = new LinkedInPageDto();
		                    linkedInPageDto.setLinkedinPageURN(organizationUrns.get(i));
		                    linkedInPageDto.setLinkedinPageAccessToken(accessToken);
		                    linkedInPageDto.setLinkedinPageName(organizationNames.get(i));
		                    data.add(linkedInPageDto);
		                }

		                // Prepare response structure
		                ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		                structure.setCode(HttpStatus.OK.value());
		                structure.setMessage("User has associated pages");
		                structure.setStatus("success");
		                structure.setPlatform("LinkedIn");
		                structure.setData(data);
		                return ResponseEntity.ok(structure);
		            }
		        } catch (JsonProcessingException e) {
		            ResponseStructure<List<LinkedInPageDto>> errorResponse = new ResponseStructure<>();
		            errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		            errorResponse.setMessage("Failed to process JSON response");
		            errorResponse.setStatus("error");
		            errorResponse.setPlatform("LinkedIn");
		            errorResponse.setData(null);
		            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		        }
		    } else {
		        ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		        structure.setCode(responseEntity.getStatusCode().value());
		        structure.setMessage("Failed to retrieve organization info");
		        structure.setStatus("error");
		        structure.setPlatform("LinkedIn");
		        structure.setData(null);
		        return new ResponseEntity<>(structure, responseEntity.getStatusCode());
		    }
		}



	 private List<String> getPageNamesFromLinkedInAPI(String accessToken, List<String> organizationUrns) {
		    List<String> pageNames = new ArrayList<>();

		    HttpHeaders headers = new HttpHeaders();
		    headers.set("Authorization", "Bearer " + accessToken);
		    HttpEntity<String> entity = new HttpEntity<>(headers);

		    for (String organizationUrn : organizationUrns) {
		        String organizationId = organizationUrn.substring(organizationUrn.lastIndexOf(':') + 1);
		        
		        try {
		            ResponseEntity<String> responseEntity = restTemplate.exchange(
		                    "https://api.linkedin.com/v2/organizations/" + organizationId,
		                    HttpMethod.GET,
		                    entity,
		                    String.class
		            );

		            if (responseEntity.getStatusCode() == HttpStatus.OK) {
		                ObjectMapper objectMapper = new ObjectMapper();
		                JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());
		                String pageName = rootNode.path("localizedName").asText();
		                pageNames.add(pageName);
		            } else {
		                pageNames.add(null); // Add null for failed requests
		            }
		        } catch (Exception e) {
		            pageNames.add(null); // Add null for exceptions
		            e.printStackTrace();
		        }
		    }

		    return pageNames;
		}

	 public ResponseEntity<ResponseStructure<String>> saveSelectedPage(LinkedInPageDto selectedLinkedInPageDto, QuantumShareUser user) {
		    // Check if the user is authenticated
		    if (user == null) {
		        ResponseStructure<String> response = new ResponseStructure<>();
		        response.setCode(HttpStatus.UNAUTHORIZED.value());
		        response.setMessage("User not authenticated");
		        response.setStatus("error");
		        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		    }

		    // Check if the selected page DTO is valid
		    if (selectedLinkedInPageDto == null || selectedLinkedInPageDto.getLinkedinPageURN() == null || selectedLinkedInPageDto.getLinkedinPageName() == null || selectedLinkedInPageDto.getLinkedinPageAccessToken() == null) {
		        ResponseStructure<String> response = new ResponseStructure<>();
		        response.setCode(HttpStatus.BAD_REQUEST.value());
		        response.setMessage("Invalid selected page data");
		        response.setStatus("error");
		        return ResponseEntity.badRequest().body(response);
		    }

		    // Create a new social accounts object if it doesn't exist
		    SocialAccounts socialAccounts = user.getSocialAccounts();
		    if (socialAccounts == null) {
		        socialAccounts = new SocialAccounts();
		    }
		    
		    // Retrieve or create the LinkedIn profile DTO
		    LinkedInProfileDto linkedInProfileDto = socialAccounts.getLinkedInProfileDto();
		    if (linkedInProfileDto == null) {
		        linkedInProfileDto = new LinkedInProfileDto();
		        socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
		    }
		    
		    // Save the LinkedIn page
		    selectedLinkedInPageDto = linkedInPageDao.save(selectedLinkedInPageDto);

		    // Extract only the value from the URN
		    String organizationId = selectedLinkedInPageDto.getLinkedinPageURN().substring(selectedLinkedInPageDto.getLinkedinPageURN().lastIndexOf(':') + 1);

		    // Set the extracted organization ID back to the DTO
		    selectedLinkedInPageDto.setLinkedinPageURN(organizationId);

		    // Associate the selected page with the LinkedIn profile
		    selectedLinkedInPageDto.setProfile(linkedInProfileDto);

		    // Add the selected page to the profile's list of pages
		    List<LinkedInPageDto> pages = new ArrayList<>();
		    pages.add(selectedLinkedInPageDto);
		    linkedInProfileDto.setPages(pages);

		    // Update the user's social accounts
		    user.setSocialAccounts(socialAccounts);
		    
		    // Save the user
		    userDao.saveUser(user);
		    
		    // Prepare the response
		    ResponseStructure<String> response = new ResponseStructure<>();
		    response.setCode(HttpStatus.OK.value());
		    response.setMessage("Selected page saved successfully");
		    response.setStatus("success"); 
		    response.setPlatform("LinkedIn");
		    response.setData(selectedLinkedInPageDto.getLinkedinPageName()); // You can change this to any other relevant data you want to return

		    return ResponseEntity.ok(response);
		}
} 
