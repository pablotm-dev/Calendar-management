package com.calendar_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication-related endpoints
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Check if the user is authenticated and return user info
     * 
     * @return User info if authenticated, or 401 if not
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            authentication instanceof OAuth2AuthenticationToken) {
            
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User user = oauthToken.getPrincipal();
            
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("name", user.getAttribute("name"));
            response.put("email", user.getAttribute("email"));
            response.put("picture", user.getAttribute("picture"));
            
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", false);
        response.put("loginUrl", "/oauth2/authorization/google");
        
        return ResponseEntity.status(401).body(response);
    }
}