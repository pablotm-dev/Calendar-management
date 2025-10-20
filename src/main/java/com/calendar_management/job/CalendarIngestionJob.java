package com.calendar_management.job;

import com.calendar_management.service.OrgCalendarIngestionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CalendarIngestionJob.class);

    private final OrgCalendarIngestionService ingestionService;

    @Value("${app.workspace.users}")
    private String usersCsv;

    //A cada 1 minuto
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        logger.info("Starting calendar synchronization job");

        List<String> emails = Arrays.stream(usersCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        logger.debug("Processing {} users for calendar sync", emails.size());

        for (String email : emails) {
            try {
                logger.debug("Syncing calendar for user: {}", email);
                ingestionService.syncUser(email);
                logger.debug("Successfully synced calendar for user: {}", email);
            } catch (Exception e) {
                logger.error("Error syncing calendar for user: {}", email, e);
            }
        }

        logger.info("Completed calendar synchronization job");
    }
}
