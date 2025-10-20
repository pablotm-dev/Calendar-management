package com.calendar_management.controller;


import com.calendar_management.dto.CalendarDTO;
import com.calendar_management.dto.EventDTO;
import com.calendar_management.service.GoogleCalendarService;
import com.google.api.services.calendar.Calendar;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Controller for Google Calendar integration
 * Only active when the "calendar" profile is active
 */
@RestController
@RequiredArgsConstructor
@Profile("calendar")
public class CalendarController {

    private final GoogleCalendarService calendarService;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/")
    public String home() {
        return """
               <h1>Google Calendar Demo</h1>
               <p>Faça login com Google em <a href="/oauth2/authorization/google">/oauth2/authorization/google</a></p>
               <p>Depois acesse:</p>
               <ul>
                 <li><code>/calendars</code></li>
                 <li><code>/calendars/{calendarId}/events?timeMin=2025-09-01T00:00:00Z&timeMax=2025-09-30T23:59:59Z</code></li>
                 <li>Dica: use <code>calendarId=primary</code> para sua agenda principal.</li>
               </ul>
               """;
    }

    @GetMapping("/calendars")
    public List<CalendarDTO> listCalendars(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client
    ) throws Exception {
        Calendar c = calendarService.buildClientFromAuthorizedClient(client);
        return calendarService.listCalendars(c);
    }

    @GetMapping("/calendars/{calendarId}/events")
    public List<EventDTO> listEvents(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            @PathVariable String calendarId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant timeMin,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant timeMax
    ) throws Exception {
        Calendar c = calendarService.buildClientFromAuthorizedClient(client);
        return calendarService.listEvents(c, calendarId, timeMin, timeMax);
    }
}
