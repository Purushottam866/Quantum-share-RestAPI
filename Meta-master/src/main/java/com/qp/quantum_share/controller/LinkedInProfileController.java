package com.qp.quantum_share.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.LinkedInProfileService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share")
@CrossOrigin(origins = "*")
public class LinkedInProfileController {

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	ResponseStructure<String> structure;
	
	@Autowired
	JwtUtilConfig jwtUtilConfig;
	
	@Autowired
	QuantumShareUserDao userDao;
	
	@Autowired
	LinkedInProfileService linkedInProfileService;
	
	@Value("${linkedin.clientId}")
    private String clientId;

//    @Value("${linkedin.clientSecret}")
//    private String clientSecret;

    @Value("${linkedin.redirectUri}")
    private String redirectUri;

    @Value("${linkedin.scope}")
    private String scope;
	
//	 @GetMapping("/connect/linkedin")
//	    public ResponseEntity<ResponseStructure<String>> login() {
//		 
//	        String token = request.getHeader("Authorization");
//	        if (token == null || !token.startsWith("Bearer ")) {
//	            // User is not authenticated or authorized
//	            // Customize the error response
//	            structure.setCode(115);
//	            structure.setMessage("Missing or invalid authorization token");
//	            structure.setStatus("error");
//	            structure.setPlatform(null);
//	            structure.setData(null);
//	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	                                 .body(structure);
//	        }
//
//	        String jwtToken = token.substring(7); // remove "Bearer " prefix
//			String userId = jwtUtilConfig.extractUserId(jwtToken);
//			QuantumShareUser user = userDao.fetchUser(userId);
//	        
//			if (user == null) {
//				structure.setCode(HttpStatus.NOT_FOUND.value());
//				structure.setMessage("user doesn't exists, please signup");
//				structure.setStatus("error");
//				structure.setData(null);
//				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
//			}
//			
//	        // User is authenticated and authorized
//	        // Generate the authorization URL and return a redirect response
//	        String authorizationUrl = linkedInProfileService.generateAuthorizationUrl();
//	        return ResponseEntity.status(HttpStatus.FOUND)
//	                             .header("Location", authorizationUrl)
//	                             .build();
//	    }
	
	@GetMapping("/connect/linkedin")
	public ResponseEntity<Map<String, String>> getLinkedInAuthUrl() {
	    String token = request.getHeader("Authorization");
	    Map<String, String> authUrlParams = new HashMap<>();
	    if (token == null || !token.startsWith("Bearer ")) {
	        // User is not authenticated or authorized
	        // Customize the error response
	        authUrlParams.put("status", "error");
	        authUrlParams.put("code", "115");
	        authUrlParams.put("message", "Missing or invalid authorization token");
	        authUrlParams.put("platform", null);
	        authUrlParams.put("data", null);
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                             .body(authUrlParams);
	    }

	    String jwtToken = token.substring(7); // remove "Bearer " prefix
	    String userId = jwtUtilConfig.extractUserId(jwtToken);
	    QuantumShareUser user = userDao.fetchUser(userId);

	    if (user == null) {
	        // User is not found
	        // Customize the error response
	        authUrlParams.put("status", "error");
	        authUrlParams.put("code", String.valueOf(HttpStatus.NOT_FOUND.value()));
	        authUrlParams.put("message", "user doesn't exist, please sign up");
	        authUrlParams.put("platform", null);
	        authUrlParams.put("data", null);
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                             .body(authUrlParams);
	    }
	    
	    // User is authenticated and authorized
	    // Generate the authorization URL and return a redirect response
	    Map<String, String> authUrlParamsBody = getLinkedInAuth().getBody();
	    if (authUrlParamsBody != null) {
	        authUrlParams.putAll(authUrlParamsBody);
	    }
	    authUrlParams.put("status", "success");
	    return ResponseEntity.ok(authUrlParams);
	}
	
