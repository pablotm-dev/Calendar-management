package com.calendar_management.job;

import com.calendar_management.service.OrgCalendarIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Job for ingesting calendar events from Google Calendar
 * Only active when the "calendar" profile is active
 */
@Component
@RequiredArgsConstructor
@Profile("calendar")
public class CalendarIngestionJob {

    private final OrgCalendarIngestionService ingestionService;

    @Value("${app.workspace.users}")
    private String usersCsv;

    // A cada 10 minutos
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        List<String> emails = Arrays.stream(usersCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        for (String email : emails) {
            try {
                ingestionService.syncUser(email);
            } catch (Exception e) {
                // TODO logar/alertar
            }
        }
    }
}
