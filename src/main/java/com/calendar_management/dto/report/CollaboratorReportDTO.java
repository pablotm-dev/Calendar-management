package com.calendar_management.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for collaborator reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorReportDTO {
    
    /**
     * Email of the collaborator
     */
    private String collaboratorEmail;
    
    /**
     * Total hours worked across all projects and tasks
     */
    private double totalHoursWorked;
    
    /**
     * Hours worked per project
     * Key: Project ID
     * Value: ProjectHoursDTO containing project details and hours
     */
    private List<ProjectHoursDTO> hoursPerProject;
    
    /**
     * Hours worked per task
     * Key: Task ID
     * Value: TaskHoursDTO containing task details and hours
     */
    private List<TaskHoursDTO> hoursPerTask;
    
    /**
     * Productivity analysis data
     */
    private ProductivityAnalysisDTO productivityAnalysis;
    
    /**
     * Context details data
     */
    private ContextDetailsDTO contextDetails;
    
    /**
     * DTO for project hours
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectHoursDTO {
        private Long projectId;
        private String projectName;
        private double hours;
        private String clientName;
    }
    
    /**
     * DTO for task hours
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskHoursDTO {
        private Long taskId;
        private String taskName;
        private double hours;
        private Long projectId;
        private String projectName;
    }
    
    /**
     * DTO for productivity analysis
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivityAnalysisDTO {
        /**
         * Hours worked by day of week
         * Key: Day of week (1-7, where 1 is Monday)
         * Value: Hours worked
         */
        private Map<Integer, Double> hoursByDayOfWeek;
        
        /**
         * Hours worked by month
         * Key: Month (1-12)
         * Value: Hours worked
         */
        private Map<Integer, Double> hoursByMonth;
        
        /**
         * Trend data for hours worked over time
         * Key: Date (ISO format)
         * Value: Hours worked
         */
        private List<TimeSeriesDataPoint> trendData;
        
        /**
         * Peak productivity periods
         */
        private List<ProductivityPeriodDTO> peakPeriods;
        
        /**
         * Low productivity periods
         */
        private List<ProductivityPeriodDTO> lowPeriods;
    }
    
    /**
     * DTO for time series data point
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        private String date;
        private double hours;
    }
    
    /**
     * DTO for productivity period
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivityPeriodDTO {
        private String startDate;
        private String endDate;
        private double hours;
    }
    
    /**
     * DTO for context details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextDetailsDTO {
        /**
         * Hours worked by time of day
         * Key: Time period (MORNING, AFTERNOON, EVENING)
         * Value: Hours worked
         */
        private Map<TimePeriod, Double> hoursByTimePeriod;
        
        /**
         * Average duration of work sessions in minutes
         */
        private double averageSessionDurationMinutes;
        
        /**
         * Enum for time periods
         */
        public enum TimePeriod {
            MORNING,    // 5:00 - 11:59
            AFTERNOON,  // 12:00 - 17:59
            EVENING     // 18:00 - 4:59
        }
    }
}