package com.calendar_management.service;

import com.calendar_management.dto.report.ClientReportDTO;
import com.calendar_management.dto.report.CollaboratorReportDTO;
import com.calendar_management.model.CalendarEventEntity;
import com.calendar_management.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for generating Excel reports from collaborator data
 */
@Service
@RequiredArgsConstructor
public class ExcelReportService {

    private final ReportingService reportingService;
    private final CalendarEventRepository calendarEventRepository;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(DEFAULT_ZONE);

    /**
     * Generate an Excel report for a collaborator
     * 
     * @param email The email of the collaborator
     * @param startDate Start date for the report period
     * @param endDate End date for the report period
     * @return Excel file as byte array
     */
    public byte[] generateCollaboratorExcelReport(String email, Instant startDate, Instant endDate) throws IOException {
        // Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle descriptionDataStyle = createDescriptionDataStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);

            // Get report data
            CollaboratorReportDTO report = reportingService.generateCollaboratorReport(
                    email, 
                    buildReportFilter(startDate, endDate)
            );

            // Get events for the collaborator in the time range
            List<CalendarEventEntity> events = calendarEventRepository
                    .findByUserEmailAndStartInstantGreaterThanEqualAndEndInstantLessThanEqual(
                            email, startDate, endDate);

            // Create consolidated sheet with all data
            createConsolidatedSheet(workbook, report, events, headerStyle, subHeaderStyle, dataStyle, descriptionDataStyle, totalStyle, startDate, endDate);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Generate an Excel report for a client
     * 
     * @param clientId The ID of the client
     * @param startDate Start date for the report period
     * @param endDate End date for the report period
     * @return Excel file as byte array
     */
    public byte[] generateClientExcelReport(Long clientId, Instant startDate, Instant endDate) throws IOException {
        // Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle descriptionDataStyle = createDescriptionDataStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle emptyCellStyle = createEmptyCellStyle(workbook);

            // Get report data
            ClientReportDTO report = reportingService.generateClientReport(
                    clientId, 
                    buildReportFilter(startDate, endDate)
            );

            // Create summary sheet (renamed from client report sheet)
            createClientReportSheet(workbook, report, headerStyle, subHeaderStyle, dataStyle, totalStyle, emptyCellStyle, startDate, endDate);

            // Rename the first sheet to "Resumo"
            workbook.setSheetName(0, "Resumo");

            // Collect all unique collaborators who worked on this client's projects
            Set<String> collaboratorEmails = new HashSet<>();
            for (ClientReportDTO.ProjectReportDTO project : report.getProjects()) {
                for (ClientReportDTO.CollaboratorHoursDTO collaborator : project.getCollaboratorHours()) {
                    collaboratorEmails.add(collaborator.getCollaboratorEmail());
                }

                // Also check tasks
                for (ClientReportDTO.TaskReportDTO task : project.getTasks()) {
                    for (ClientReportDTO.CollaboratorHoursDTO collaborator : task.getCollaboratorHours()) {
                        collaboratorEmails.add(collaborator.getCollaboratorEmail());
                    }
                }
            }

            // For each collaborator, create a tab with the collaborator report layout
            for (String email : collaboratorEmails) {
                // Get collaborator report data filtered for this client
                CollaboratorReportDTO collaboratorReport = reportingService.generateCollaboratorReport(
                        email, 
                        buildReportFilter(startDate, endDate)
                );

                // Filter events for this client only
                List<CalendarEventEntity> events = calendarEventRepository
                        .findByUserEmailAndStartInstantGreaterThanEqualAndEndInstantLessThanEqual(
                                email, startDate, endDate);

                // Filter events to only include those related to this client's projects
                Set<Long> clientProjectIds = report.getProjects().stream()
                        .map(ClientReportDTO.ProjectReportDTO::getProjectId)
                        .collect(Collectors.toSet());

                List<CalendarEventEntity> filteredEvents = events.stream()
                        .filter(event -> event.getTask() != null && 
                                event.getTask().getProjeto() != null &&
                                clientProjectIds.contains(event.getTask().getProjeto().getId()))
                        .collect(Collectors.toList());

                // Filter projects to only include those from this client
                List<CollaboratorReportDTO.ProjectHoursDTO> filteredProjects = collaboratorReport.getHoursPerProject().stream()
                        .filter(project -> clientProjectIds.contains(project.getProjectId()))
                        .collect(Collectors.toList());

                // Filter tasks to only include those from this client's projects
                List<CollaboratorReportDTO.TaskHoursDTO> filteredTasks = collaboratorReport.getHoursPerTask().stream()
                        .filter(task -> clientProjectIds.contains(task.getProjectId()))
                        .collect(Collectors.toList());

                // Create a filtered copy of the collaborator report
                // Calculate total hours from tasks only to avoid double-counting
                double totalTaskHours = filteredTasks.stream().mapToDouble(CollaboratorReportDTO.TaskHoursDTO::getHours).sum();

                CollaboratorReportDTO filteredCollaboratorReport = CollaboratorReportDTO.builder()
                        .collaboratorEmail(collaboratorReport.getCollaboratorEmail())
                        .totalHoursWorked(totalTaskHours)
                        .hoursPerProject(filteredProjects)
                        .hoursPerTask(filteredTasks)
                        .productivityAnalysis(collaboratorReport.getProductivityAnalysis())
                        .contextDetails(collaboratorReport.getContextDetails())
                        .build();

                // Create a sheet for this collaborator with the filtered data
                createCollaboratorTabForClientReport(
                        workbook, 
                        filteredCollaboratorReport, 
                        filteredEvents, 
                        headerStyle, 
                        subHeaderStyle, 
                        dataStyle, 
                        descriptionDataStyle, 
                        totalStyle,
                        emptyCellStyle,
                        startDate, 
                        endDate,
                        report.getClientName()
                );
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create the summary sheet
     */
    private void createSummarySheet(Workbook workbook, CollaboratorReportDTO report, 
                                   CellStyle headerStyle, CellStyle subHeaderStyle, 
                                   CellStyle dataStyle, CellStyle totalStyle) {
        Sheet sheet = workbook.createSheet("Resumo");

        // Set column widths
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);

        // Create title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Relatório de Horas");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Create collaborator info
        Row emailRow = sheet.createRow(2);
        Cell emailLabelCell = emailRow.createCell(0);
        emailLabelCell.setCellValue("Email do Colaborador:");
        emailLabelCell.setCellStyle(subHeaderStyle);

        Cell emailValueCell = emailRow.createCell(1);
        emailValueCell.setCellValue(report.getCollaboratorEmail());
        emailValueCell.setCellStyle(dataStyle);

        // Create total hours
        Row totalHoursRow = sheet.createRow(4);
        Cell totalHoursLabelCell = totalHoursRow.createCell(0);
        totalHoursLabelCell.setCellValue("Total de Horas no Mês:");
        totalHoursLabelCell.setCellStyle(subHeaderStyle);

        Cell totalHoursValueCell = totalHoursRow.createCell(1);
        totalHoursValueCell.setCellValue(report.getTotalHoursWorked());
        totalHoursValueCell.setCellStyle(totalStyle);
    }

    /**
     * Create the projects sheet
     */
    private void createProjectsSheet(Workbook workbook, CollaboratorReportDTO report, 
                                    CellStyle headerStyle, CellStyle subHeaderStyle, 
                                    CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("Projetos");

        // Set column widths
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 8000);
        sheet.setColumnWidth(3, 8000);

        // Create header
        Row headerRow = sheet.createRow(0);
        Cell idHeaderCell = headerRow.createCell(0);
        idHeaderCell.setCellValue("ID");
        idHeaderCell.setCellStyle(headerStyle);

        Cell nameHeaderCell = headerRow.createCell(1);
        nameHeaderCell.setCellValue("Nome do Projeto");
        nameHeaderCell.setCellStyle(headerStyle);

        Cell hoursHeaderCell = headerRow.createCell(2);
        hoursHeaderCell.setCellValue("Horas");
        hoursHeaderCell.setCellStyle(headerStyle);

        Cell clientHeaderCell = headerRow.createCell(3);
        clientHeaderCell.setCellValue("Cliente");
        clientHeaderCell.setCellStyle(headerStyle);

        // Create data rows
        int rowNum = 1;
        for (CollaboratorReportDTO.ProjectHoursDTO project : report.getHoursPerProject()) {
            Row row = sheet.createRow(rowNum++);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(project.getProjectId());
            idCell.setCellStyle(dataStyle);

            Cell nameCell = row.createCell(1);
            nameCell.setCellValue(project.getProjectName());
            nameCell.setCellStyle(dataStyle);

            Cell hoursCell = row.createCell(2);
            hoursCell.setCellValue(project.getHours());
            hoursCell.setCellStyle(dataStyle);

            Cell clientCell = row.createCell(3);
            clientCell.setCellValue(project.getClientName());
            clientCell.setCellStyle(dataStyle);
        }
    }

    /**
     * Create the tasks sheet
     */
    private void createTasksSheet(Workbook workbook, CollaboratorReportDTO report, 
                                 CellStyle headerStyle, CellStyle subHeaderStyle, 
                                 CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("Tasks");

        // Set column widths
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 8000);
        sheet.setColumnWidth(3, 8000);
        sheet.setColumnWidth(4, 8000);

        // Create header
        Row headerRow = sheet.createRow(0);
        Cell idHeaderCell = headerRow.createCell(0);
        idHeaderCell.setCellValue("ID");
        idHeaderCell.setCellStyle(headerStyle);

        Cell nameHeaderCell = headerRow.createCell(1);
        nameHeaderCell.setCellValue("Nome da Task");
        nameHeaderCell.setCellStyle(headerStyle);

        Cell hoursHeaderCell = headerRow.createCell(2);
        hoursHeaderCell.setCellValue("Horas");
        hoursHeaderCell.setCellStyle(headerStyle);

        Cell projectIdHeaderCell = headerRow.createCell(3);
        projectIdHeaderCell.setCellValue("ID Projeto");
        projectIdHeaderCell.setCellStyle(headerStyle);

        Cell projectNameHeaderCell = headerRow.createCell(4);
        projectNameHeaderCell.setCellValue("Nome do Projeto");
        projectNameHeaderCell.setCellStyle(headerStyle);

        // Create data rows
        int rowNum = 1;
        for (CollaboratorReportDTO.TaskHoursDTO task : report.getHoursPerTask()) {
            Row row = sheet.createRow(rowNum++);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(task.getTaskId());
            idCell.setCellStyle(dataStyle);

            Cell nameCell = row.createCell(1);
            nameCell.setCellValue(task.getTaskName());
            nameCell.setCellStyle(dataStyle);

            Cell hoursCell = row.createCell(2);
            hoursCell.setCellValue(task.getHours());
            hoursCell.setCellStyle(dataStyle);

            Cell projectIdCell = row.createCell(3);
            projectIdCell.setCellValue(task.getProjectId());
            projectIdCell.setCellStyle(dataStyle);

            Cell projectNameCell = row.createCell(4);
            projectNameCell.setCellValue(task.getProjectName());
            projectNameCell.setCellStyle(dataStyle);
        }
    }

