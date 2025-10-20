package com.calendar_management.service;

import com.calendar_management.dto.CalendarDTO;
import com.calendar_management.dto.EventDTO;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service for Google Calendar integration
 * Only active when the "calendar" profile is active
 */
@Service
@Profile("calendar")
public class GoogleCalendarService {

    private static final String APP_NAME = "calendar-management";

    public Calendar buildClientFromAuthorizedClient(OAuth2AuthorizedClient client) {
        AccessToken token = new AccessToken(
                client.getAccessToken().getTokenValue(),
                client.getAccessToken().getExpiresAt() != null
                        ? Date.from(client.getAccessToken().getExpiresAt())
                        : null
        );

        GoogleCredentials credentials = GoogleCredentials.create(token);
        HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(credentials);

        return new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                adapter
        ).setApplicationName(APP_NAME).build();
    }

    public List<CalendarDTO> listCalendars(Calendar calendarClient) throws Exception {
        List<CalendarDTO> out = new ArrayList<>();
        String pageToken = null;

        do {
            CalendarList calendarList = calendarClient.calendarList().list()
                    .setPageToken(pageToken)
                    .execute();

            if (calendarList.getItems() != null) {
                for (CalendarListEntry entry : calendarList.getItems()) {
                    out.add(new CalendarDTO(
                            entry.getId(),
                            entry.getSummary(),
                            entry.getDescription(),
                            entry.getTimeZone(),
                            Boolean.toString(Boolean.TRUE.equals(entry.getPrimary()))
                    ));
                }
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        return out;
    }

    public List<EventDTO> listEvents(Calendar calendarClient,
                                     String calendarId,
                                     Instant timeMin,
                                     Instant timeMax) throws Exception {

        com.google.api.client.util.DateTime gTimeMin =
                timeMin != null ? new com.google.api.client.util.DateTime(Date.from(timeMin)) : null;
        com.google.api.client.util.DateTime gTimeMax =
                timeMax != null ? new com.google.api.client.util.DateTime(Date.from(timeMax)) : null;

        List<EventDTO> out = new ArrayList<>();
        String pageToken = null;

        do {
            Calendar.Events.List list = calendarClient.events().list(calendarId)
                    .setSingleEvents(true)
                    .setOrderBy("startTime");

            if (gTimeMin != null) list.setTimeMin(gTimeMin);
            if (gTimeMax != null) list.setTimeMax(gTimeMax);

            Events events = list.setPageToken(pageToken).execute();

            if (events.getItems() != null) {
                for (Event e : events.getItems()) {
                    String startIso = e.getStart() != null
                            ? (e.getStart().getDateTime() != null
                            ? e.getStart().getDateTime().toStringRfc3339()
                            : e.getStart().getDate() != null ? e.getStart().getDate().toStringRfc3339() : null)
                            : null;

                    String endIso = e.getEnd() != null
                            ? (e.getEnd().getDateTime() != null
                            ? e.getEnd().getDateTime().toStringRfc3339()
                            : e.getEnd().getDate() != null ? e.getEnd().getDate().toStringRfc3339() : null)
                            : null;

                    out.add(new EventDTO(
                            e.getId(),
                            e.getSummary(),
                            e.getOrganizer() != null ? e.getOrganizer().getEmail() : null,
                            e.getHtmlLink(),
                            startIso,
                            endIso,
                            e.getLocation()
                    ));
                }
            }
            pageToken = events.getNextPageToken();
        } while (pageToken != null);

        return out;
    }
}
