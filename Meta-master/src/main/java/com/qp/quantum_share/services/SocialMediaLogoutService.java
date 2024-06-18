package com.qp.quantum_share.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dao.FaceBookPageDao;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.InstagramUserDao;
import com.qp.quantum_share.dao.LinkedInPageDao;
import com.qp.quantum_share.dao.LinkedInProfileDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.SocialAccountDao;
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.response.ResponseStructure;

import jakarta.transaction.Transactional;

@Service
public class SocialMediaLogoutService {

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	FaceBookPageDao pageDao;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SocialAccountDao accountDao;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	InstagramUserDao instagramUserDao;
	
	@Autowired
	LinkedInProfileDao linkedInProfileDao;
	
	@Autowired
	LinkedInPageDao linkedInPageDao;
	
	@Autowired
	SocialAccountDao socialAccountDao;

	public ResponseEntity<ResponseStructure<String>> disconnectFacebook(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getFacebookUser() == null) {
			structure.setCode(404); // Or a custom code for Facebook not linked
			structure.setMessage("Facebook account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("facebook");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		FaceBookUser deleteUser = accounts.getFacebookUser();
		List<FacebookPageDetails> pages = accounts.getFacebookUser().getPageDetails();
		System.out.println("1 "+pages);
		accounts.getFacebookUser().setPageDetails(null);
		accounts.setFacebookUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);
		System.out.println("2 "+pages);
		facebookUserDao.deleteFbUser(deleteUser);
		pageDao.deletePage(pages);

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Facebook Disconnected Successfully");
		structure.setPlatform("facebook");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> disconnectInstagram(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getInstagramUser() == null) {
			structure.setCode(404);
			structure.setMessage("Instagram account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("instagram");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		InstagramUser deleteUser = accounts.getInstagramUser();
		accounts.setInstagramUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		instagramUserDao.deleteUser(deleteUser);

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Instagram Disconnected Successfully");
		structure.setPlatform("instagram");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<String>> disconnectLinkedIn(QuantumShareUser user) {
		
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getLinkedInProfileDto() == null) {
			structure.setCode(404);
			structure.setMessage("LinkedIn account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("LinkedIn");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}
		LinkedInProfileDto deleteUser = accounts.getLinkedInProfileDto();
		accounts.setLinkedInProfileDto(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);
		
		linkedInProfileDao.deleteUser(deleteUser);
		
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("LinkedIn Profile Disconnected Successfully");
		structure.setPlatform("LinkedIn");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	@Transactional
	public ResponseEntity<ResponseStructure<String>> disconnectLinkedInPage(QuantumShareUser user) { 
	    // Retrieve the user's LinkedIn profile
	    SocialAccounts socialAccounts = user.getSocialAccounts();
	    if (socialAccounts == null) {
	        ResponseStructure<String> response = new ResponseStructure<>();
	        response.setCode(HttpStatus.BAD_REQUEST.value());
	        response.setMessage("No social accounts found for user");
	        response.setStatus("error");
	        response.setPlatform("LinkedIn");
	        response.setData(null);
	        return ResponseEntity.badRequest().body(response);
	    }

	    LinkedInProfileDto linkedInProfileDto = socialAccounts.getLinkedInProfileDto();
	    if (linkedInProfileDto == null) {
	        ResponseStructure<String> response = new ResponseStructure<>();
	        response.setCode(HttpStatus.BAD_REQUEST.value());
	        response.setMessage("No LinkedIn profile found for user");
	        response.setStatus("error");
	        response.setPlatform("LinkedIn");
	        response.setData(null);
	        return ResponseEntity.badRequest().body(response);
	    }

	    // Check if the LinkedIn profile has any pages
	    List<LinkedInPageDto> pages = linkedInProfileDto.getPages();
	    if (pages.isEmpty()) {
	        ResponseStructure<String> response = new ResponseStructure<>();
	        response.setCode(HttpStatus.BAD_REQUEST.value());
	        response.setMessage("No LinkedIn pages found for user");
	        response.setStatus("error");
	        response.setPlatform("LinkedIn");
	        response.setData(null);
	        return ResponseEntity.badRequest().body(response);
	    }

	    // Assuming here that you may have logic to select the appropriate page to disconnect,
	    // or you may disconnect all pages associated with the user.
	    // For this example, let's disconnect the first page.
	    LinkedInPageDto linkedInPageDto = pages.get(0);

	    // Remove the LinkedIn page from the profile
	    linkedInProfileDto.getPages().remove(linkedInPageDto);

	    // Remove the LinkedIn page from the database
	    linkedInPageDao.deletePage(linkedInPageDto);

	    // Check if the LinkedIn profile has any more pages
	    if (linkedInProfileDto.getPages().isEmpty()) {
	        // Remove the LinkedIn profile from the social accounts
	        socialAccounts.setLinkedInProfileDto(null);
	        linkedInProfileDao.deleteUser(linkedInProfileDto);
	        
	        // Remove the social accounts reference
	        user.setSocialAccounts(null);
	        
	        // Delete the social accounts from the database
	        socialAccountDao.deleteSocialAccount(socialAccounts);
	    }

	    // Save the updated user information
	    userDao.save(user);

	    // Prepare the response
	    ResponseStructure<String> response = new ResponseStructure<>();
	    response.setCode(HttpStatus.OK.value());
	    response.setMessage("LinkedIn Page Disconnected Successfully");
	    response.setStatus("success");
	    response.setPlatform("LinkedIn");
	    
	    // Set the disconnected page name in the response data
	    response.setData("Page '" + linkedInPageDto.getLinkedinPageName() + "' disconnected successfully");

	    return ResponseEntity.ok(response);
	}
}