    /**
     * Create the events sheet
     */
    private void createEventsSheet(Workbook workbook, List<CalendarEventEntity> events, 
                                  CellStyle headerStyle, CellStyle subHeaderStyle, 
                                  CellStyle dataStyle) {
        Sheet sheet = workbook.createSheet("Eventos");

        // Set column widths
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 8000);
        sheet.setColumnWidth(3, 8000);
        sheet.setColumnWidth(4, 8000);

        // Create header
        Row headerRow = sheet.createRow(0);
        Cell summaryHeaderCell = headerRow.createCell(0);
        summaryHeaderCell.setCellValue("Descrição");
        summaryHeaderCell.setCellStyle(headerStyle);

        Cell startHeaderCell = headerRow.createCell(1);
        startHeaderCell.setCellValue("Início");
        startHeaderCell.setCellStyle(headerStyle);

        Cell endHeaderCell = headerRow.createCell(2);
        endHeaderCell.setCellValue("Fim");
        endHeaderCell.setCellStyle(headerStyle);

        Cell hoursHeaderCell = headerRow.createCell(3);
        hoursHeaderCell.setCellValue("Horas");
        hoursHeaderCell.setCellStyle(headerStyle);

        Cell taskHeaderCell = headerRow.createCell(4);
        taskHeaderCell.setCellValue("Task");
        taskHeaderCell.setCellStyle(headerStyle);

