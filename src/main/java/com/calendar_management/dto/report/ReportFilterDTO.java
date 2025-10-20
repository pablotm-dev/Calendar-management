package com.calendar_management.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for filtering report data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterDTO {
    
    /**
     * Start date for the report period
     */
    private Instant startDate;
    
    /**
     * End date for the report period
     */
    private Instant endDate;
    
    /**
     * Period type (WEEK, MONTH, QUARTER, YEAR)
     */
    private PeriodType periodType;
    
    /**
     * List of collaborator emails to filter by (optional)
     */
    private List<String> collaboratorEmails;
    
    /**
     * List of project IDs to filter by (optional)
     */
    private List<Long> projectIds;
    
    /**
     * List of task IDs to filter by (optional)
     */
    private List<Long> taskIds;
    
    /**
     * Enum for period types
     */
    public enum PeriodType {
        WEEK,
        MONTH,
        QUARTER,
        YEAR
    }
}