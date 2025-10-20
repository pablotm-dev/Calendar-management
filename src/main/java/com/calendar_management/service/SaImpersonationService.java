package com.calendar_management.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Service for impersonating users with a service account
 * Only active when the "calendar" profile is active
 */
@Service
@Profile("calendar")
public class SaImpersonationService {

    private static final Logger logger = LoggerFactory.getLogger(SaImpersonationService.class);

    private final ResourceLoader resourceLoader;

    public SaImpersonationService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Value("${app.google.sa.key-file:classpath:service-account.json}")
    private String keyLocation;

    @Value("${GOOGLE_SERVICE_ACCOUNT_JSON:#{null}}")
    private String serviceAccountJson;

    private ServiceAccountCredentials baseCreds;

    /**
     * Initialize credentials from a JSON string
     * @param jsonContent The JSON content as a string
     * @throws Exception If initialization fails
     */
    private void initFromJsonString(String jsonContent) throws Exception {
        // Validate JSON content
        if (!jsonContent.contains("\"private_key\"") || !jsonContent.contains("\"client_email\"")) {
            throw new IllegalStateException("Service account JSON is missing required fields");
        }

        // Ensure the private key is properly formatted
        jsonContent = ensureProperPrivateKeyFormat(jsonContent);

        // Log the client email from the JSON content for debugging
        int clientEmailStart = jsonContent.indexOf("\"client_email\":");
        if (clientEmailStart != -1) {
            int valueStart = jsonContent.indexOf("\"", clientEmailStart + 15) + 1;
            int valueEnd = jsonContent.indexOf("\"", valueStart);
            if (valueStart != -1 && valueEnd != -1) {
                String clientEmail = jsonContent.substring(valueStart, valueEnd);
                logger.info("Service account client email from JSON: {}", clientEmail);
            }
        }

        // Create credentials from the validated JSON content
        try (InputStream jsonStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8))) {
            // Create credentials from JSON
            this.baseCreds = ServiceAccountCredentials.fromStream(jsonStream);
            logger.info("Created service account credentials from JSON: {}", this.baseCreds.getClientEmail());

            // Verify the private key exists
            if (this.baseCreds.getPrivateKey() == null) {
                logger.error("Service account private key is null!");
                throw new IllegalStateException("Service account private key is null");
            }
            logger.debug("Private key algorithm: {}", this.baseCreds.getPrivateKey().getAlgorithm());

            // Set explicit scopes
            this.baseCreds = (ServiceAccountCredentials) this.baseCreds.createScoped(
                Collections.singletonList(CalendarScopes.CALENDAR_READONLY)
            );
            logger.info("Added CALENDAR_READONLY scope to service account credentials");

            // Try to refresh the token to verify credentials are valid
            try {
                this.baseCreds.refresh();
                logger.info("Successfully refreshed service account token");
            } catch (IOException e) {
                logger.warn("Failed to refresh service account token: {}", e.getMessage());
                // Continue anyway, as we'll retry with delegated credentials
            }

            logger.info("Successfully initialized service account: {}", this.baseCreds.getClientEmail());
        } catch (Exception e) {
            logger.error("Failed to initialize service account credentials", e);
            throw new IllegalStateException("Failed to initialize service account credentials: " + e.getMessage(), e);
        }
    }

    @PostConstruct
    void init() throws Exception {
        // First check if we have the service account JSON in an environment variable
        if (serviceAccountJson != null && !serviceAccountJson.trim().isEmpty()) {
            logger.info("Initializing service account credentials from environment variable");
            try {
                initFromJsonString(serviceAccountJson);
                return;
            } catch (Exception e) {
                logger.warn("Failed to initialize from environment variable, falling back to file: {}", e.getMessage());
                // Fall back to file-based approach
            }
        }

        // Fall back to file-based approach
        logger.info("Initializing service account credentials from file: {}", keyLocation);

        String loc = keyLocation;
        if (!loc.startsWith("classpath:") && !loc.startsWith("file:")) {
            // assume filesystem se não tiver esquema explícito
            loc = "file:" + loc;
        }

        Resource keyRes = resourceLoader.getResource(loc);
        if (!keyRes.exists()) {
            throw new IllegalStateException("Service account key não encontrada em: " + loc);
        }

        try {
            // Read the JSON content as a string
            String jsonContent;
            try (InputStream in = keyRes.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                jsonContent = new String(bytes, StandardCharsets.UTF_8);
            }

            // Validate JSON content
            if (!jsonContent.contains("\"private_key\"") || !jsonContent.contains("\"client_email\"")) {
                throw new IllegalStateException("Service account key file is missing required fields");
            }

            // Ensure the private key is properly formatted
            jsonContent = ensureProperPrivateKeyFormat(jsonContent);

            // Log the client email from the JSON content for debugging
            int clientEmailStart = jsonContent.indexOf("\"client_email\":");
            if (clientEmailStart != -1) {
                int valueStart = jsonContent.indexOf("\"", clientEmailStart + 15) + 1;
                int valueEnd = jsonContent.indexOf("\"", valueStart);
                if (valueStart != -1 && valueEnd != -1) {
                    String clientEmail = jsonContent.substring(valueStart, valueEnd);
                    logger.info("Service account client email from JSON: {}", clientEmail);
                }
            }

            // Create credentials from the validated JSON content
            try (InputStream jsonStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8))) {
                // Create credentials from JSON
                this.baseCreds = ServiceAccountCredentials.fromStream(jsonStream);
                logger.info("Created service account credentials from JSON: {}", this.baseCreds.getClientEmail());

                // Verify the private key exists
                if (this.baseCreds.getPrivateKey() == null) {
                    logger.error("Service account private key is null!");
                    throw new IllegalStateException("Service account private key is null");
                }
                logger.debug("Private key algorithm: {}", this.baseCreds.getPrivateKey().getAlgorithm());

                // Set explicit scopes
                this.baseCreds = (ServiceAccountCredentials) this.baseCreds.createScoped(
                    Collections.singletonList(CalendarScopes.CALENDAR_READONLY)
                );
                logger.info("Added CALENDAR_READONLY scope to service account credentials");

                // Try to refresh the token to verify credentials are valid
                try {
                    this.baseCreds.refresh();
                    logger.info("Successfully refreshed service account token");
                } catch (IOException e) {
                    logger.warn("Failed to refresh service account token: {}", e.getMessage());
                    // Continue anyway, as we'll retry with delegated credentials
                }

                logger.info("Successfully initialized service account: {}", this.baseCreds.getClientEmail());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize service account credentials", e);
            throw new IllegalStateException("Failed to initialize service account credentials: " + e.getMessage(), e);
        }
    }

    /**
     * Ensures the private key in the JSON content is properly formatted.
     * This method handles potential issues with line breaks in the private key.
     */
    private String ensureProperPrivateKeyFormat(String jsonContent) {
        // Check if the private key contains proper line breaks
        if (jsonContent.contains("\\n") && !jsonContent.contains("\n-----")) {
            // The key already has encoded line breaks, which is good
            logger.debug("Private key already has encoded line breaks");
            return jsonContent;
        }

        logger.info("Reformatting private key to ensure proper encoding");

        // If we're here, we might need to fix the format
        int startIndex = jsonContent.indexOf("\"private_key\":");
        if (startIndex != -1) {
            int valueStart = jsonContent.indexOf("\"", startIndex + 14) + 1;
            int valueEnd = jsonContent.indexOf("\"", valueStart);
            if (valueStart != -1 && valueEnd != -1) {
                String privateKey = jsonContent.substring(valueStart, valueEnd);
                logger.debug("Extracted private key, length: {}", privateKey.length());

                // Handle different line break formats
                String fixedKey = privateKey;

                // Replace literal line breaks with encoded \n
                fixedKey = fixedKey.replace("\r\n", "\\n").replace("\n", "\\n");
                logger.debug("Replaced literal line breaks");

                // Handle the case where the key might already have some encoded line breaks
                fixedKey = fixedKey.replace("\\\\n", "\\n");
                logger.debug("Normalized double-encoded line breaks");

                // Ensure BEGIN and END markers are properly formatted
                fixedKey = fixedKey.replace("-----BEGIN PRIVATE KEY-----\\n", "-----BEGIN PRIVATE KEY-----\\n");
                fixedKey = fixedKey.replace("\\n-----END PRIVATE KEY-----", "\\n-----END PRIVATE KEY-----");
                logger.debug("Ensured BEGIN/END markers are properly formatted");

                // Ensure there are no double encoded line breaks
                int replacements = 0;
                while (fixedKey.contains("\\n\\n")) {
                    fixedKey = fixedKey.replace("\\n\\n", "\\n");
                    replacements++;
                }
                if (replacements > 0) {
                    logger.debug("Removed {} instances of double line breaks", replacements);
                }

                // Check if the key starts and ends correctly
                if (!fixedKey.startsWith("-----BEGIN PRIVATE KEY-----")) {
                    logger.warn("Private key does not start with the correct marker");
                }
                if (!fixedKey.endsWith("-----END PRIVATE KEY-----")) {
                    logger.warn("Private key does not end with the correct marker");
                }

                logger.info("Private key format processed. Original length: {}, New length: {}", 
                        privateKey.length(), fixedKey.length());

                // Reconstruct the JSON
                return jsonContent.substring(0, valueStart) + fixedKey + jsonContent.substring(valueEnd);
            } else {
                logger.warn("Could not locate private key value boundaries in JSON");
            }
        } else {
            logger.warn("Could not locate private_key field in JSON");
        }
        return jsonContent;
    }

    public Calendar calendarAs(String userEmail) {
        try {
            // Normalize email (lowercase and trim)
            userEmail = userEmail.toLowerCase().trim();
            logger.info("Creating delegated credentials for user: {}", userEmail);

            // Create a fresh copy of credentials for this user to avoid any caching issues
            ServiceAccountCredentials freshCreds;
            try {
                String loc = keyLocation;
                if (!loc.startsWith("classpath:") && !loc.startsWith("file:")) {
                    loc = "file:" + loc;
                }

                Resource keyRes = resourceLoader.getResource(loc);
                if (!keyRes.exists()) {
                    throw new IllegalStateException("Service account key not found at: " + loc);
                }

                // Read and process the JSON content
                String jsonContent;
                try (InputStream in = keyRes.getInputStream()) {
                    byte[] bytes = in.readAllBytes();
                    jsonContent = new String(bytes, StandardCharsets.UTF_8);
                    logger.debug("Read service account key file, size: {} bytes", bytes.length);
                }

                // Ensure proper formatting
                String originalJson = jsonContent;
                jsonContent = ensureProperPrivateKeyFormat(jsonContent);
                boolean jsonChanged = !originalJson.equals(jsonContent);
                logger.debug("Private key formatting applied: {}", jsonChanged ? "yes, changes made" : "no changes needed");

                // Extract client email for verification
                String clientEmail = null;
                int clientEmailStart = jsonContent.indexOf("\"client_email\":");
                if (clientEmailStart != -1) {
                    int valueStart = jsonContent.indexOf("\"", clientEmailStart + 15) + 1;
                    int valueEnd = jsonContent.indexOf("\"", valueStart);
                    if (valueStart != -1 && valueEnd != -1) {
                        clientEmail = jsonContent.substring(valueStart, valueEnd);
                        logger.debug("Using service account: {}", clientEmail);
                    }
                }

                // Create fresh credentials
                try (InputStream jsonStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8))) {
                    freshCreds = ServiceAccountCredentials.fromStream(jsonStream);

                    // Verify the client email matches what we extracted
                    if (clientEmail != null && !clientEmail.equals(freshCreds.getClientEmail())) {
                        logger.warn("Client email mismatch! Extracted: {}, Credential: {}", 
                                clientEmail, freshCreds.getClientEmail());
                    }

                    // Set explicit scopes
                    freshCreds = (ServiceAccountCredentials) freshCreds.createScoped(
                            Collections.singletonList(CalendarScopes.CALENDAR_READONLY));

                    logger.info("Successfully created fresh credentials for user: {}", userEmail);
                }
            } catch (Exception e) {
                logger.warn("Failed to create fresh credentials, falling back to cached ones: {}", e.getMessage(), e);
                if (baseCreds == null) {
                    throw new IllegalStateException("Base credentials are not initialized", e);
                }
                freshCreds = baseCreds;
            }

            // Create delegated credentials with explicit scopes
            GoogleCredentials delegated;
            try {
                logger.debug("Creating delegated credentials with user subject: {}", userEmail);

                // First approach: create delegated then scoped
                delegated = freshCreds.createDelegated(userEmail);
                logger.debug("Successfully created delegated credentials for user: {}", userEmail);

                // Add scopes if needed
                if (delegated.createScopedRequired()) {
                    delegated = delegated.createScoped(Collections.singletonList(CalendarScopes.CALENDAR_READONLY));
                    logger.debug("Added scopes to delegated credentials");
                }
            } catch (Exception e) {
                logger.warn("First delegation approach failed, trying alternative: {}", e.getMessage());
                try {
                    // Alternative approach: create scoped then delegated
                    GoogleCredentials scoped = freshCreds.createScoped(Collections.singletonList(CalendarScopes.CALENDAR_READONLY));
                    delegated = scoped.createDelegated(userEmail);
                    logger.debug("Successfully created delegated credentials using alternative approach");
                } catch (Exception e2) {
                    logger.error("Both delegation approaches failed", e2);
                    throw new IllegalStateException("Failed to create delegated credentials", e2);
                }
            }

            // Force refresh the token
            try {
                logger.debug("Refreshing access token for user: {}", userEmail);
                delegated.refresh();
                logger.info("Successfully refreshed token for user: {}", userEmail);
            } catch (IOException e) {
                logger.warn("Failed to refresh token, will continue with potentially cached token: {}", e.getMessage());
            }

            // Create HTTP transport with proper security
            NetHttpTransport httpTransport;
            try {
                httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                logger.debug("Created trusted HTTP transport");
            } catch (GeneralSecurityException e) {
                logger.warn("Failed to create trusted transport, falling back to standard: {}", e.getMessage());
                httpTransport = new NetHttpTransport();
            }

            // Create HTTP credentials adapter
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(delegated);
            logger.debug("Created HTTP credentials adapter");

            // Build and return the Calendar client
            Calendar client = new Calendar.Builder(
                    httpTransport,
                    GsonFactory.getDefaultInstance(),
                    requestInitializer
            ).setApplicationName("calendar-management").build();

            logger.info("Successfully created Calendar client for user: {}", userEmail);
            return client;

        } catch (Exception e) {
            logger.error("Failed to create Calendar client for user: {}", userEmail, e);
            throw new RuntimeException("Failed to create Calendar client: " + e.getMessage(), e);
        }
    }
}