        // Create data rows
        int rowNum = 1;
        for (CalendarEventEntity event : events) {
            Row row = sheet.createRow(rowNum++);

            Cell summaryCell = row.createCell(0);
            summaryCell.setCellValue(event.getSummary());
            summaryCell.setCellStyle(dataStyle);

            Cell startCell = row.createCell(1);
            if (event.getStartInstant() != null) {
                startCell.setCellValue(DATE_TIME_FORMATTER.format(event.getStartInstant()));
            }
            startCell.setCellStyle(dataStyle);

            Cell endCell = row.createCell(2);
            if (event.getEndInstant() != null) {
                endCell.setCellValue(DATE_TIME_FORMATTER.format(event.getEndInstant()));
            }
            endCell.setCellStyle(dataStyle);

            Cell hoursCell = row.createCell(3);
            if (event.getStartInstant() != null && event.getEndInstant() != null) {
                double hours = (double) java.time.Duration.between(
                        event.getStartInstant(), event.getEndInstant()).toMinutes() / 60.0;
                hoursCell.setCellValue(hours);
            }
            hoursCell.setCellStyle(dataStyle);

            Cell taskCell = row.createCell(4);
            if (event.getTask() != null) {
                taskCell.setCellValue(event.getTask().getNomeTask());
            }
            taskCell.setCellStyle(dataStyle);
        }
    }

    /**
     * Create header style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create sub-header style
     */
    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Create data style
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        // Add alternate row coloring in the future if needed
        return style;
    }

    /**
     * Create empty cell style without borders for spacing
     */
    private CellStyle createEmptyCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // Removed borders for empty cells used for spacing
        return style;
    }

    /**
     * Create data style for description cells (not centered)
     */
    private CellStyle createDescriptionDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        // No horizontal alignment to keep text left-aligned
        // Add alternate row coloring in the future if needed
        return style;
    }

    /**
     * Create total style
     */
    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Build report filter
     */
    private com.calendar_management.dto.report.ReportFilterDTO buildReportFilter(Instant startDate, Instant endDate) {
        return com.calendar_management.dto.report.ReportFilterDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * Create a consolidated sheet with all data
     */
    private void createConsolidatedSheet(Workbook workbook, CollaboratorReportDTO report, 
                                        List<CalendarEventEntity> events,
                                        CellStyle headerStyle, CellStyle subHeaderStyle, 
                                        CellStyle dataStyle, CellStyle descriptionDataStyle, CellStyle totalStyle,
                                        Instant startDate, Instant endDate) {
        Sheet sheet = workbook.createSheet("Relatório Consolidado");

        // Set column widths - using only 2 columns total
        sheet.setColumnWidth(0, 12000); // Project/Task name or Description column
        sheet.setColumnWidth(1, 12000); // Hours/Client/Project name column

        int rowNum = 0;

        // Create title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RELATÓRIO DE HORAS - " + report.getCollaboratorEmail());
        titleCell.setCellStyle(headerStyle);
        // Ensure title spans both columns (0-1)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRow1 = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow1.createCell(i);
            emptyCell.setCellStyle(createEmptyCellStyle(workbook));
        }

        // Summary section with a separator line
        Row summaryLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = summaryLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row summaryHeaderRow = sheet.createRow(rowNum++);
        Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
        summaryHeaderCell.setCellValue("RESUMO");
        summaryHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row emailRow = sheet.createRow(rowNum++);
        Cell emailLabelCell = emailRow.createCell(0);
        emailLabelCell.setCellValue("Email do Colaborador:");
        emailLabelCell.setCellStyle(subHeaderStyle);

        Cell emailValueCell = emailRow.createCell(1);
        emailValueCell.setCellValue(report.getCollaboratorEmail());
        emailValueCell.setCellStyle(dataStyle);

        Row totalHoursRow = sheet.createRow(rowNum++);
        Cell totalHoursLabelCell = totalHoursRow.createCell(0);
        totalHoursLabelCell.setCellValue("Total de Horas no Período:");
        totalHoursLabelCell.setCellStyle(subHeaderStyle);

        Cell totalHoursValueCell = totalHoursRow.createCell(1);
        totalHoursValueCell.setCellValue(report.getTotalHoursWorked());
        totalHoursValueCell.setCellStyle(totalStyle);

        // Add period information
        Row periodRow = sheet.createRow(rowNum++);
        Cell periodLabelCell = periodRow.createCell(0);
        periodLabelCell.setCellValue("Período do Relatório:");
        periodLabelCell.setCellStyle(subHeaderStyle);

        Cell periodValueCell = periodRow.createCell(1);
        String periodText = "";
        if (startDate != null && endDate != null) {
            String startDateStr = DATE_TIME_FORMATTER.format(startDate);
            String endDateStr = DATE_TIME_FORMATTER.format(endDate);
            periodText = startDateStr + " até " + endDateStr;
        } else if (startDate != null) {
            periodText = "A partir de " + DATE_TIME_FORMATTER.format(startDate);
        } else if (endDate != null) {
            periodText = "Até " + DATE_TIME_FORMATTER.format(endDate);
        } else {
            periodText = "Período completo";
        }
        periodValueCell.setCellValue(periodText);
        periodValueCell.setCellStyle(dataStyle);

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRow2 = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow2.createCell(i);
            emptyCell.setCellStyle(createEmptyCellStyle(workbook));
        }

        // Projects section with a separator line
        Row projectsLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = projectsLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row projectsHeaderRow = sheet.createRow(rowNum++);
        Cell projectsHeaderCell = projectsHeaderRow.createCell(0);
        projectsHeaderCell.setCellValue("HORAS POR PROJETO");
        projectsHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        // Projects header
        Row projectsTableHeaderRow = sheet.createRow(rowNum++);

        Cell projectNameHeaderCell = projectsTableHeaderRow.createCell(0);
        projectNameHeaderCell.setCellValue("Nome do Projeto");
        projectNameHeaderCell.setCellStyle(headerStyle);

        Cell projectHoursHeaderCell = projectsTableHeaderRow.createCell(1);
        projectHoursHeaderCell.setCellValue("Horas / Cliente");
        projectHoursHeaderCell.setCellStyle(headerStyle);

        // Projects data
        for (CollaboratorReportDTO.ProjectHoursDTO project : report.getHoursPerProject()) {
            Row row = sheet.createRow(rowNum++);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(project.getProjectName());
            nameCell.setCellStyle(dataStyle);

            Cell hoursCell = row.createCell(1);
            hoursCell.setCellValue(project.getHours() + " (" + project.getClientName() + ")");
            hoursCell.setCellStyle(dataStyle);
        }

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRowProjects = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRowProjects.createCell(i);
            emptyCell.setCellStyle(createEmptyCellStyle(workbook));
        }

        // Tasks section with a separator line
        Row tasksLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = tasksLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row tasksHeaderRow = sheet.createRow(rowNum++);
        Cell tasksHeaderCell = tasksHeaderRow.createCell(0);
        tasksHeaderCell.setCellValue("HORAS POR TASK");
        tasksHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        // Tasks header
        Row tasksTableHeaderRow = sheet.createRow(rowNum++);

        Cell taskNameHeaderCell = tasksTableHeaderRow.createCell(0);
        taskNameHeaderCell.setCellValue("Nome da Task");
        taskNameHeaderCell.setCellStyle(headerStyle);

        Cell taskHoursHeaderCell = tasksTableHeaderRow.createCell(1);
        taskHoursHeaderCell.setCellValue("Horas / Projeto");
        taskHoursHeaderCell.setCellStyle(headerStyle);

        // Tasks data
        for (CollaboratorReportDTO.TaskHoursDTO task : report.getHoursPerTask()) {
            Row row = sheet.createRow(rowNum++);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(task.getTaskName());
            nameCell.setCellStyle(dataStyle);

            Cell hoursCell = row.createCell(1);
            hoursCell.setCellValue(task.getHours() + " (" + task.getProjectName() + ")");
            hoursCell.setCellStyle(dataStyle);
        }

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRow3 = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow3.createCell(i);
            emptyCell.setCellStyle(createEmptyCellStyle(workbook));
        }

        // Events section - with a separator line
        Row eventsLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = eventsLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row eventsHeaderRow = sheet.createRow(rowNum++);
        Cell eventsHeaderCell = eventsHeaderRow.createCell(0);
        eventsHeaderCell.setCellValue("EVENTOS DO CALENDÁRIO");
        eventsHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        // Events header - using only 2 columns
        Row eventsTableHeaderRow = sheet.createRow(rowNum++);

        Cell eventSummaryHeaderCell = eventsTableHeaderRow.createCell(0);
        eventSummaryHeaderCell.setCellValue("Descrição");
        eventSummaryHeaderCell.setCellStyle(headerStyle);

        Cell eventHoursHeaderCell = eventsTableHeaderRow.createCell(1);
        eventHoursHeaderCell.setCellValue("Horas / Task");
        eventHoursHeaderCell.setCellStyle(headerStyle);

        // Events data - using only 2 columns
        for (CalendarEventEntity event : events) {
            Row row = sheet.createRow(rowNum++);

            Cell summaryCell = row.createCell(0);
            summaryCell.setCellValue(event.getSummary());
            summaryCell.setCellStyle(descriptionDataStyle);

            Cell hoursCell = row.createCell(1);
            String hoursAndTask = "";
            if (event.getStartInstant() != null && event.getEndInstant() != null) {
                double hours = (double) java.time.Duration.between(
                        event.getStartInstant(), event.getEndInstant()).toMinutes() / 60.0;
                hoursAndTask = String.format("%.2f", hours);
            }

            if (event.getTask() != null) {
                hoursAndTask += " (" + event.getTask().getNomeTask() + ")";
            }

            hoursCell.setCellValue(hoursAndTask);
            hoursCell.setCellStyle(dataStyle);
        }
    }

    /**
     * Create a collaborator tab for client report
     */
    private void createCollaboratorTabForClientReport(Workbook workbook, CollaboratorReportDTO report, 
                                        List<CalendarEventEntity> events,
                                        CellStyle headerStyle, CellStyle subHeaderStyle, 
                                        CellStyle dataStyle, CellStyle descriptionDataStyle, CellStyle totalStyle, CellStyle emptyCellStyle,
                                        Instant startDate, Instant endDate,
                                        String clientName) {
        // Create a sheet with the collaborator's email as the name
        Sheet sheet = workbook.createSheet(report.getCollaboratorEmail());

        // Set column widths - using only 2 columns total
        sheet.setColumnWidth(0, 12000); // Project/Task name or Description column
        sheet.setColumnWidth(1, 12000); // Hours/Client/Project name column

        int rowNum = 0;

        // Create title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RELATÓRIO DE HORAS - " + report.getCollaboratorEmail() + " - Cliente: " + clientName);
        titleCell.setCellStyle(headerStyle);
        // Ensure title spans both columns (0-1)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRow1 = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow1.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        // Summary section with a separator line
        Row summaryLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = summaryLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row summaryHeaderRow = sheet.createRow(rowNum++);
        Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
        summaryHeaderCell.setCellValue("RESUMO");
        summaryHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row emailRow = sheet.createRow(rowNum++);
        Cell emailLabelCell = emailRow.createCell(0);
        emailLabelCell.setCellValue("Email do Colaborador:");
        emailLabelCell.setCellStyle(subHeaderStyle);

        Cell emailValueCell = emailRow.createCell(1);
        emailValueCell.setCellValue(report.getCollaboratorEmail());
        emailValueCell.setCellStyle(dataStyle);

        Row totalHoursRow = sheet.createRow(rowNum++);
        Cell totalHoursLabelCell = totalHoursRow.createCell(0);
        totalHoursLabelCell.setCellValue("Total de Horas no Período:");
        totalHoursLabelCell.setCellStyle(subHeaderStyle);

        Cell totalHoursValueCell = totalHoursRow.createCell(1);
        totalHoursValueCell.setCellValue(report.getTotalHoursWorked());
        totalHoursValueCell.setCellStyle(totalStyle);

        // Add client information
        Row clientRow = sheet.createRow(rowNum++);
        Cell clientLabelCell = clientRow.createCell(0);
        clientLabelCell.setCellValue("Cliente:");
        clientLabelCell.setCellStyle(subHeaderStyle);

        Cell clientValueCell = clientRow.createCell(1);
        clientValueCell.setCellValue(clientName);
        clientValueCell.setCellStyle(dataStyle);

        // Add period information
        Row periodRow = sheet.createRow(rowNum++);
        Cell periodLabelCell = periodRow.createCell(0);
        periodLabelCell.setCellValue("Período do Relatório:");
        periodLabelCell.setCellStyle(subHeaderStyle);

        Cell periodValueCell = periodRow.createCell(1);
        String periodText = "";
        if (startDate != null && endDate != null) {
            String startDateStr = DATE_TIME_FORMATTER.format(startDate);
            String endDateStr = DATE_TIME_FORMATTER.format(endDate);
            periodText = startDateStr + " até " + endDateStr;
        } else if (startDate != null) {
            periodText = "A partir de " + DATE_TIME_FORMATTER.format(startDate);
        } else if (endDate != null) {
            periodText = "Até " + DATE_TIME_FORMATTER.format(endDate);
        } else {
            periodText = "Período completo";
        }
        periodValueCell.setCellValue(periodText);
        periodValueCell.setCellStyle(dataStyle);

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRow2 = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow2.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        // Projects section with a separator line
        Row projectsLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = projectsLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row projectsHeaderRow = sheet.createRow(rowNum++);
        Cell projectsHeaderCell = projectsHeaderRow.createCell(0);
        projectsHeaderCell.setCellValue("HORAS POR PROJETO");
        projectsHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        // Projects header
        Row projectsTableHeaderRow = sheet.createRow(rowNum++);

        Cell projectNameHeaderCell = projectsTableHeaderRow.createCell(0);
        projectNameHeaderCell.setCellValue("Nome do Projeto");
        projectNameHeaderCell.setCellStyle(headerStyle);

        Cell projectHoursHeaderCell = projectsTableHeaderRow.createCell(1);
        projectHoursHeaderCell.setCellValue("Horas / Cliente");
        projectHoursHeaderCell.setCellStyle(headerStyle);

        // Projects data
        for (CollaboratorReportDTO.ProjectHoursDTO project : report.getHoursPerProject()) {
            Row row = sheet.createRow(rowNum++);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(project.getProjectName());
            nameCell.setCellStyle(dataStyle);

            Cell hoursCell = row.createCell(1);
            hoursCell.setCellValue(project.getHours() + " (" + project.getClientName() + ")");
            hoursCell.setCellStyle(dataStyle);
        }

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRowProjects = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRowProjects.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        // Tasks section with a separator line
        Row tasksLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = tasksLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row tasksHeaderRow = sheet.createRow(rowNum++);
        Cell tasksHeaderCell = tasksHeaderRow.createCell(0);
        tasksHeaderCell.setCellValue("HORAS POR TASK");
        tasksHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        // Tasks header
        Row tasksTableHeaderRow = sheet.createRow(rowNum++);

        Cell taskNameHeaderCell = tasksTableHeaderRow.createCell(0);
        taskNameHeaderCell.setCellValue("Nome da Task");
        taskNameHeaderCell.setCellStyle(headerStyle);

        Cell taskHoursHeaderCell = tasksTableHeaderRow.createCell(1);
        taskHoursHeaderCell.setCellValue("Horas / Projeto");
        taskHoursHeaderCell.setCellStyle(headerStyle);

        // Tasks data
        for (CollaboratorReportDTO.TaskHoursDTO task : report.getHoursPerTask()) {
            Row row = sheet.createRow(rowNum++);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(task.getTaskName());
            nameCell.setCellStyle(dataStyle);

            Cell hoursCell = row.createCell(1);
            hoursCell.setCellValue(task.getHours() + " (" + task.getProjectName() + ")");
            hoursCell.setCellStyle(dataStyle);
        }

        rowNum++; // Empty row for spacing
        // Create empty row with borders
        Row emptyRow3 = sheet.createRow(rowNum - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow3.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        // Events section - with a separator line
        Row eventsLineRow = sheet.createRow(rowNum++);
        for (int i = 0; i < 2; i++) {
            Cell cell = eventsLineRow.createCell(i);
            cell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        Row eventsHeaderRow = sheet.createRow(rowNum++);
        Cell eventsHeaderCell = eventsHeaderRow.createCell(0);
        eventsHeaderCell.setCellValue("EVENTOS DO CALENDÁRIO");
        eventsHeaderCell.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 1));

        // Events header - using only 2 columns
        Row eventsTableHeaderRow = sheet.createRow(rowNum++);

        Cell eventSummaryHeaderCell = eventsTableHeaderRow.createCell(0);
        eventSummaryHeaderCell.setCellValue("Descrição");
        eventSummaryHeaderCell.setCellStyle(headerStyle);

        Cell eventHoursHeaderCell = eventsTableHeaderRow.createCell(1);
        eventHoursHeaderCell.setCellValue("Horas / Task");
        eventHoursHeaderCell.setCellStyle(headerStyle);

        // Events data - using only 2 columns
        for (CalendarEventEntity event : events) {
            Row row = sheet.createRow(rowNum++);

            Cell summaryCell = row.createCell(0);
            summaryCell.setCellValue(event.getSummary());
            summaryCell.setCellStyle(descriptionDataStyle);

            Cell hoursCell = row.createCell(1);
            String hoursAndTask = "";
            if (event.getStartInstant() != null && event.getEndInstant() != null) {
                double hours = (double) java.time.Duration.between(
                        event.getStartInstant(), event.getEndInstant()).toMinutes() / 60.0;
                hoursAndTask = String.format("%.2f", hours);
            }

            if (event.getTask() != null) {
                hoursAndTask += " (" + event.getTask().getNomeTask() + ")";
            }

            hoursCell.setCellValue(hoursAndTask);
            hoursCell.setCellStyle(dataStyle);
        }
    }
    /**
     * Create a sheet with client report data
     * 
     * @param workbook The workbook to add the sheet to
     * @param report The client report data
     * @param headerStyle Style for headers
     * @param subHeaderStyle Style for subheaders
     * @param dataStyle Style for data cells
     * @param totalStyle Style for total cells
     * @param startDate Start date for the report period
     * @param endDate End date for the report period
     */
    private void createClientReportSheet(Workbook workbook, ClientReportDTO report, 
                                        CellStyle headerStyle, CellStyle subHeaderStyle, 
                                        CellStyle dataStyle, CellStyle totalStyle, CellStyle emptyCellStyle,
                                        Instant startDate, Instant endDate) {
        Sheet sheet = workbook.createSheet("Relatório de Cliente");

        // Set column widths - using only 2 columns total
        sheet.setColumnWidth(0, 12000); // Project/Task/Name
        sheet.setColumnWidth(1, 12000); // Hours/Collaborator

        int rowIndex = 0;

        // Create title
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Relatório de Horas - Cliente: " + report.getClientName());
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Add period information
        rowIndex++;
        // Create empty row with borders
        Row emptyRow1 = sheet.createRow(rowIndex - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow1.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        Row periodRow = sheet.createRow(rowIndex++);
        Cell periodLabelCell = periodRow.createCell(0);
        periodLabelCell.setCellValue("Período do Relatório:");
        periodLabelCell.setCellStyle(subHeaderStyle);

        Cell periodValueCell = periodRow.createCell(1);
        String periodText = "";
        if (startDate != null && endDate != null) {
            periodText = DATE_TIME_FORMATTER.format(startDate) + " até " + DATE_TIME_FORMATTER.format(endDate);
        } else if (startDate != null) {
            periodText = "A partir de " + DATE_TIME_FORMATTER.format(startDate);
        } else if (endDate != null) {
            periodText = "Até " + DATE_TIME_FORMATTER.format(endDate);
        } else {
            periodText = "Todo o período";
        }
        periodValueCell.setCellValue(periodText);
        periodValueCell.setCellStyle(dataStyle);

        // Add client info
        rowIndex++;
        // Create empty row with borders
        Row emptyRow2 = sheet.createRow(rowIndex - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow2.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        Row clientRow = sheet.createRow(rowIndex++);
        Cell clientLabelCell = clientRow.createCell(0);
        clientLabelCell.setCellValue("Cliente:");
        clientLabelCell.setCellStyle(subHeaderStyle);

        Cell clientValueCell = clientRow.createCell(1);
        clientValueCell.setCellValue(report.getClientName() + " (ID: " + report.getClientId() + ")");
        clientValueCell.setCellStyle(dataStyle);

        // Add separator for PROJETOS section
        rowIndex++;
        // Create empty row with borders
        Row emptyRow3 = sheet.createRow(rowIndex - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow3.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        Row projectsSeparatorRow = sheet.createRow(rowIndex++);
        Cell projectsSeparatorCell = projectsSeparatorRow.createCell(0);
        projectsSeparatorCell.setCellValue("PROJETOS");
        projectsSeparatorCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 1));

        // Add project header
        Row projectHeaderRow = sheet.createRow(rowIndex++);
        Cell projectNameHeaderCell = projectHeaderRow.createCell(0);
        projectNameHeaderCell.setCellValue("Nome do Projeto");
        projectNameHeaderCell.setCellStyle(subHeaderStyle);

        Cell projectHoursHeaderCell = projectHeaderRow.createCell(1);
        projectHoursHeaderCell.setCellValue("Horas Totais");
        projectHoursHeaderCell.setCellStyle(subHeaderStyle);

        // Add projects
        double totalClientHours = 0;
        for (ClientReportDTO.ProjectReportDTO project : report.getProjects()) {
            Row projectRow = sheet.createRow(rowIndex++);

            Cell projectNameCell = projectRow.createCell(0);
            projectNameCell.setCellValue(project.getProjectName());
            projectNameCell.setCellStyle(dataStyle);

            Cell projectHoursCell = projectRow.createCell(1);
            projectHoursCell.setCellValue(project.getTotalHours());
            projectHoursCell.setCellStyle(dataStyle);

            totalClientHours += project.getTotalHours();
        }


        // Add separator for COLABORADORES section
        rowIndex++;
        // Create empty row with borders
        Row emptyRow4 = sheet.createRow(rowIndex - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRow4.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        Row collaboratorsSeparatorRow = sheet.createRow(rowIndex++);
        Cell collaboratorsSeparatorCell = collaboratorsSeparatorRow.createCell(0);
        collaboratorsSeparatorCell.setCellValue("COLABORADORES POR PROJETO");
        collaboratorsSeparatorCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 1));

        // Create a nested map to aggregate hours per collaborator per project
        // Map<ProjectId, Map<CollaboratorEmail, Hours>>
        Map<Long, Map<String, Double>> projectCollaboratorHoursMap = new HashMap<>();

        // Initialize the map with all projects
        for (ClientReportDTO.ProjectReportDTO project : report.getProjects()) {
            projectCollaboratorHoursMap.put(project.getProjectId(), new HashMap<>());
        }

        // Aggregate hours from tasks by project and collaborator
        for (ClientReportDTO.ProjectReportDTO project : report.getProjects()) {
            Map<String, Double> collaboratorHoursForProject = projectCollaboratorHoursMap.get(project.getProjectId());

            // Aggregate hours from tasks only to avoid double-counting
            for (ClientReportDTO.TaskReportDTO task : project.getTasks()) {
                for (ClientReportDTO.CollaboratorHoursDTO collaborator : task.getCollaboratorHours()) {
                    collaboratorHoursForProject.merge(
                        collaborator.getCollaboratorEmail(), 
                        collaborator.getHours(), 
                        Double::sum
                    );
                }
            }
        }

        // Display collaborator hours grouped by project
        for (ClientReportDTO.ProjectReportDTO project : report.getProjects()) {
            // Add project header
            Row projectCollabHeaderRow = sheet.createRow(rowIndex++);
            Cell projectHeaderCell = projectCollabHeaderRow.createCell(0);
            projectHeaderCell.setCellValue("PROJETO: " + project.getProjectName());
            projectHeaderCell.setCellStyle(subHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 1));

            // Add collaborator header for this project
            Row collaboratorHeaderRow = sheet.createRow(rowIndex++);
            Cell collaboratorNameHeaderCell = collaboratorHeaderRow.createCell(0);
            collaboratorNameHeaderCell.setCellValue("Nome do Colaborador");
            collaboratorNameHeaderCell.setCellStyle(subHeaderStyle);

            Cell collaboratorHoursHeaderCell = collaboratorHeaderRow.createCell(1);
            collaboratorHoursHeaderCell.setCellValue("Horas");
            collaboratorHoursHeaderCell.setCellStyle(subHeaderStyle);

            // Get collaborator hours for this project
            Map<String, Double> collaboratorHoursForProject = projectCollaboratorHoursMap.get(project.getProjectId());

            // Add collaborator rows for this project
            if (collaboratorHoursForProject.isEmpty()) {
                Row noDataRow = sheet.createRow(rowIndex++);
                Cell noDataCell = noDataRow.createCell(0);
                noDataCell.setCellValue("Nenhum colaborador registrou horas neste projeto");
                noDataCell.setCellStyle(dataStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 1));
            } else {
                for (Map.Entry<String, Double> entry : collaboratorHoursForProject.entrySet()) {
                    Row collaboratorRow = sheet.createRow(rowIndex++);

                    Cell collaboratorNameCell = collaboratorRow.createCell(0);
                    collaboratorNameCell.setCellValue(entry.getKey());
                    collaboratorNameCell.setCellStyle(dataStyle);

                    Cell collaboratorHoursCell = collaboratorRow.createCell(1);
                    collaboratorHoursCell.setCellValue(entry.getValue());
                    collaboratorHoursCell.setCellStyle(dataStyle);
                }
            }

            // Add spacing between projects
            rowIndex++;
            // Create empty row with borders
            Row emptyRowBetweenProjects = sheet.createRow(rowIndex - 1);
            for (int i = 0; i < 2; i++) {
                Cell emptyCell = emptyRowBetweenProjects.createCell(i);
                emptyCell.setCellStyle(emptyCellStyle);
            }
        }

        // Also create a summary of total hours per collaborator across all projects
        rowIndex++;
        // Create empty row with borders
        Row emptyRowBeforeTotalCollaborators = sheet.createRow(rowIndex - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRowBeforeTotalCollaborators.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        Row totalCollaboratorsSeparatorRow = sheet.createRow(rowIndex++);
        Cell totalCollaboratorsSeparatorCell = totalCollaboratorsSeparatorRow.createCell(0);
        totalCollaboratorsSeparatorCell.setCellValue("TOTAL DE HORAS POR COLABORADOR");
        totalCollaboratorsSeparatorCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 1));

        // Add collaborator header for totals
        Row totalCollaboratorHeaderRow = sheet.createRow(rowIndex++);
        Cell totalCollaboratorNameHeaderCell = totalCollaboratorHeaderRow.createCell(0);
        totalCollaboratorNameHeaderCell.setCellValue("Nome do Colaborador");
        totalCollaboratorNameHeaderCell.setCellStyle(subHeaderStyle);

        Cell totalCollaboratorHoursHeaderCell = totalCollaboratorHeaderRow.createCell(1);
        totalCollaboratorHoursHeaderCell.setCellValue("Horas Totais");
        totalCollaboratorHoursHeaderCell.setCellStyle(subHeaderStyle);

        // Create a map to aggregate total hours per collaborator across all projects
        Map<String, Double> totalCollaboratorHoursMap = new HashMap<>();

        // Aggregate hours from all projects
        for (Map<String, Double> collaboratorHoursForProject : projectCollaboratorHoursMap.values()) {
            for (Map.Entry<String, Double> entry : collaboratorHoursForProject.entrySet()) {
                totalCollaboratorHoursMap.merge(
                    entry.getKey(),
                    entry.getValue(),
                    Double::sum
                );
            }
        }

        // Add total collaborator rows
        for (Map.Entry<String, Double> entry : totalCollaboratorHoursMap.entrySet()) {
            Row collaboratorRow = sheet.createRow(rowIndex++);

            Cell collaboratorNameCell = collaboratorRow.createCell(0);
            collaboratorNameCell.setCellValue(entry.getKey());
            collaboratorNameCell.setCellStyle(dataStyle);

            Cell collaboratorHoursCell = collaboratorRow.createCell(1);
            collaboratorHoursCell.setCellValue(entry.getValue());
            collaboratorHoursCell.setCellStyle(dataStyle);
        }

        // Add total hours
        rowIndex++;
        // Create empty row with borders
        Row emptyRowBeforeTotalHours = sheet.createRow(rowIndex - 1);
        for (int i = 0; i < 2; i++) {
            Cell emptyCell = emptyRowBeforeTotalHours.createCell(i);
            emptyCell.setCellStyle(emptyCellStyle);
        }

        Row totalRow = sheet.createRow(rowIndex++);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("TOTAL DE HORAS:");
        totalLabelCell.setCellStyle(headerStyle);

        Cell totalValueCell = totalRow.createCell(1);
        totalValueCell.setCellValue(totalClientHours);
        totalValueCell.setCellStyle(totalStyle);
    }
}
