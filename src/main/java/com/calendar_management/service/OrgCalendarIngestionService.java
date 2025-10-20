package com.calendar_management.service;

import com.calendar_management.model.Task;
import com.calendar_management.model.CalendarEventEntity;
import com.calendar_management.repository.CalendarEventRepository;
import com.calendar_management.model.CalendarSyncStateEntity;
import com.calendar_management.repository.CalendarSyncStateRepository;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for ingesting calendar events from Google Calendar
 * Only active when the "calendar" profile is active
 */
@Service
@RequiredArgsConstructor
@Profile("calendar")
public class OrgCalendarIngestionService {

    private final SaImpersonationService saImpersonationService;
    private final CalendarEventRepository eventRepo;
    private final CalendarSyncStateRepository syncRepo;
    private final TaskService taskService; // usa cache + resolução em lote

    @Value("${app.calendar.initial-days:90}")
    private int initialDays;

    private static final String CALENDAR_ID = "primary";

    // ===== helpers de janela/tempo =====

    private ZoneId userZone(Calendar client) throws IOException {
        CalendarListEntry entry = client.calendarList().get(CALENDAR_ID).execute();
        String tz = entry.getTimeZone() != null ? entry.getTimeZone() : "UTC";
        return ZoneId.of(tz);
    }

    /** timeMax EXCLUSIVO = início de amanhã na TZ do usuário */
    private Instant endOfTodayExclusive(ZoneId zone) {
        return LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();
    }

    private boolean sameLocalDay(Instant a, Instant b, ZoneId zone) {
        return a.atZone(zone).toLocalDate().equals(b.atZone(zone).toLocalDate());
    }

    // ===== fluxo principal =====