	public ResponseEntity<Map<String, String>> getLinkedInAuth() {
        Map<String, String> authUrlParams = new HashMap<>();
        authUrlParams.put("clientId", clientId);
        authUrlParams.put("redirectUri", redirectUri);
        authUrlParams.put("scope", scope);
        return ResponseEntity.ok(authUrlParams);
    }


   
//    @GetMapping("/callback/success")
//    public ResponseEntity<?> callbackEndpoint(@RequestParam("code") String code, @RequestParam("type") String type) throws IOException {
//        System.out.println("code = " + code);
//        String token = request.getHeader("Authorization");
//        if (token == null || !token.startsWith("Bearer ")) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                                 .body(createErrorStructure(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token"));
//        }
//
//        String jwtToken = token.substring(7); // remove "Bearer " prefix
//        String userId = jwtUtilConfig.extractUserId(jwtToken);
//        QuantumShareUser user = userDao.fetchUser(userId);
//
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                                 .body(createErrorStructure(HttpStatus.NOT_FOUND, "User doesn't exist, please sign up"));
//        }
//
//        if (code == null) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                                 .body(createErrorStructure(HttpStatus.BAD_REQUEST, "Please accept all the permissions while logging in"));
//        }
//
//        System.out.println("code = " + code);
//
//        if ("profile".equals(type)) {
//            return linkedInProfileService.getUserInfoWithToken(code, user);
//        } else if ("page".equals(type)) {
//            return linkedInProfileService.getOrganizationsDetailsByProfile(code, user);
//        } else {
//            // Handle unknown type
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                                 .body(createErrorStructure(HttpStatus.BAD_REQUEST, "Unknown connection type"));
//        }
//    }
//
//    private ResponseStructure<?> createErrorStructure(HttpStatus status, String message) {
//        ResponseStructure<?> structure = new ResponseStructure<>();
//        structure.setCode(status.value());
//        structure.setMessage(message);
//        structure.setStatus("error");
//        structure.setPlatform("linkedin");
//        structure.setData(null);
//        return structure;
//    }

	
	
	@PostMapping("/callback/success")
    public ResponseEntity<?> callbackEndpoint(@RequestParam("code") String code, @RequestParam("type") String type) throws IOException {
        System.out.println("code = " + code);
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(createErrorStructure(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token"));
        }

        String jwtToken = token.substring(7); // remove "Bearer " prefix
        String userId = jwtUtilConfig.extractUserId(jwtToken);
        QuantumShareUser user = userDao.fetchUser(userId);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(createErrorStructure(HttpStatus.NOT_FOUND, "User doesn't exist, please sign up"));
        }

        if (code == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(createErrorStructure(HttpStatus.BAD_REQUEST, "Please accept all the permissions while logging in"));
        }

        System.out.println("code = " + code);

        if ("profile".equals(type)) {
            return linkedInProfileService.getUserInfoWithToken(code, user);
        } else if ("page".equals(type)) {
            return linkedInProfileService.getOrganizationsDetailsByProfile(code, user);
        } else {
            // Handle unknown type
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(createErrorStructure(HttpStatus.BAD_REQUEST, "Unknown connection type"));
        }
    }

    private ResponseStructure<?> createErrorStructure(HttpStatus status, String message) {
        ResponseStructure<?> structure = new ResponseStructure<>();
        structure.setCode(status.value());
        structure.setMessage(message);
        structure.setStatus("error");
        structure.setPlatform("linkedin");
        structure.setData(null);
        return structure;
    }

    @PostMapping("/save-selected-page")
    public ResponseEntity<ResponseStructure<String>> saveSelectedPage(@RequestBody LinkedInPageDto selectedLinkedInPageDto) {
    	String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			structure.setCode(115);
			structure.setMessage("Missing or invalid authorization token");
			structure.setStatus("error");
			structure.setPlatform(null);
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
		}
		String jwtToken = token.substring(7); // remove "Bearer " prefix
		String userId = jwtUtilConfig.extractUserId(jwtToken);
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		
    	return linkedInProfileService.saveSelectedPage(selectedLinkedInPageDto, user);
    }
    
}
