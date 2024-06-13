package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.configuration.JwtUtilConfig;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.response.ResponseStructure;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share")
public class LinkedInPageController {
	
	@Autowired
	HttpServletRequest request;
	
	@Autowired
	ResponseStructure<String> structure;
	
	@Autowired
	JwtUtilConfig jwtUtilConfig;
	
	@Autowired
	QuantumShareUserDao userDao;
	
	
	
}