    public void syncUser(String userEmail) throws Exception {
        Calendar client = saImpersonationService.calendarAs(userEmail);

        CalendarSyncStateEntity state = Optional
                .ofNullable(syncRepo.findByUserEmailAndCalendarId(userEmail, CALENDAR_ID))
                .orElseGet(() -> {
                    CalendarSyncStateEntity s = new CalendarSyncStateEntity();
                    s.setUserEmail(userEmail);
                    s.setCalendarId(CALENDAR_ID);
                    return s;
                });

        String syncToken = state.getSyncToken();
        String nextSyncToken = null;

        ZoneId zone = userZone(client);
        Instant windowUntil = endOfTodayExclusive(zone);
        Instant now = Instant.now();

        if (state.getLastSyncedAt() == null || !sameLocalDay(state.getLastSyncedAt(), now, zone)) {
            syncToken = null;
        }

        try {
            if (syncToken == null || syncToken.isBlank()) {
                // -------- FULL SYNC --------
                Instant from = now.minus(initialDays, ChronoUnit.DAYS);
                String pageToken = null;

                do {
                    Calendar.Events.List list = client.events().list(CALENDAR_ID)
                            .setSingleEvents(true)
                            .setOrderBy("startTime")
                            .setShowDeleted(true)
                            .setTimeMin(new DateTime(java.util.Date.from(from)))
                            .setTimeMax(new DateTime(java.util.Date.from(windowUntil)));

                    if (pageToken != null) list.setPageToken(pageToken);
                    Events events = list.execute();

                    upsertBatch(userEmail, CALENDAR_ID, events.getItems());
                    pageToken = events.getNextPageToken();
                    nextSyncToken = events.getNextSyncToken();
                } while (pageToken != null);

            } else {
                // -------- INCREMENTAL --------
                String pageToken = null;

                do {
                    Calendar.Events.List list = client.events().list(CALENDAR_ID)
                            .setShowDeleted(true)
                            .setSyncToken(syncToken);

                    if (pageToken != null) list.setPageToken(pageToken);
                    Events events = list.execute();

                    // filtra futuros (segurança)
                    List<Event> onlyPastOrToday = new ArrayList<>();
                    if (events.getItems() != null) {
                        for (Event e : events.getItems()) {
                            Instant start = null;
                            if (e.getStart() != null) {
                                if (e.getStart().getDateTime() != null) {
                                    start = Instant.ofEpochMilli(e.getStart().getDateTime().getValue());
                                } else if (e.getStart().getDate() != null) {
                                    start = Instant.ofEpochMilli(e.getStart().getDate().getValue());
                                }
                            }
                            if (start == null || start.isBefore(windowUntil)) {
                                onlyPastOrToday.add(e);
                            }
                        }
                    }

                    upsertBatch(userEmail, CALENDAR_ID, onlyPastOrToday);
                    pageToken = events.getNextPageToken();
                    nextSyncToken = events.getNextSyncToken();
                } while (pageToken != null);
            }

            if (nextSyncToken != null) {
                state.setSyncToken(nextSyncToken);
                state.setLastSyncedAt(now);
                syncRepo.save(state);
            }

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 410) {
                state.setSyncToken(null);
                syncRepo.save(state);
                syncUser(userEmail);
            } else {
                throw e;
            }
        }
    }

    /**
     * Upsert/Remove e VÍNCULO COM TASK
     * - resolve tags em LOTE para evitar 1 query por evento
     * - usa chaves NORMALIZADAS (consistência com o cache)
     */
    private void upsertBatch(String userEmail, String calendarId, List<Event> items) {
        if (items == null || items.isEmpty()) return;

        // 1) Coleta as tags NORMALIZADAS dos summaries desta página
        Set<String> normalizedTags = items.stream()
                .map(Event::getSummary)
                .map(taskService::normalizedLeadingTag) // extrai + normaliza (aceita acento)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 2) Resolve em LOTE (1 query IN) + cache; chaves são NORMALIZADAS
        Map<String, Task> tagMap = taskService.resolveTagsBulk(normalizedTags);
        Task generic = taskService.getGenericTask();

        for (Event e : items) {
            String eventId = e.getId();
            String status = e.getStatus(); // confirmed | tentative | cancelled

            if ("cancelled".equalsIgnoreCase(status)) {
                eventRepo.deleteByUserEmailAndCalendarIdAndEventId(userEmail, calendarId, eventId);
                continue;
            }

            Instant start = null, end = null;
            if (e.getStart() != null) {
                if (e.getStart().getDateTime() != null) {
                    start = Instant.ofEpochMilli(e.getStart().getDateTime().getValue());
                } else if (e.getStart().getDate() != null) {
                    start = Instant.ofEpochMilli(e.getStart().getDate().getValue());
                }
            }
            if (e.getEnd() != null) {
                if (e.getEnd().getDateTime() != null) {
                    end = Instant.ofEpochMilli(e.getEnd().getDateTime().getValue());
                } else if (e.getEnd().getDate() != null) {
                    end = Instant.ofEpochMilli(e.getEnd().getDate().getValue());
                }
            }

            Instant updated = e.getUpdated() != null
                    ? Instant.ofEpochMilli(e.getUpdated().getValue())
                    : null;

            // 3) Descobre a Task (pela tag normalizada) ou #GENERICO
            String normTag = taskService.normalizedLeadingTag(e.getSummary());
            Task resolvedTask = (normTag != null) ? tagMap.getOrDefault(normTag, generic) : generic;

            CalendarEventEntity existing =
                    eventRepo.findByUserEmailAndCalendarIdAndEventId(userEmail, calendarId, eventId);

            if (existing == null) {
                CalendarEventEntity neo = CalendarEventEntity.builder()
                        .userEmail(userEmail)
                        .calendarId(calendarId)
                        .eventId(eventId)
                        .summary(e.getSummary())
                        .organizerEmail(e.getOrganizer() != null ? e.getOrganizer().getEmail() : null)
                        .htmlLink(e.getHtmlLink())
                        .location(e.getLocation())
                        .startInstant(start)
                        .endInstant(end)
                        .status(status)
                        .updatedInstant(updated)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .task(resolvedTask)
                        .build();
                eventRepo.save(neo);
            } else {
                existing.setSummary(e.getSummary());
                existing.setOrganizerEmail(e.getOrganizer() != null ? e.getOrganizer().getEmail() : null);
                existing.setHtmlLink(e.getHtmlLink());
                existing.setLocation(e.getLocation());
                existing.setStartInstant(start);
                existing.setEndInstant(end);
                existing.setStatus(status);
                existing.setUpdatedInstant(updated);
                existing.setUpdatedAt(Instant.now());
                existing.setTask(resolvedTask);
                eventRepo.save(existing);
            }
        }
    }
}
