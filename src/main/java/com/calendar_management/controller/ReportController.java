package com.calendar_management.controller;

import com.calendar_management.dto.report.ClientReportDTO;
import com.calendar_management.dto.report.CollaboratorReportDTO;
import com.calendar_management.dto.report.ReportFilterDTO;
import com.calendar_management.service.ExcelReportService;
import com.calendar_management.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller for report endpoints
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportingService reportingService;
    private final ExcelReportService excelReportService;

    /**
     * Generate an Excel spreadsheet report for a specific collaborator
     * 
     * @param email Email of the collaborator
     * @param startDateStr Start date for the report period (ISO-8601 format)
     * @param endDateStr End date for the report period (ISO-8601 format)
     * @return Excel spreadsheet with the collaborator's hours
     */
    @GetMapping("/collaborators/{email}/excel")
    public ResponseEntity<byte[]> getCollaboratorExcelReport(
            @PathVariable String email,
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endDateStr
    ) {
        try {
            // Parse date strings to Instant objects with validation
            Instant startDate = null;
            Instant endDate = null;

            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    startDate = Instant.parse(startDateStr);
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest()
                            .body(("Invalid start date format. Use ISO-8601 format (e.g., 2023-10-01T00:00:00Z)").getBytes());
                }
            }

            if (endDateStr != null && !endDateStr.isEmpty()) {
                try {
                    endDate = Instant.parse(endDateStr);
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest()
                            .body(("Invalid end date format. Use ISO-8601 format (e.g., 2023-10-31T23:59:59Z)").getBytes());
                }
            }

            // Generate the Excel report
            byte[] excelBytes = excelReportService.generateCollaboratorExcelReport(email, startDate, endDate);

            // Format the current date for the filename
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .format(LocalDateTime.now(ZoneId.systemDefault()));

            // Set up the response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "relatorio_horas_" + email.replace("@", "_at_") + "_" + timestamp + ".xlsx");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate a report for a specific collaborator
     * 
     * @param email Email of the collaborator
     * @param startDateStr Start date for the report period (ISO-8601 format)
     * @param endDateStr End date for the report period (ISO-8601 format)
     * @param periodType Period type (WEEK, MONTH, QUARTER, YEAR)
     * @param projectIds List of project IDs to filter by
     * @param taskIds List of task IDs to filter by
     * @return The collaborator report
     */
    @GetMapping("/collaborators/{email}")
    public ResponseEntity<CollaboratorReportDTO> getCollaboratorReport(
            @PathVariable String email,
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endDateStr,
            @RequestParam(required = false) ReportFilterDTO.PeriodType periodType,
            @RequestParam(required = false) List<Long> projectIds,
            @RequestParam(required = false) List<Long> taskIds
    ) {
        // Parse date strings to Instant objects with validation
        Instant startDate = null;
        Instant endDate = null;

        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                startDate = Instant.parse(startDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                endDate = Instant.parse(endDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .periodType(periodType)
                .projectIds(projectIds)
                .taskIds(taskIds)
                .build();

        CollaboratorReportDTO report = reportingService.generateCollaboratorReport(email, filter);
        return ResponseEntity.ok(report);
    }

    /**
     * Generate reports for all collaborators
     * 
     * @param startDateStr Start date for the report period (ISO-8601 format)
     * @param endDateStr End date for the report period (ISO-8601 format)
     * @param periodType Period type (WEEK, MONTH, QUARTER, YEAR)
     * @param collaboratorEmails List of collaborator emails to filter by
     * @param projectIds List of project IDs to filter by
     * @param taskIds List of task IDs to filter by
     * @return List of collaborator reports
     */
    @GetMapping("/collaborators")
    public ResponseEntity<List<CollaboratorReportDTO>> getAllCollaboratorReports(
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endDateStr,
            @RequestParam(required = false) ReportFilterDTO.PeriodType periodType,
            @RequestParam(required = false) List<String> collaboratorEmails,
            @RequestParam(required = false) List<Long> projectIds,
            @RequestParam(required = false) List<Long> taskIds
    ) {
        // Parse date strings to Instant objects with validation
        Instant startDate = null;
        Instant endDate = null;

        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                startDate = Instant.parse(startDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                endDate = Instant.parse(endDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .periodType(periodType)
                .collaboratorEmails(collaboratorEmails)
                .projectIds(projectIds)
                .taskIds(taskIds)
                .build();

        List<CollaboratorReportDTO> reports = reportingService.generateAllCollaboratorReports(filter);
        return ResponseEntity.ok(reports);
    }

    /**
     * Generate a report for a specific client
     * 
     * @param clientId ID of the client
     * @param startDateStr Start date for the report period (ISO-8601 format)
     * @param endDateStr End date for the report period (ISO-8601 format)
     * @param periodType Period type (WEEK, MONTH, QUARTER, YEAR)
     * @param collaboratorEmails List of collaborator emails to filter by
     * @param projectIds List of project IDs to filter by
     * @param taskIds List of task IDs to filter by
     * @return The client report
     */
    @GetMapping("/clients/{clientId}")
    public ResponseEntity<ClientReportDTO> getClientReport(
            @PathVariable Long clientId,
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endDateStr,
            @RequestParam(required = false) ReportFilterDTO.PeriodType periodType,
            @RequestParam(required = false) List<String> collaboratorEmails,
            @RequestParam(required = false) List<Long> projectIds,
            @RequestParam(required = false) List<Long> taskIds
    ) {
        // Parse date strings to Instant objects with validation
        Instant startDate = null;
        Instant endDate = null;

        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                startDate = Instant.parse(startDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                endDate = Instant.parse(endDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .periodType(periodType)
                .collaboratorEmails(collaboratorEmails)
                .projectIds(projectIds)
                .taskIds(taskIds)
                .build();

        try {
            ClientReportDTO report = reportingService.generateClientReport(clientId, filter);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate reports for all clients
     * 
     * @param startDateStr Start date for the report period (ISO-8601 format)
     * @param endDateStr End date for the report period (ISO-8601 format)
     * @param periodType Period type (WEEK, MONTH, QUARTER, YEAR)
     * @param collaboratorEmails List of collaborator emails to filter by
     * @param projectIds List of project IDs to filter by
     * @param taskIds List of task IDs to filter by
     * @return List of client reports
     */
    @GetMapping("/clients")
    public ResponseEntity<List<ClientReportDTO>> getAllClientReports(
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endDateStr,
            @RequestParam(required = false) ReportFilterDTO.PeriodType periodType,
            @RequestParam(required = false) List<String> collaboratorEmails,
            @RequestParam(required = false) List<Long> projectIds,
            @RequestParam(required = false) List<Long> taskIds
    ) {
        // Parse date strings to Instant objects with validation
        Instant startDate = null;
        Instant endDate = null;

        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                startDate = Instant.parse(startDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                endDate = Instant.parse(endDateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .periodType(periodType)
                .collaboratorEmails(collaboratorEmails)
                .projectIds(projectIds)
                .taskIds(taskIds)
                .build();

        List<ClientReportDTO> reports = reportingService.generateAllClientReports(filter);
        return ResponseEntity.ok(reports);
    }
    /**
     * Generate an Excel spreadsheet report for a specific client
     * 
     * @param clientId ID of the client
     * @param startDateStr Start date for the report period (ISO-8601 format)
     * @param endDateStr End date for the report period (ISO-8601 format)
     * @return Excel spreadsheet with the client's hours
     */
    @GetMapping("/clients/{clientId}/excel")
    public ResponseEntity<byte[]> getClientExcelReport(
            @PathVariable Long clientId,
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endDateStr
    ) {
        try {
            // Parse date strings to Instant objects with validation
            Instant startDate = null;
            Instant endDate = null;

            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    startDate = Instant.parse(startDateStr);
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest()
                            .body(("Invalid start date format. Use ISO-8601 format (e.g., 2023-10-01T00:00:00Z)").getBytes());
                }
            }

            if (endDateStr != null && !endDateStr.isEmpty()) {
                try {
                    endDate = Instant.parse(endDateStr);
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest()
                            .body(("Invalid end date format. Use ISO-8601 format (e.g., 2023-10-31T23:59:59Z)").getBytes());
                }
            }

            // Generate the Excel report
            byte[] excelBytes = excelReportService.generateClientExcelReport(clientId, startDate, endDate);

            // Format the current date for the filename
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .format(LocalDateTime.now(ZoneId.systemDefault()));

            // Set up the response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "relatorio_cliente_" + clientId + "_" + timestamp + ".xlsx");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
