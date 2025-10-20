package com.calendar_management.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple controller to test API connectivity.
 * This controller provides a basic endpoint that can be used to verify
 * that the application is correctly configured and accessible.
 */
@RestController
public class HttpsTestController {

    /**
     * Simple endpoint that returns a success message.
     * This can be used to test that the application is correctly
     * configured and accessible via HTTP.
     *
     * @return A map containing a success message
     */
    @GetMapping("/api-test")
    public Map<String, String> testApi() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "API is working correctly");
        response.put("protocol", "HTTP");
        return response;
    }
}
