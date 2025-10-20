package com.calendar_management.service;

import com.calendar_management.dto.report.ClientReportDTO;
import com.calendar_management.dto.report.CollaboratorReportDTO;
import com.calendar_management.dto.report.ReportFilterDTO;
import com.calendar_management.model.CalendarEventEntity;
import com.calendar_management.model.Cliente;
import com.calendar_management.model.Projeto;
import com.calendar_management.model.Task;
import com.calendar_management.repository.CalendarEventRepository;
import com.calendar_management.repository.ClienteRepository;
import com.calendar_management.repository.ProjetoRepository;
import com.calendar_management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for generating reports from calendar events
 */
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final CalendarEventRepository calendarEventRepository;
    private final TaskRepository taskRepository;
    private final ProjetoRepository projetoRepository;
    private final ClienteRepository clienteRepository;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Generate a report for a specific collaborator
     * 
     * @param collaboratorEmail The email of the collaborator
     * @param filter The filter to apply to the report
     * @return The collaborator report
     */
    public CollaboratorReportDTO generateCollaboratorReport(String collaboratorEmail, ReportFilterDTO filter) {
        // Apply filter and get time range
        Instant startInstant = getStartInstant(filter);
        Instant endInstant = getEndInstant(filter);

        // Get events for the collaborator in the time range
        List<CalendarEventEntity> events;

        if (filter.getTaskIds() != null && !filter.getTaskIds().isEmpty()) {
            events = calendarEventRepository.findByUserEmailAndTaskIdsAndTimeRange(
                    collaboratorEmail, filter.getTaskIds(), startInstant, endInstant);
        } else if (filter.getProjectIds() != null && !filter.getProjectIds().isEmpty()) {
            events = calendarEventRepository.findByUserEmailAndProjectIdsAndTimeRange(
                    collaboratorEmail, filter.getProjectIds(), startInstant, endInstant);
        } else {
            events = calendarEventRepository.findByUserEmailAndStartInstantGreaterThanEqualAndEndInstantLessThanEqual(
                    collaboratorEmail, startInstant, endInstant);
        }

        if (events.isEmpty()) {
            return CollaboratorReportDTO.builder()
                    .collaboratorEmail(collaboratorEmail)
                    .totalHoursWorked(0)
                    .hoursPerProject(Collections.emptyList())
                    .hoursPerTask(Collections.emptyList())
                    .build();
        }

        // Calculate total hours worked
        double totalHours = calculateTotalHours(events);

        // Calculate hours per project
        List<CollaboratorReportDTO.ProjectHoursDTO> hoursPerProject = calculateHoursPerProject(events);

        // Calculate hours per task
        List<CollaboratorReportDTO.TaskHoursDTO> hoursPerTask = calculateHoursPerTask(events);

        // Generate productivity analysis
        CollaboratorReportDTO.ProductivityAnalysisDTO productivityAnalysis = generateProductivityAnalysis(events);

        // Generate context details
        CollaboratorReportDTO.ContextDetailsDTO contextDetails = generateContextDetails(events);

        return CollaboratorReportDTO.builder()
                .collaboratorEmail(collaboratorEmail)
                .totalHoursWorked(totalHours)
                .hoursPerProject(hoursPerProject)
                .hoursPerTask(hoursPerTask)
                .productivityAnalysis(productivityAnalysis)
                .contextDetails(contextDetails)
                .build();
    }

    /**
     * Generate reports for all collaborators
     * 
     * @param filter The filter to apply to the reports
     * @return List of collaborator reports
     */
    public List<CollaboratorReportDTO> generateAllCollaboratorReports(ReportFilterDTO filter) {
        List<String> collaboratorEmails;

        if (filter.getCollaboratorEmails() != null && !filter.getCollaboratorEmails().isEmpty()) {
            collaboratorEmails = filter.getCollaboratorEmails();
        } else {
            collaboratorEmails = calendarEventRepository.findAllUserEmails();
        }

        return collaboratorEmails.stream()
                .map(email -> generateCollaboratorReport(email, filter))
                .collect(Collectors.toList());
    }

    /**
     * Generate a report for a specific client
     * 
     * @param clientId The ID of the client
     * @param filter The filter to apply to the report
     * @return The client report
     */
    public ClientReportDTO generateClientReport(Long clientId, ReportFilterDTO filter) {
        // Get client
        Cliente cliente = clienteRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // Apply filter and get time range
        Instant startInstant = getStartInstant(filter);
        Instant endInstant = getEndInstant(filter);

        // Get events for the client in the time range
        List<CalendarEventEntity> events = calendarEventRepository.findByClientIdAndTimeRange(
                clientId, startInstant, endInstant);

        if (events.isEmpty()) {
            return ClientReportDTO.builder()
                    .clientId(clientId)
                    .clientName(cliente.getNomeCliente())
                    .projects(Collections.emptyList())
                    .build();
        }

        // Group events by project
        Map<Long, List<CalendarEventEntity>> eventsByProject = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTask().getProjeto().getId()));

        // Generate project reports
        List<ClientReportDTO.ProjectReportDTO> projectReports = new ArrayList<>();

        for (Map.Entry<Long, List<CalendarEventEntity>> entry : eventsByProject.entrySet()) {
            Long projectId = entry.getKey();
            List<CalendarEventEntity> projectEvents = entry.getValue();

            Projeto projeto = projetoRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

            // Calculate total hours for the project
            double totalHours = calculateTotalHours(projectEvents);

            // Group events by collaborator
            Map<String, List<CalendarEventEntity>> eventsByCollaborator = projectEvents.stream()
                    .collect(Collectors.groupingBy(CalendarEventEntity::getUserEmail));

            // Calculate hours per collaborator
            List<ClientReportDTO.CollaboratorHoursDTO> collaboratorHours = eventsByCollaborator.entrySet().stream()
                    .map(e -> ClientReportDTO.CollaboratorHoursDTO.builder()
                            .collaboratorEmail(e.getKey())
                            .hours(calculateTotalHours(e.getValue()))
                            .build())
                    .collect(Collectors.toList());

            // Group events by task
            Map<Long, List<CalendarEventEntity>> eventsByTask = projectEvents.stream()
                    .collect(Collectors.groupingBy(e -> e.getTask().getId()));

            // Generate task reports
            List<ClientReportDTO.TaskReportDTO> taskReports = new ArrayList<>();

            for (Map.Entry<Long, List<CalendarEventEntity>> taskEntry : eventsByTask.entrySet()) {
                Long taskId = taskEntry.getKey();
                List<CalendarEventEntity> taskEvents = taskEntry.getValue();

                Task task = taskRepository.findById(taskId)
                        .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

                // Calculate total hours for the task
                double taskTotalHours = calculateTotalHours(taskEvents);

                // Group task events by collaborator
                Map<String, List<CalendarEventEntity>> taskEventsByCollaborator = taskEvents.stream()
                        .collect(Collectors.groupingBy(CalendarEventEntity::getUserEmail));

                // Calculate hours per collaborator for the task
                List<ClientReportDTO.CollaboratorHoursDTO> taskCollaboratorHours = taskEventsByCollaborator.entrySet().stream()
                        .map(e -> ClientReportDTO.CollaboratorHoursDTO.builder()
                                .collaboratorEmail(e.getKey())
                                .hours(calculateTotalHours(e.getValue()))
                                .build())
                        .collect(Collectors.toList());

                taskReports.add(ClientReportDTO.TaskReportDTO.builder()
                        .taskId(taskId)
                        .taskName(task.getNomeTask())
                        .totalHours(taskTotalHours)
                        .collaboratorHours(taskCollaboratorHours)
                        .build());
            }

            projectReports.add(ClientReportDTO.ProjectReportDTO.builder()
                    .projectId(projectId)
                    .projectName(projeto.getNomeProjeto())
                    .totalHours(totalHours)
                    .collaboratorHours(collaboratorHours)
                    .tasks(taskReports)
                    .build());
        }

        return ClientReportDTO.builder()
                .clientId(clientId)
                .clientName(cliente.getNomeCliente())
                .projects(projectReports)
                .build();
    }

    /**
     * Generate reports for all clients
     * 
     * @param filter The filter to apply to the reports
     * @return List of client reports
     */
    public List<ClientReportDTO> generateAllClientReports(ReportFilterDTO filter) {
        List<Cliente> clients = clienteRepository.findAll();

        return clients.stream()
                .map(cliente -> generateClientReport(cliente.getId(), filter))
                .collect(Collectors.toList());
    }

    // Helper methods

    /**
     * Calculate total hours from a list of events
     */
    private double calculateTotalHours(List<CalendarEventEntity> events) {
        return events.stream()
                .filter(e -> e.getStartInstant() != null && e.getEndInstant() != null)
                .mapToDouble(e -> {
                    Duration duration = Duration.between(e.getStartInstant(), e.getEndInstant());
                    return duration.toMinutes() / 60.0;
                })
                .sum();
    }

    /**
     * Calculate hours per project from a list of events
     */
    private List<CollaboratorReportDTO.ProjectHoursDTO> calculateHoursPerProject(List<CalendarEventEntity> events) {
        // Group events by project
        Map<Long, List<CalendarEventEntity>> eventsByProject = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTask().getProjeto().getId()));

        // Calculate hours per project
        return eventsByProject.entrySet().stream()
                .map(entry -> {
                    Long projectId = entry.getKey();
                    List<CalendarEventEntity> projectEvents = entry.getValue();

                    Projeto projeto = projetoRepository.findById(projectId)
                            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

                    return CollaboratorReportDTO.ProjectHoursDTO.builder()
                            .projectId(projectId)
                            .projectName(projeto.getNomeProjeto())
                            .hours(calculateTotalHours(projectEvents))
                            .clientName(projeto.getCliente().getNomeCliente())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate hours per task from a list of events
     */
    private List<CollaboratorReportDTO.TaskHoursDTO> calculateHoursPerTask(List<CalendarEventEntity> events) {
        // Group events by task
        Map<Long, List<CalendarEventEntity>> eventsByTask = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTask().getId()));

        // Calculate hours per task
        return eventsByTask.entrySet().stream()
                .map(entry -> {
                    Long taskId = entry.getKey();
                    List<CalendarEventEntity> taskEvents = entry.getValue();

                    Task task = taskRepository.findById(taskId)
                            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

                    return CollaboratorReportDTO.TaskHoursDTO.builder()
                            .taskId(taskId)
                            .taskName(task.getNomeTask())
                            .hours(calculateTotalHours(taskEvents))
                            .projectId(task.getProjeto().getId())
                            .projectName(task.getProjeto().getNomeProjeto())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Generate productivity analysis from a list of events
     */
    private CollaboratorReportDTO.ProductivityAnalysisDTO generateProductivityAnalysis(List<CalendarEventEntity> events) {
        // Calculate hours by day of week
        Map<Integer, Double> hoursByDayOfWeek = new HashMap<>();

        // Calculate hours by month
        Map<Integer, Double> hoursByMonth = new HashMap<>();

        // Calculate trend data
        Map<LocalDate, Double> hoursByDate = new HashMap<>();

        for (CalendarEventEntity event : events) {
            if (event.getStartInstant() == null || event.getEndInstant() == null) {
                continue;
            }

            // Calculate duration in hours
            Duration duration = Duration.between(event.getStartInstant(), event.getEndInstant());
            double hours = duration.toMinutes() / 60.0;

            // Get day of week (1-7, where 1 is Monday)
            LocalDate date = event.getStartInstant().atZone(DEFAULT_ZONE).toLocalDate();
            int dayOfWeek = date.getDayOfWeek().getValue();

            // Get month (1-12)
            int month = date.getMonthValue();

            // Update hours by day of week
            hoursByDayOfWeek.put(dayOfWeek, hoursByDayOfWeek.getOrDefault(dayOfWeek, 0.0) + hours);

            // Update hours by month
            hoursByMonth.put(month, hoursByMonth.getOrDefault(month, 0.0) + hours);

            // Update hours by date
            hoursByDate.put(date, hoursByDate.getOrDefault(date, 0.0) + hours);
        }

        // Convert hours by date to trend data
        List<CollaboratorReportDTO.TimeSeriesDataPoint> trendData = hoursByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> CollaboratorReportDTO.TimeSeriesDataPoint.builder()
                        .date(entry.getKey().format(DATE_FORMATTER))
                        .hours(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // Find peak and low periods
        List<CollaboratorReportDTO.ProductivityPeriodDTO> peakPeriods = findProductivityPeriods(hoursByDate, true);
        List<CollaboratorReportDTO.ProductivityPeriodDTO> lowPeriods = findProductivityPeriods(hoursByDate, false);

        return CollaboratorReportDTO.ProductivityAnalysisDTO.builder()
                .hoursByDayOfWeek(hoursByDayOfWeek)
                .hoursByMonth(hoursByMonth)
                .trendData(trendData)
                .peakPeriods(peakPeriods)
                .lowPeriods(lowPeriods)
                .build();
    }

    /**
     * Find productivity periods (peak or low)
     * 
     * @param hoursByDate Map of hours by date
     * @param findPeaks If true, find peak periods; if false, find low periods
     * @return List of productivity periods
     */
    private List<CollaboratorReportDTO.ProductivityPeriodDTO> findProductivityPeriods(
            Map<LocalDate, Double> hoursByDate, boolean findPeaks) {
        if (hoursByDate.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate average hours per day
        double averageHours = hoursByDate.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Find periods where hours are significantly above/below average
        double threshold = findPeaks ? averageHours * 1.5 : averageHours * 0.5;

        // Sort dates
        List<LocalDate> sortedDates = new ArrayList<>(hoursByDate.keySet());
        Collections.sort(sortedDates);

        List<CollaboratorReportDTO.ProductivityPeriodDTO> periods = new ArrayList<>();
        LocalDate periodStart = null;
        LocalDate periodEnd = null;
        double periodHours = 0.0;

        for (LocalDate date : sortedDates) {
            double hours = hoursByDate.get(date);
            boolean isSignificant = findPeaks ? hours > threshold : hours < threshold;

            if (isSignificant) {
                if (periodStart == null) {
                    // Start a new period
                    periodStart = date;
                    periodEnd = date;
                    periodHours = hours;
                } else if (date.isEqual(periodEnd.plusDays(1))) {
                    // Extend the current period
                    periodEnd = date;
                    periodHours += hours;
                } else {
                    // End the current period and start a new one
                    periods.add(createProductivityPeriod(periodStart, periodEnd, periodHours));
                    periodStart = date;
                    periodEnd = date;
                    periodHours = hours;
                }
            } else if (periodStart != null) {
                // End the current period
                periods.add(createProductivityPeriod(periodStart, periodEnd, periodHours));
                periodStart = null;
                periodEnd = null;
                periodHours = 0.0;
            }
        }

        // Add the last period if it exists
        if (periodStart != null) {
            periods.add(createProductivityPeriod(periodStart, periodEnd, periodHours));
        }

        return periods;
    }

    /**
     * Create a productivity period DTO
     */
    private CollaboratorReportDTO.ProductivityPeriodDTO createProductivityPeriod(
            LocalDate start, LocalDate end, double hours) {
        return CollaboratorReportDTO.ProductivityPeriodDTO.builder()
                .startDate(start.format(DATE_FORMATTER))
                .endDate(end.format(DATE_FORMATTER))
                .hours(hours)
                .build();
    }

    /**
     * Generate context details from a list of events
     */
    private CollaboratorReportDTO.ContextDetailsDTO generateContextDetails(List<CalendarEventEntity> events) {
        // Calculate hours by time period
        Map<CollaboratorReportDTO.ContextDetailsDTO.TimePeriod, Double> hoursByTimePeriod = new EnumMap<>(
                CollaboratorReportDTO.ContextDetailsDTO.TimePeriod.class);

        // Calculate session durations
        List<Double> sessionDurations = new ArrayList<>();

        for (CalendarEventEntity event : events) {
            if (event.getStartInstant() == null || event.getEndInstant() == null) {
                continue;
            }

            // Calculate duration in hours
            Duration duration = Duration.between(event.getStartInstant(), event.getEndInstant());
            double hours = duration.toMinutes() / 60.0;

            // Add to session durations (in minutes)
            sessionDurations.add((double) duration.toMinutes());

            // Get hour of day (0-23)
            int hourOfDay = event.getStartInstant().atZone(DEFAULT_ZONE).getHour();

            // Determine time period
            CollaboratorReportDTO.ContextDetailsDTO.TimePeriod timePeriod;
            if (hourOfDay >= 5 && hourOfDay < 12) {
                timePeriod = CollaboratorReportDTO.ContextDetailsDTO.TimePeriod.MORNING;
            } else if (hourOfDay >= 12 && hourOfDay < 18) {
                timePeriod = CollaboratorReportDTO.ContextDetailsDTO.TimePeriod.AFTERNOON;
            } else {
                timePeriod = CollaboratorReportDTO.ContextDetailsDTO.TimePeriod.EVENING;
            }

            // Update hours by time period
            hoursByTimePeriod.put(timePeriod, hoursByTimePeriod.getOrDefault(timePeriod, 0.0) + hours);
        }

        // Calculate average session duration
        double averageSessionDuration = sessionDurations.isEmpty() ? 0.0 :
                sessionDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return CollaboratorReportDTO.ContextDetailsDTO.builder()
                .hoursByTimePeriod(hoursByTimePeriod)
                .averageSessionDurationMinutes(averageSessionDuration)
                .build();
    }

    /**
     * Get start instant from filter
     */
    private Instant getStartInstant(ReportFilterDTO filter) {
        if (filter.getStartDate() != null) {
            return filter.getStartDate();
        }

        // Default to start of current period based on period type
        LocalDate now = LocalDate.now();
        LocalDate startDate;

        if (filter.getPeriodType() == null) {
            // Default to start of month
            startDate = now.withDayOfMonth(1);
        } else {
            switch (filter.getPeriodType()) {
                case WEEK:
                    startDate = now.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
                    break;
                case MONTH:
                    startDate = now.withDayOfMonth(1);
                    break;
                case QUARTER:
                    int quarter = (now.getMonthValue() - 1) / 3;
                    startDate = now.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                    break;
                case YEAR:
                    startDate = now.withDayOfYear(1);
                    break;
                default:
                    startDate = now.withDayOfMonth(1);
            }
        }

        return startDate.atStartOfDay(DEFAULT_ZONE).toInstant();
    }

    /**
     * Get end instant from filter
     */
    private Instant getEndInstant(ReportFilterDTO filter) {
        if (filter.getEndDate() != null) {
            return filter.getEndDate();
        }

        // Default to end of current period based on period type
        LocalDate now = LocalDate.now();
        LocalDate endDate;

        if (filter.getPeriodType() == null) {
            // Default to end of month
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        } else {
            switch (filter.getPeriodType()) {
                case WEEK:
                    endDate = now.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 7);
                    break;
                case MONTH:
                    endDate = now.withDayOfMonth(now.lengthOfMonth());
                    break;
                case QUARTER:
                    int quarter = (now.getMonthValue() - 1) / 3;
                    Month endMonth = Month.of(quarter * 3 + 3);
                    endDate = now.withMonth(endMonth.getValue())
                            .withDayOfMonth(endMonth.length(now.isLeapYear()));
                    break;
                case YEAR:
                    endDate = now.withDayOfYear(now.isLeapYear() ? 366 : 365);
                    break;
                default:
                    endDate = now.withDayOfMonth(now.lengthOfMonth());
            }
        }

        return endDate.atTime(LocalTime.MAX).atZone(DEFAULT_ZONE).toInstant();
    }
}
