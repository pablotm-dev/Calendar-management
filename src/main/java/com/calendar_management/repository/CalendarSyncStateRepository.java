package com.calendar_management.repository;

import com.calendar_management.model.CalendarSyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarSyncStateRepository extends JpaRepository<CalendarSyncStateEntity, Long> {
    CalendarSyncStateEntity findByUserEmailAndCalendarId(String userEmail, String calendarId);
}
