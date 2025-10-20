package com.calendar_management.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for client reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientReportDTO {
    
    /**
     * Client ID
     */
    private Long clientId;
    
    /**
     * Client name
     */
    private String clientName;
    
    /**
     * List of project reports for this client
     */
    private List<ProjectReportDTO> projects;
    
    /**
     * DTO for project reports
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectReportDTO {
        /**
         * Project ID
         */
        private Long projectId;
        
        /**
         * Project name
         */
        private String projectName;
        
        /**
         * Total hours spent on this project
         */
        private double totalHours;
        
        /**
         * Hours per collaborator for this project
         */
        private List<CollaboratorHoursDTO> collaboratorHours;
        
        /**
         * Task reports for this project
         */
        private List<TaskReportDTO> tasks;
    }
    
    /**
     * DTO for collaborator hours
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollaboratorHoursDTO {
        /**
         * Collaborator email
         */
        private String collaboratorEmail;
        
        /**
         * Hours spent by this collaborator
         */
        private double hours;
    }
    
    /**
     * DTO for task reports
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskReportDTO {
        /**
         * Task ID
         */
        private Long taskId;
        
        /**
         * Task name
         */
        private String taskName;
        
        /**
         * Total hours spent on this task
         */
        private double totalHours;
        
        /**
         * Hours per collaborator for this task
         */
        private List<CollaboratorHoursDTO> collaboratorHours;
    }
}