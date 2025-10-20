package com.calendar_management.repository;

import com.calendar_management.model.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {
    CalendarEventEntity findByUserEmailAndCalendarIdAndEventId(String userEmail, String calendarId, String eventId);

    @Transactional
    void deleteByUserEmailAndCalendarIdAndEventId(String userEmail, String calendarId, String eventId);

    /**
     * Find events by user email and time range
     */
    List<CalendarEventEntity> findByUserEmailAndStartInstantGreaterThanEqualAndEndInstantLessThanEqual(
            String userEmail, Instant startInstant, Instant endInstant);

    /**
     * Find events by time range
     */
    List<CalendarEventEntity> findByStartInstantGreaterThanEqualAndEndInstantLessThanEqual(
            Instant startInstant, Instant endInstant);

    /**
     * Find events by user email, task IDs, and time range
     */
    @Query("SELECT e FROM CalendarEventEntity e WHERE e.userEmail = :userEmail AND e.task.id IN :taskIds " +
            "AND e.startInstant >= :startInstant AND e.endInstant <= :endInstant")
    List<CalendarEventEntity> findByUserEmailAndTaskIdsAndTimeRange(
            @Param("userEmail") String userEmail,
            @Param("taskIds") List<Long> taskIds,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant);

    /**
     * Find events by user email, project IDs (via task), and time range
     */
    @Query("SELECT e FROM CalendarEventEntity e WHERE e.userEmail = :userEmail AND e.task.projeto.id IN :projectIds " +
            "AND e.startInstant >= :startInstant AND e.endInstant <= :endInstant")
    List<CalendarEventEntity> findByUserEmailAndProjectIdsAndTimeRange(
            @Param("userEmail") String userEmail,
            @Param("projectIds") List<Long> projectIds,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant);

    /**
     * Find events by task IDs and time range
     */
    @Query("SELECT e FROM CalendarEventEntity e WHERE e.task.id IN :taskIds " +
            "AND e.startInstant >= :startInstant AND e.endInstant <= :endInstant")
    List<CalendarEventEntity> findByTaskIdsAndTimeRange(
            @Param("taskIds") List<Long> taskIds,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant);

    /**
     * Find events by project IDs (via task) and time range
     */
    @Query("SELECT e FROM CalendarEventEntity e WHERE e.task.projeto.id IN :projectIds " +
            "AND e.startInstant >= :startInstant AND e.endInstant <= :endInstant")
    List<CalendarEventEntity> findByProjectIdsAndTimeRange(
            @Param("projectIds") List<Long> projectIds,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant);

    /**
     * Find events by client ID (via task.projeto.cliente) and time range
     */
    @Query("SELECT e FROM CalendarEventEntity e WHERE e.task.projeto.cliente.id = :clientId " +
            "AND e.startInstant >= :startInstant AND e.endInstant <= :endInstant")
    List<CalendarEventEntity> findByClientIdAndTimeRange(
            @Param("clientId") Long clientId,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant);

    /**
     * Find all unique user emails in the events
     */
    @Query("SELECT DISTINCT e.userEmail FROM CalendarEventEntity e")
    List<String> findAllUserEmails();
}
