package com.calendar_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "calendar_sync_state",
        uniqueConstraints = @UniqueConstraint(name="uk_sync_user_cal",
                columnNames = {"user_email", "calendar_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarSyncStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_email", nullable = false, length = 320)
    private String userEmail;

    @Column(name="calendar_id", nullable = false, length = 512)
    private String calendarId; // normalmente "primary"

    @Column(name="sync_token", length = 2048)
    private String syncToken;

    @Column(name="last_synced_at")
    private Instant lastSyncedAt;
}
