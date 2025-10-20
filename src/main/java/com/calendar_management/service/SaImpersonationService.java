package com.calendar_management.service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Service for impersonating users with a service account
 * Only active when the "calendar" profile is active
 */
@Service
@Profile("calendar")
public class SaImpersonationService {

    private final ResourceLoader resourceLoader;

    public SaImpersonationService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Value("${app.google.sa.key-file}")
    private String keyLocation;

    private ServiceAccountCredentials baseCreds;

    @PostConstruct
    void init() throws Exception {
        String loc = keyLocation;
        if (!loc.startsWith("classpath:") && !loc.startsWith("file:")) {
            // assume filesystem se não tiver esquema explícito
            loc = "file:" + loc;
        }

        Resource keyRes = resourceLoader.getResource(loc);
        if (!keyRes.exists()) {
            throw new IllegalStateException("Service account key não encontrada em: " + loc);
        }

        try (InputStream in = keyRes.getInputStream()) {
            this.baseCreds = (ServiceAccountCredentials) ServiceAccountCredentials.fromStream(in);
        }
    }

    public Calendar calendarAs(String userEmail) {
        GoogleCredentials delegated = baseCreds
                .createDelegated(userEmail)
                .createScoped(List.of(CalendarScopes.CALENDAR_READONLY));

        return new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(delegated)
        ).setApplicationName("calendar-management").build();
    }
}
