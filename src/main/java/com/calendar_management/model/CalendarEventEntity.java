package com.calendar_management.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "calendar_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_user_cal",
                columnNames = {"user_email", "calendar_id", "event_id"}
        ),
        indexes = {
                @Index(name = "idx_calendar_event_task", columnList = "task_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_email", nullable = false, length = 320)
    private String userEmail;

    @Column(name="calendar_id", nullable = false, length = 512)
    private String calendarId;

    @Column(name="event_id", nullable = false, length = 512)
    private String eventId;

    @Column(name="summary", length = 1024)
    private String summary;

    @Column(name="organizer_email", length = 320)
    private String organizerEmail;

    @Column(name="html_link", length = 1024)
    private String htmlLink;

    @Column(name="location", length = 1024)
    private String location;

    @Column(name="start_instant")
    private Instant startInstant;

    @Column(name="end_instant")
    private Instant endInstant;

    @Column(name="status", length = 64) // confirmed | tentative | cancelled
    private String status;

    @Column(name="updated_instant")
    private Instant updatedInstant;

    @Column(name="created_at")
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;

    // NOVO: vínculo do evento com a Task (pela tag extraída do summary)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_calendar_event_task"))
    private Task task;
}
