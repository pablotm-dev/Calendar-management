package com.calendar_management.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import java.util.Map;

@RestController
public class DebugTokenController {

    @GetMapping("/debug/token")
    public Map<String, Object> token(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        return Map.of(
                "access_token", client.getAccessToken().getTokenValue(),
                "token_type", client.getAccessToken().getTokenType().getValue(),
                "expires_at", client.getAccessToken().getExpiresAt()
        );
    }
}
