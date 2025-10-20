package com.calendar_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class to verify HTTP configuration.
 * This test verifies that the application correctly responds to HTTP requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HttpConfigTest {

    @LocalServerPort
    private int port;

    /**
     * This test verifies that the API test endpoint is accessible via HTTP.
     * It attempts to access the /api-test endpoint and expects a 200 (OK) response.
     */
    @Test
    public void testHttpEndpoint() throws URISyntaxException {
        TestRestTemplate restTemplate = new TestRestTemplate();

        // Create HTTP URL for the test endpoint
        URI httpUri = new URI("http://localhost:" + port + "/api-test");

        // Send request and verify response
        ResponseEntity<Map> response = restTemplate.getForEntity(httpUri, Map.class);

        // Log the response
        System.out.println("[DEBUG_LOG] HTTP request resulted in status: " + response.getStatusCode());
        System.out.println("[DEBUG_LOG] Response body: " + response.getBody());

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().get("status"));
        assertEquals("API is working correctly", response.getBody().get("message"));
        assertEquals("HTTP", response.getBody().get("protocol"));
    }
}