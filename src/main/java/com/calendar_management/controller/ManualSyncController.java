package com.calendar_management.controller;

import com.calendar_management.model.CalendarSyncStateEntity;
import com.calendar_management.repository.CalendarSyncStateRepository;
import com.calendar_management.service.OrgCalendarIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/sync")
@RequiredArgsConstructor
public class ManualSyncController {

     private final OrgCalendarIngestionService ingestionService;
    private final CalendarSyncStateRepository syncRepo;

    @Value("${app.workspace.users:}")
    private String usersCsv;

    private static final String CALENDAR_ID = "primary";

    @PostMapping("/user")
    public ResponseEntity<?> syncUser(
            @RequestParam("email") String email,
            @RequestParam(value = "reset", required = false, defaultValue = "false") boolean reset
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", email);
        result.put("calendarId", CALENDAR_ID);
        result.put("reset", reset);

        try {
            if (reset) {
                CalendarSyncStateEntity state = syncRepo.findByUserEmailAndCalendarId(email, CALENDAR_ID);
                if (state != null) {
                    state.setSyncToken(null);
                    state.setLastSyncedAt(null);
                    syncRepo.save(state);
                }
            }
            ingestionService.syncUser(email);
            result.put("status", "OK");
            result.put("syncedAt", Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @PostMapping("/all")
    public ResponseEntity<?> syncAll(
            @RequestParam(value = "reset", required = false, defaultValue = "false") boolean reset
    ) {
        List<String> emails = Arrays.stream(Optional.ofNullable(usersCsv).orElse("")
                        .split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (emails.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "error", "app.workspace.users está vazio; defina a lista de e-mails ou integre a Directory API."
            ));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (String email : emails) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("email", email);
            r.put("calendarId", CALENDAR_ID);
            r.put("reset", reset);
            try {
                if (reset) {
                    CalendarSyncStateEntity state = syncRepo.findByUserEmailAndCalendarId(email, CALENDAR_ID);
                    if (state != null) {
                        state.setSyncToken(null);
                        state.setLastSyncedAt(null);
                        syncRepo.save(state);
                    }
                }
                ingestionService.syncUser(email);
                r.put("status", "OK");
                r.put("syncedAt", Instant.now().toString());
            } catch (Exception e) {
                r.put("status", "ERROR");
                r.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            results.add(r);
        }

        return ResponseEntity.ok(results);
    }
}
