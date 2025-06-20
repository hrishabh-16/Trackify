package com.trackify.service.impl;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.trackify.dto.request.ReportRequest;
import com.trackify.dto.response.ReportResponse;
import com.trackify.entity.*;
import com.trackify.enums.ApprovalStatus;
import com.trackify.enums.ExpenseStatus;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.exception.ForbiddenException;
import com.trackify.repository.*;
import com.trackify.service.ReportService;
import com.trackify.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);
    private final Map<String, List<String>> sharedReports = new HashMap<>();

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ApprovalWorkflowRepository approvalWorkflowRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TeamMemberRepository teamMemberRepository;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    private final Map<String, List<ReportResponse.ScheduledReportInfo>> userScheduledReports = new HashMap<>();

    private final Map<Long, String> scheduleOwnerMap = new HashMap<>();

 
    // In-memory storage for reports (in production, use database or file storage)
    private final Map<String, ReportResponse> reportStorage = new HashMap<>();
    private final Map<String, byte[]> reportFiles = new HashMap<>();

    @Override
    public ReportResponse generateExpenseReport(ReportRequest.ExpenseReportRequest request, String username) {
        try {
            logger.info("Generating expense report for user: {} from {} to {}", 
                    username, request.getStartDate(), request.getEndDate());

            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            // Get expense data with proper error handling
            List<Expense> expenses = getExpensesForReport(user.getId(), request);
            
            // Create report response
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("EXPENSE");
            report.setReportName("Expense Report - " + request.getStartDate() + " to " + request.getEndDate());
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));

            // Calculate summary with null safety
            ReportResponse.ReportSummary summary = calculateExpenseSummary(expenses);
            report.setSummary(summary);

            // Prepare report data
            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setExpenses(convertToExpenseItems(expenses));
            
            if ("CATEGORY".equals(request.getGroupBy())) {
                data.setCategorySummaries(calculateCategorySummaries(expenses));
            }
            
            report.setData(data);

            // Generate charts - check both format and includeCharts
            if ("PDF".equals(request.getFormat()) || Boolean.TRUE.equals(request.getIncludeCharts())) {
                List<ReportResponse.ReportChart> charts = generateExpenseCharts(expenses, request.getGroupBy());
                report.setCharts(charts);
            }

            // Generate report file
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            // Store report
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            logger.info("Successfully generated expense report: {}", reportId);
            return report;

        } catch (Exception e) {
            logger.error("Error generating expense report for user: {}", username, e);
            throw new RuntimeException("Failed to generate expense report: " + e.getMessage(), e);
        }
    }

    // Helper method with proper null checking and error handling
    private List<Expense> getExpensesForReport(Long userId, ReportRequest.ExpenseReportRequest request) {
        try {
            LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
            LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
            
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDateTime, endDateTime);
            
            if (expenses == null) {
                expenses = new ArrayList<>();
            }
            
            // Apply filters with null safety
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                expenses = expenses.stream()
                        .filter(e -> e.getCategory() != null && request.getCategoryIds().contains(e.getCategory().getId()))
                        .collect(Collectors.toList());
            }
            
            if (request.getExpenseStatuses() != null && !request.getExpenseStatuses().isEmpty()) {
                expenses = expenses.stream()
                        .filter(e -> e.getStatus() != null && request.getExpenseStatuses().contains(e.getStatus().name()))
                        .collect(Collectors.toList());
            }
            
            if (request.getMinAmount() != null) {
                expenses = expenses.stream()
                        .filter(e -> e.getAmount() != null && e.getAmount().compareTo(request.getMinAmount()) >= 0)
                        .collect(Collectors.toList());
            }
            
            if (request.getMaxAmount() != null) {
                expenses = expenses.stream()
                        .filter(e -> e.getAmount() != null && e.getAmount().compareTo(request.getMaxAmount()) <= 0)
                        .collect(Collectors.toList());
            }
            
            return expenses;
        } catch (Exception e) {
            logger.error("Error fetching expenses for report", e);
            return new ArrayList<>();
        }
    }

    private ReportResponse.ReportSummary calculateExpenseSummary(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new ReportResponse.ReportSummary(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }

        BigDecimal totalAmount = expenses.stream()
                .filter(e -> e.getAmount() != null)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = expenses.size();

        BigDecimal averageAmount = totalCount > 0 ? 
                totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;

        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalAmount, totalCount, averageAmount);
        
        if (!expenses.isEmpty()) {
            BigDecimal maxAmount = expenses.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(Expense::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal minAmount = expenses.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(Expense::getAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            summary.setMaxAmount(maxAmount);
            summary.setMinAmount(minAmount);
        }

        return summary;
    }

    private List<ReportResponse.ExpenseItem> convertToExpenseItems(List<Expense> expenses) {
        if (expenses == null) {
            return new ArrayList<>();
        }
        
        return expenses.stream()
                .map(expense -> new ReportResponse.ExpenseItem(
                        expense.getId(),
                        expense.getTitle() != null ? expense.getTitle() : "Unknown",
                        expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO,
                        expense.getExpenseDate() != null ? expense.getExpenseDate() : LocalDate.now(),
                        expense.getCategory() != null ? expense.getCategory().getName() : "Unknown",
                        expense.getUser() != null ? expense.getUser().getUsername() : "Unknown",
                        expense.getStatus() != null ? expense.getStatus().name() : "UNKNOWN"
                ))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.CategorySummary> calculateCategorySummaries(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, List<Expense>> categoryGroups = expenses.stream()
                .collect(Collectors.groupingBy(e -> 
                        e.getCategory() != null ? e.getCategory().getName() : "Unknown"));

        return categoryGroups.entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    List<Expense> categoryExpenses = entry.getValue();
                    
                    BigDecimal totalAmount = categoryExpenses.stream()
                            .filter(e -> e.getAmount() != null)
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return new ReportResponse.CategorySummary(categoryName, totalAmount, (long) categoryExpenses.size());
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.ReportChart> generateExpenseCharts(List<Expense> expenses, String groupBy) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();

        if ("CATEGORY".equals(groupBy) && expenses != null && !expenses.isEmpty()) {
            List<ReportResponse.CategorySummary> categorySummaries = calculateCategorySummaries(expenses);
            
            List<ReportResponse.ChartDataPoint> dataPoints = categorySummaries.stream()
                    .map(summary -> new ReportResponse.ChartDataPoint(summary.getCategoryName(), summary.getTotalAmount()))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("PIE", "Expenses by Category", dataPoints));
        }

        return charts;
    }

    private byte[] generateReportFile(ReportResponse report, String format) {
        try {
            switch (format.toUpperCase()) {
                case "PDF":
                    return generatePDFReport(report);
                case "CSV":
                    return generateCSVReport(report);
                case "XLSX":
                    return generateExcelReport(report);
                case "JSON":
                    return generateJSONReport(report);
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            logger.error("Error generating report file", e);
            return ("Error generating report: " + e.getMessage()).getBytes();
        }
    }

    private byte[] generatePDFReport(ReportResponse report) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph(report.getReportName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Add report info
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            document.add(new Paragraph("Generated: " + report.getGeneratedAt(), infoFont));
            document.add(new Paragraph("Report Type: " + report.getReportType(), infoFont));
            document.add(new Paragraph("Period: " + report.getStartDate() + " to " + report.getEndDate(), infoFont));
            document.add(new Paragraph("Generated By: " + report.getGeneratedBy(), infoFont));
            document.add(Chunk.NEWLINE);
            
            // Add summary
            if (report.getSummary() != null) {
                Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
                document.add(new Paragraph("SUMMARY", summaryFont));
                
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
                document.add(new Paragraph("Total Amount: $" + report.getSummary().getTotalAmount(), normalFont));
                document.add(new Paragraph("Total Count: " + report.getSummary().getTotalCount(), normalFont));
                document.add(new Paragraph("Average Amount: $" + report.getSummary().getAverageAmount(), normalFont));
                document.add(Chunk.NEWLINE);
            }
            
            // Add expenses table
            if (report.getData() != null && report.getData().getExpenses() != null && !report.getData().getExpenses().isEmpty()) {
                Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
                document.add(new Paragraph("EXPENSES", tableHeaderFont));
                
                // Create table
                PdfPTable table = new PdfPTable(7); // 7 columns
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);
                table.setSpacingAfter(10f);
                
                // Set column widths
                float[] columnWidths = {1f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 1f};
                table.setWidths(columnWidths);
                
                // Add headers
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
                BaseColor headerColor = new BaseColor(100, 100, 100);
                
                addTableHeader(table, "ID", headerFont, headerColor);
                addTableHeader(table, "Title", headerFont, headerColor);
                addTableHeader(table, "Amount", headerFont, headerColor);
                addTableHeader(table, "Date", headerFont, headerColor);
                addTableHeader(table, "Category", headerFont, headerColor);
                addTableHeader(table, "User", headerFont, headerColor);
                addTableHeader(table, "Status", headerFont, headerColor);
                
                // Add data rows
                Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
                for (ReportResponse.ExpenseItem expense : report.getData().getExpenses()) {
                    addTableCell(table, expense.getExpenseId().toString(), cellFont);
                    addTableCell(table, expense.getTitle(), cellFont);
                    addTableCell(table, "$" + expense.getAmount(), cellFont);
                    addTableCell(table, expense.getExpenseDate().toString(), cellFont);
                    addTableCell(table, expense.getCategory(), cellFont);
                    addTableCell(table, expense.getUser(), cellFont);
                    addTableCell(table, expense.getStatus(), cellFont);
                }
                
                document.add(table);
            }
            
            // Add category summaries if available
            if (report.getData() != null && report.getData().getCategorySummaries() != null && !report.getData().getCategorySummaries().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Font categoryHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
                document.add(new Paragraph("CATEGORY BREAKDOWN", categoryHeaderFont));
                
                PdfPTable categoryTable = new PdfPTable(3);
                categoryTable.setWidthPercentage(100);
                categoryTable.setSpacingBefore(10f);
                
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
                BaseColor headerColor = new BaseColor(100, 100, 100);
                
                addTableHeader(categoryTable, "Category", headerFont, headerColor);
                addTableHeader(categoryTable, "Total Amount", headerFont, headerColor);
                addTableHeader(categoryTable, "Count", headerFont, headerColor);
                
                Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
                for (ReportResponse.CategorySummary category : report.getData().getCategorySummaries()) {
                    addTableCell(categoryTable, category.getCategoryName(), cellFont);
                    addTableCell(categoryTable, "$" + category.getTotalAmount(), cellFont);
                    addTableCell(categoryTable, category.getExpenseCount().toString(), cellFont);
                }
                
                document.add(categoryTable);
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating PDF report", e);
            return ("Error generating PDF: " + e.getMessage()).getBytes();
        }
    }
    
    private void addTableHeader(PdfPTable table, String headerText, Font font, BaseColor backgroundColor) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(backgroundColor);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerText, font));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPadding(5);
        table.addCell(header);
    }
    
    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase(text, font));
        cell.setPadding(3);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private byte[] generateCSVReport(ReportResponse report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Report Name,").append("\"").append(report.getReportName()).append("\"").append("\n");
        csv.append("Generated At,").append(report.getGeneratedAt()).append("\n");
        csv.append("Type,").append(report.getReportType()).append("\n");
        csv.append("Period,").append(report.getStartDate()).append(" to ").append(report.getEndDate()).append("\n\n");
        
        if (report.getData() != null && report.getData().getExpenses() != null) {
            csv.append("Expense ID,Title,Amount,Date,Category,User,Status\n");
            for (ReportResponse.ExpenseItem expense : report.getData().getExpenses()) {
                csv.append(expense.getExpenseId()).append(",")
                   .append("\"").append(expense.getTitle()).append("\"").append(",")
                   .append(expense.getAmount()).append(",")
                   .append(expense.getExpenseDate()).append(",")
                   .append("\"").append(expense.getCategory()).append("\"").append(",")
                   .append("\"").append(expense.getUser()).append("\"").append(",")
                   .append(expense.getStatus()).append("\n");
            }
        }
        
        return csv.toString().getBytes();
    }

    private byte[] generateExcelReport(ReportResponse report) {
        // Simplified - in production use Apache POI
        return generateCSVReport(report);
    }

    private byte[] generateJSONReport(ReportResponse report) {
        // Simplified JSON generation - in production use Jackson ObjectMapper
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"reportName\": \"").append(report.getReportName()).append("\",\n");
        json.append("  \"generatedAt\": \"").append(report.getGeneratedAt()).append("\",\n");
        json.append("  \"type\": \"").append(report.getReportType()).append("\",\n");
        json.append("  \"startDate\": \"").append(report.getStartDate()).append("\",\n");
        json.append("  \"endDate\": \"").append(report.getEndDate()).append("\",\n");
        
        if (report.getSummary() != null) {
            json.append("  \"summary\": {\n");
            json.append("    \"totalAmount\": ").append(report.getSummary().getTotalAmount()).append(",\n");
            json.append("    \"totalCount\": ").append(report.getSummary().getTotalCount()).append(",\n");
            json.append("    \"averageAmount\": ").append(report.getSummary().getAverageAmount()).append("\n");
            json.append("  },\n");
        }
        
        json.append("  \"expenseCount\": ").append(
            report.getData() != null && report.getData().getExpenses() != null ? 
            report.getData().getExpenses().size() : 0).append("\n");
        json.append("}");
        
        return json.toString().getBytes();
    }

    // Implement all other required methods from interface
    @Override
    public ReportResponse generateBudgetReport(ReportRequest.BudgetReportRequest request, String username) {
        try {
            logger.info("Generating budget report for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            List<Budget> budgets = getBudgetsForReport(user.getId(), request);
            
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("BUDGET");
            report.setReportName("Budget Report - " + request.getStartDate() + " to " + request.getEndDate());
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));

            ReportResponse.ReportSummary summary = calculateBudgetSummary(budgets);
            report.setSummary(summary);

            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setBudgets(convertToBudgetItems(budgets));
            report.setData(data);

            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            return report;
        } catch (Exception e) {
            logger.error("Error generating budget report", e);
            throw new RuntimeException("Failed to generate budget report: " + e.getMessage(), e);
        }
    }

    @Override
    public ReportResponse generateApprovalReport(ReportRequest.ApprovalReportRequest request, String username) {
        try {
            logger.info("Generating approval report for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            List<ApprovalWorkflow> workflows = getApprovalWorkflowsForReport(user.getId(), request);
            
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("APPROVAL");
            report.setReportName("Approval Report - " + request.getStartDate() + " to " + request.getEndDate());
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));

            ReportResponse.ReportSummary summary = calculateApprovalSummary(workflows);
            report.setSummary(summary);

            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setApprovals(convertToApprovalItems(workflows));
            report.setData(data);

            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            return report;
        } catch (Exception e) {
            logger.error("Error generating approval report", e);
            throw new RuntimeException("Failed to generate approval report: " + e.getMessage(), e);
        }
    }

    @Override
    public ReportResponse generateTeamReport(ReportRequest.TeamReportRequest request, String username) {
        try {
            logger.info("Generating team report for team: {}", request.getTeamId());
            
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + request.getTeamId()));

            String reportId = generateReportId();
            List<Expense> teamExpenses = getTeamExpensesForReport(request.getTeamId(), request);
            List<Budget> teamBudgets = getTeamBudgetsForReport(request.getTeamId(), request);
            
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("TEAM");
            report.setReportName("Team Report - " + team.getName());
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));

            ReportResponse.ReportSummary summary = calculateTeamSummary(teamExpenses, teamBudgets);
            report.setSummary(summary);

            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setExpenses(convertToExpenseItems(teamExpenses));
            data.setBudgets(convertToBudgetItems(teamBudgets));
            
            if (Boolean.TRUE.equals(request.getIncludeMemberBreakdown())) {
                data.setUserSummaries(calculateTeamMemberSummaries(teamExpenses));
            }
            
            report.setData(data);

            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            return report;
        } catch (Exception e) {
            logger.error("Error generating team report", e);
            throw new RuntimeException("Failed to generate team report: " + e.getMessage(), e);
        }
    }

    @Override
    public ReportResponse generateCustomReport(ReportRequest request, String username) {
        try {
            logger.info("Generating custom report type: {} for user: {}", request.getReportType(), username);
            
            // Handle CUSTOM report type
            if ("CUSTOM".equals(request.getReportType())) {
                return generateAdvancedCustomReport(request, username);
            }
            
            // Handle other standard report types
            switch (request.getReportType()) {
                case "EXPENSE":
                    ReportRequest.ExpenseReportRequest expenseReq = new ReportRequest.ExpenseReportRequest(
                            request.getStartDate(), request.getEndDate(), request.getFormat());
                    expenseReq.setCategoryIds(request.getCategoryIds());
                    expenseReq.setExpenseStatuses(request.getExpenseStatuses());
                    expenseReq.setMinAmount(request.getMinAmount());
                    expenseReq.setMaxAmount(request.getMaxAmount());
                    expenseReq.setGroupBy(request.getGroupBy());
                    expenseReq.setIncludeCharts(request.getIncludeCharts());
                    return generateExpenseReport(expenseReq, username);

                case "BUDGET":
                    ReportRequest.BudgetReportRequest budgetReq = new ReportRequest.BudgetReportRequest(
                            request.getStartDate(), request.getEndDate(), request.getFormat());
                    budgetReq.setCategoryIds(request.getCategoryIds());
                    budgetReq.setTeamIds(request.getTeamIds());
                    budgetReq.setGroupBy(request.getGroupBy());
                    return generateBudgetReport(budgetReq, username);

                case "APPROVAL":
                    ReportRequest.ApprovalReportRequest approvalReq = new ReportRequest.ApprovalReportRequest(
                            request.getStartDate(), request.getEndDate(), request.getFormat());
                    approvalReq.setApprovalStatuses(request.getExpenseStatuses());
                    approvalReq.setGroupBy(request.getGroupBy());
                    return generateApprovalReport(approvalReq, username);

                case "CATEGORY":
                    return generateCategoryAnalysisReport(request, username);
                    
                case "TEAM":
                    return generateTeamPerformanceReport(request, username);
                    
                case "USER":
                    return generateUserAnalysisReport(request, username);
                    
                case "FINANCIAL":
                    return generateFinancialSummaryReport(request, username);

                default:
                    throw new IllegalArgumentException("Unsupported report type: " + request.getReportType());
            }
        } catch (Exception e) {
            logger.error("Error generating custom report", e);
            throw new RuntimeException("Failed to generate custom report: " + e.getMessage(), e);
        }
    }

    // NEW: Method to handle advanced custom reports
    private ReportResponse generateAdvancedCustomReport(ReportRequest request, String username) {
        try {
            logger.info("Generating advanced custom report for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            // Parse custom parameters
            Map<String, Object> customParams = request.getCustomParameters();
            String analysisType = customParams != null ? (String) customParams.get("analysisType") : "SUMMARY";
            String groupBy = customParams != null ? (String) customParams.get("groupBy") : "CATEGORY";
            
            // Create report response
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("CUSTOM");
            report.setReportName(request.getReportName() != null ? request.getReportName() : 
                    "Custom Analysis Report - " + analysisType);
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));
            
            // Generate custom analysis based on parameters
            ReportResponse.ReportData data = generateCustomAnalysisData(user.getId(), request, analysisType, groupBy);
            report.setData(data);
            
            // Calculate summary
            ReportResponse.ReportSummary summary = generateCustomSummary(data, analysisType);
            report.setSummary(summary);
            
            // Generate charts if requested
            if ("PDF".equals(request.getFormat()) || Boolean.TRUE.equals(request.getIncludeCharts())) {
                List<ReportResponse.ReportChart> charts = generateCustomCharts(data, analysisType, groupBy);
                report.setCharts(charts);
            }

            // Generate report file
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            // Store report
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            logger.info("Successfully generated custom report: {}", reportId);
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating advanced custom report", e);
            throw new RuntimeException("Failed to generate advanced custom report: " + e.getMessage(), e);
        }
    }

    // Helper method to generate custom analysis data
    private ReportResponse.ReportData generateCustomAnalysisData(Long userId, ReportRequest request, String analysisType, String groupBy) {
        ReportResponse.ReportData data = new ReportResponse.ReportData();
        
        try {
            // Get expenses and budgets for the period
            LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
            LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
            
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDateTime, endDateTime);
            List<Budget> budgets = budgetRepository.findByUserIdAndPeriodOverlap(userId, request.getStartDate(), request.getEndDate());
            
            // Apply filters if provided
            expenses = applyExpenseFilters(expenses, request);
            budgets = applyBudgetFilters(budgets, request);
            
            // Set basic data
            data.setExpenses(convertToExpenseItems(expenses));
            data.setBudgets(convertToBudgetItems(budgets));
            
            // Generate analysis based on type
            switch (analysisType.toUpperCase()) {
                case "TREND":
                    data.setTrendAnalysis(generateTrendAnalysis(expenses, groupBy));
                    break;
                case "COMPARISON":
                    data.setComparisonAnalysis(generateComparisonAnalysis(expenses, budgets));
                    break;
                case "VARIANCE":
                    data.setVarianceAnalysis(generateVarianceAnalysis(expenses, budgets));
                    break;
                case "FORECAST":
                    data.setForecastAnalysis(generateForecastAnalysis(expenses, request));
                    break;
                default:
                    data.setCategorySummaries(calculateCategorySummaries(expenses));
                    break;
            }
            
            return data;
            
        } catch (Exception e) {
            logger.error("Error generating custom analysis data", e);
            return new ReportResponse.ReportData();
        }
    }

    // Helper methods for custom analysis
    private List<Expense> applyExpenseFilters(List<Expense> expenses, ReportRequest request) {
        if (expenses == null) return new ArrayList<>();
        
        return expenses.stream()
                .filter(e -> request.getCategoryIds() == null || request.getCategoryIds().isEmpty() ||
                        (e.getCategory() != null && request.getCategoryIds().contains(e.getCategory().getId())))
                .filter(e -> request.getMinAmount() == null ||
                        (e.getAmount() != null && e.getAmount().compareTo(request.getMinAmount()) >= 0))
                .filter(e -> request.getMaxAmount() == null ||
                        (e.getAmount() != null && e.getAmount().compareTo(request.getMaxAmount()) <= 0))
                .collect(Collectors.toList());
    }

    private List<Budget> applyBudgetFilters(List<Budget> budgets, ReportRequest request) {
        if (budgets == null) return new ArrayList<>();
        
        return budgets.stream()
                .filter(b -> request.getCategoryIds() == null || request.getCategoryIds().isEmpty() ||
                        (b.getCategory() != null && request.getCategoryIds().contains(b.getCategory().getId())))
                .filter(b -> request.getTeamIds() == null || request.getTeamIds().isEmpty() ||
                        (b.getTeam() != null && request.getTeamIds().contains(b.getTeam().getId())))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.TrendAnalysis> generateTrendAnalysis(List<Expense> expenses, String groupBy) {
        List<ReportResponse.TrendAnalysis> trends = new ArrayList<>();
        
        if ("CATEGORY".equals(groupBy)) {
            Map<String, List<Expense>> categoryGroups = expenses.stream()
                    .collect(Collectors.groupingBy(e -> 
                            e.getCategory() != null ? e.getCategory().getName() : "Unknown"));
            
            categoryGroups.forEach((category, categoryExpenses) -> {
                BigDecimal totalAmount = categoryExpenses.stream()
                        .filter(e -> e.getAmount() != null)
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                trends.add(new ReportResponse.TrendAnalysis(category, totalAmount, (long) categoryExpenses.size()));
            });
        }
        
        return trends;
    }

    private List<ReportResponse.ComparisonAnalysis> generateComparisonAnalysis(List<Expense> expenses, List<Budget> budgets) {
        List<ReportResponse.ComparisonAnalysis> comparisons = new ArrayList<>();
        
        // Group expenses by category
        Map<String, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().getName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, 
                                e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, 
                                BigDecimal::add)));
        
        // Group budgets by category
        Map<String, BigDecimal> budgetsByCategory = budgets.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategory() != null ? b.getCategory().getName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, 
                                b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO, 
                                BigDecimal::add)));
        
        // Compare expenses vs budgets
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(expensesByCategory.keySet());
        allCategories.addAll(budgetsByCategory.keySet());
        
        for (String category : allCategories) {
            BigDecimal expenseAmount = expensesByCategory.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal budgetAmount = budgetsByCategory.getOrDefault(category, BigDecimal.ZERO);
            
            comparisons.add(new ReportResponse.ComparisonAnalysis(
                    "BUDGET_VS_ACTUAL", category, expenseAmount, budgetAmount));
        }
        
        return comparisons;
    }

    private List<ReportResponse.VarianceAnalysis> generateVarianceAnalysis(List<Expense> expenses, List<Budget> budgets) {
        List<ReportResponse.VarianceAnalysis> variances = new ArrayList<>();
        
        // Group expenses by category
        Map<String, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().getName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, 
                                e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, 
                                BigDecimal::add)));
        
        // Analyze variance for each budget
        for (Budget budget : budgets) {
            String categoryName = budget.getCategory() != null ? budget.getCategory().getName() : "Unknown";
            BigDecimal actualAmount = expensesByCategory.getOrDefault(categoryName, BigDecimal.ZERO);
            BigDecimal budgetedAmount = budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO;
            
            variances.add(new ReportResponse.VarianceAnalysis(categoryName, budgetedAmount, actualAmount));
        }
        
        return variances;
    }

    private List<ReportResponse.ForecastAnalysis> generateForecastAnalysis(List<Expense> expenses, ReportRequest request) {
        List<ReportResponse.ForecastAnalysis> forecasts = new ArrayList<>();
        
        // Simple linear projection based on current period
        Map<String, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().getName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, 
                                e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, 
                                BigDecimal::add)));
        
        // Calculate days in current period
        long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        
        // Project for next month (30 days)
        for (Map.Entry<String, BigDecimal> entry : expensesByCategory.entrySet()) {
            String category = entry.getKey();
            BigDecimal currentAmount = entry.getValue();
            
            // Simple daily average projection
            BigDecimal dailyAverage = daysInPeriod > 0 ? 
                    currentAmount.divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO;
            BigDecimal projectedAmount = dailyAverage.multiply(BigDecimal.valueOf(30));
            
            forecasts.add(new ReportResponse.ForecastAnalysis(
                    category, "NEXT_MONTH", projectedAmount, BigDecimal.valueOf(75))); // 75% confidence
        }
        
        return forecasts;
    }

    private ReportResponse.ReportSummary generateCustomSummary(ReportResponse.ReportData data, String analysisType) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalCount = 0;
        
        if (data.getExpenses() != null) {
            totalAmount = data.getExpenses().stream()
                    .map(ReportResponse.ExpenseItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalCount = data.getExpenses().size();
        }
        
        BigDecimal averageAmount = totalCount > 0 ? 
                totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        
        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalAmount, totalCount, averageAmount);
        
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("analysisType", analysisType);
        additionalMetrics.put("budgetCount", data.getBudgets() != null ? data.getBudgets().size() : 0);
        additionalMetrics.put("trendDataPoints", data.getTrendAnalysis() != null ? data.getTrendAnalysis().size() : 0);
        additionalMetrics.put("comparisonDataPoints", data.getComparisonAnalysis() != null ? data.getComparisonAnalysis().size() : 0);
        additionalMetrics.put("varianceDataPoints", data.getVarianceAnalysis() != null ? data.getVarianceAnalysis().size() : 0);
        summary.setAdditionalMetrics(additionalMetrics);
        
        return summary;
    }

    private List<ReportResponse.ReportChart> generateCustomCharts(ReportResponse.ReportData data, String analysisType, String groupBy) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();
        
        if ("TREND".equals(analysisType) && data.getTrendAnalysis() != null) {
            List<ReportResponse.ChartDataPoint> dataPoints = data.getTrendAnalysis().stream()
                    .map(trend -> new ReportResponse.ChartDataPoint(trend.getCategory(), trend.getAmount()))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("LINE", "Trend Analysis by " + groupBy, dataPoints));
        }
        
        if ("COMPARISON".equals(analysisType) && data.getComparisonAnalysis() != null) {
            List<ReportResponse.ChartDataPoint> dataPoints = data.getComparisonAnalysis().stream()
                    .map(comp -> new ReportResponse.ChartDataPoint(comp.getCategory(), comp.getCurrentAmount()))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("BAR", "Budget vs Actual Comparison", dataPoints));
        }
        
        if ("VARIANCE".equals(analysisType) && data.getVarianceAnalysis() != null) {
            List<ReportResponse.ChartDataPoint> dataPoints = data.getVarianceAnalysis().stream()
                    .map(var -> new ReportResponse.ChartDataPoint(var.getCategory(), var.getVariance()))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("BAR", "Variance Analysis", dataPoints));
        }
        
        return charts;
    }

    // Additional report type implementations (basic implementations)
    private ReportResponse generateCategoryAnalysisReport(ReportRequest request, String username) {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        String reportId = generateReportId();
        
        ReportResponse report = new ReportResponse();
        report.setReportId(reportId);
        report.setReportType("CATEGORY");
        report.setReportName("Category Analysis Report");
        report.setFormat(request.getFormat());
        report.setStartDate(request.getStartDate());
        report.setEndDate(request.getEndDate());
        report.setGeneratedAt(LocalDateTime.now());
        report.setGeneratedBy(username);
        report.setStatus("COMPLETED");
        report.setExpiresAt(LocalDateTime.now().plusDays(30));
        
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(user.getId(), startDateTime, endDateTime);
        
        ReportResponse.ReportData data = new ReportResponse.ReportData();
        data.setCategorySummaries(calculateCategorySummaries(expenses));
        report.setData(data);
        
        ReportResponse.ReportSummary summary = calculateExpenseSummary(expenses);
        report.setSummary(summary);
        
        byte[] reportFile = generateReportFile(report, request.getFormat());
        report.setFileSizeBytes((long) reportFile.length);
        report.setDownloadUrl("/api/reports/" + reportId + "/download");
        
        reportStorage.put(reportId, report);
        reportFiles.put(reportId, reportFile);
        
        return report;
    }

    private ReportResponse generateTeamPerformanceReport(ReportRequest request, String username) {
        try {
            logger.info("Generating team performance report for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            // Get all teams user is part of, or specific teams if provided
            List<Long> teamIds = request.getTeamIds();
            if (teamIds == null || teamIds.isEmpty()) {
                // Get all teams user is a member of
                teamIds = teamMemberRepository.findByUserIdAndIsActiveTrue(user.getId())
                        .stream()
                        .map(teamMember -> teamMember.getTeam().getId())
                        .collect(Collectors.toList());
            }
            
            if (teamIds.isEmpty()) {
                throw new IllegalArgumentException("No teams found for user or specified in request");
            }
            
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("TEAM");
            report.setReportName("Team Performance Report");
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));
            
            // Generate team performance data
            ReportResponse.ReportData data = generateTeamPerformanceData(teamIds, request);
            report.setData(data);
            
            // Calculate team performance summary
            ReportResponse.ReportSummary summary = generateTeamPerformanceSummary(data);
            report.setSummary(summary);
            
            // Generate charts if requested
            if ("PDF".equals(request.getFormat()) || Boolean.TRUE.equals(request.getIncludeCharts())) {
                List<ReportResponse.ReportChart> charts = generateTeamPerformanceCharts(data);
                report.setCharts(charts);
            }
            
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");
            
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);
            
            logger.info("Successfully generated team performance report: {}", reportId);
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating team performance report", e);
            throw new RuntimeException("Failed to generate team performance report: " + e.getMessage(), e);
        }
    }

    private ReportResponse.ReportData generateTeamPerformanceData(List<Long> teamIds, ReportRequest request) {
        ReportResponse.ReportData data = new ReportResponse.ReportData();
        
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        
        List<ReportResponse.TeamSummary> teamSummaries = new ArrayList<>();
        List<ReportResponse.UserSummary> userSummaries = new ArrayList<>();
        List<ReportResponse.PerformanceMetrics> performanceMetrics = new ArrayList<>();
        
        for (Long teamId : teamIds) {
            try {
                Team team = teamRepository.findById(teamId).orElse(null);
                if (team == null) continue;
                
                // Get team expenses
                List<Expense> teamExpenses = expenseRepository.findByTeamId(teamId)
                        .stream()
                        .filter(e -> !e.getExpenseDate().isBefore(request.getStartDate()) &&
                                   !e.getExpenseDate().isAfter(request.getEndDate()))
                        .collect(Collectors.toList());
                
                // Get team budgets
                List<Budget> teamBudgets = budgetRepository.findByTeamIdAndIsActiveTrue(teamId)
                        .stream()
                        .filter(b -> !b.getStartDate().isAfter(request.getEndDate()) &&
                                   !b.getEndDate().isBefore(request.getStartDate()))
                        .collect(Collectors.toList());
                
                // Get team members
                List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId);
                
                // Calculate team metrics
                BigDecimal totalExpenses = teamExpenses.stream()
                        .filter(e -> e.getAmount() != null)
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal totalBudget = teamBudgets.stream()
                        .filter(b -> b.getTotalAmount() != null)
                        .map(Budget::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal budgetUtilization = totalBudget.compareTo(BigDecimal.ZERO) > 0 ?
                        totalExpenses.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                        BigDecimal.ZERO;
                
                // Create team summary
                ReportResponse.TeamSummary teamSummary = new ReportResponse.TeamSummary(
                        team.getName(), totalExpenses, (long) teamExpenses.size(), teamMembers.size());
                teamSummary.setBudgetAmount(totalBudget);
                teamSummary.setBudgetUsedPercentage(budgetUtilization);
                teamSummaries.add(teamSummary);
                
                // Calculate member performance
                Map<Long, List<Expense>> expensesByUser = teamExpenses.stream()
                        .filter(e -> e.getUser() != null)
                        .collect(Collectors.groupingBy(e -> e.getUser().getId()));
                
                for (TeamMember member : teamMembers) {
                    List<Expense> memberExpenses = expensesByUser.getOrDefault(member.getUser().getId(), new ArrayList<>());
                    BigDecimal memberTotal = memberExpenses.stream()
                            .filter(e -> e.getAmount() != null)
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    String fullName = member.getUser().getFirstName() + " " + member.getUser().getLastName();
                    ReportResponse.UserSummary userSummary = new ReportResponse.UserSummary(
                            member.getUser().getUsername(), fullName, memberTotal, (long) memberExpenses.size());
                    
                    // Add role-specific metrics
                    long pendingApprovals = 0;
                    long approvedExpenses = 0;
                    long rejectedExpenses = 0;
                    
                    if (member.getRole() != null && 
                        (member.getRole().name().equals("ADMIN") || member.getRole().name().equals("MANAGER"))) {
                        // Count approvals for managers/admins
                        pendingApprovals = memberExpenses.stream()
                                .filter(e -> e.getStatus() == ExpenseStatus.PENDING)
                                .count();
                        approvedExpenses = memberExpenses.stream()
                                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED)
                                .count();
                        rejectedExpenses = memberExpenses.stream()
                                .filter(e -> e.getStatus() == ExpenseStatus.REJECTED)
                                .count();
                    }
                    
                    userSummary.setPendingApprovals(pendingApprovals);
                    userSummary.setApprovedExpenses(approvedExpenses);
                    userSummary.setRejectedExpenses(rejectedExpenses);
                    userSummaries.add(userSummary);
                }
                
                // Generate performance metrics
                performanceMetrics.add(new ReportResponse.PerformanceMetrics(
                        "Budget Utilization - " + team.getName(), budgetUtilization, BigDecimal.valueOf(80)));
                
                performanceMetrics.add(new ReportResponse.PerformanceMetrics(
                        "Avg Expense per Member - " + team.getName(), 
                        teamMembers.size() > 0 ? totalExpenses.divide(BigDecimal.valueOf(teamMembers.size()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                        BigDecimal.valueOf(1000))); // Target $1000 per member
                
            } catch (Exception e) {
                logger.error("Error processing team: {}", teamId, e);
            }
        }
        
        data.setTeamSummaries(teamSummaries);
        data.setUserSummaries(userSummaries);
        // Note: You'll need to add performanceMetrics to ReportData class
        
        return data;
    }

    private ReportResponse.ReportSummary generateTeamPerformanceSummary(ReportResponse.ReportData data) {
        BigDecimal totalAmount = data.getTeamSummaries().stream()
                .map(ReportResponse.TeamSummary::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalExpenses = data.getTeamSummaries().stream()
                .mapToLong(ReportResponse.TeamSummary::getExpenseCount)
                .sum();
        
        int totalMembers = data.getTeamSummaries().stream()
                .mapToInt(ReportResponse.TeamSummary::getMemberCount)
                .sum();
        
        BigDecimal averageAmount = totalExpenses > 0 ?
                totalAmount.divide(BigDecimal.valueOf(totalExpenses), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        
        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalAmount, totalExpenses, averageAmount);
        
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("totalTeams", data.getTeamSummaries().size());
        additionalMetrics.put("totalMembers", totalMembers);
        additionalMetrics.put("averageTeamSize", data.getTeamSummaries().size() > 0 ? 
                totalMembers / data.getTeamSummaries().size() : 0);
        summary.setAdditionalMetrics(additionalMetrics);
        
        return summary;
    }

    private List<ReportResponse.ReportChart> generateTeamPerformanceCharts(ReportResponse.ReportData data) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();
        
        // Team expense comparison chart
        List<ReportResponse.ChartDataPoint> teamExpensePoints = data.getTeamSummaries().stream()
                .map(team -> new ReportResponse.ChartDataPoint(team.getTeamName(), team.getTotalAmount()))
                .collect(Collectors.toList());
        
        charts.add(new ReportResponse.ReportChart("BAR", "Team Expense Comparison", teamExpensePoints));
        
        // Budget utilization chart
        List<ReportResponse.ChartDataPoint> budgetUtilizationPoints = data.getTeamSummaries().stream()
                .map(team -> new ReportResponse.ChartDataPoint(team.getTeamName(), 
                        team.getBudgetUsedPercentage() != null ? team.getBudgetUsedPercentage() : BigDecimal.ZERO))
                .collect(Collectors.toList());
        
        charts.add(new ReportResponse.ReportChart("BAR", "Budget Utilization by Team", budgetUtilizationPoints));
        
        return charts;
    }

    private ReportResponse generateUserAnalysisReport(ReportRequest request, String username) {
        try {
            logger.info("Generating user analysis report for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("USER");
            report.setReportName("User Analysis Report");
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));
            
            // Generate user analysis data
            ReportResponse.ReportData data = generateUserAnalysisData(user, request);
            report.setData(data);
            
            // Calculate user analysis summary
            ReportResponse.ReportSummary summary = generateUserAnalysisSummary(data, user);
            report.setSummary(summary);
            
            // Generate charts if requested
            if ("PDF".equals(request.getFormat()) || Boolean.TRUE.equals(request.getIncludeCharts())) {
                List<ReportResponse.ReportChart> charts = generateUserAnalysisCharts(data);
                report.setCharts(charts);
            }
            
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");
            
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);
            
            logger.info("Successfully generated user analysis report: {}", reportId);
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating user analysis report", e);
            throw new RuntimeException("Failed to generate user analysis report: " + e.getMessage(), e);
        }
    }

    private ReportResponse.ReportData generateUserAnalysisData(User user, ReportRequest request) {
        ReportResponse.ReportData data = new ReportResponse.ReportData();
        
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        
        // Get user's expenses
        List<Expense> userExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                user.getId(), startDateTime, endDateTime);
        
        // Get user's budgets
        List<Budget> userBudgets = budgetRepository.findByUserIdAndPeriodOverlap(
                user.getId(), request.getStartDate(), request.getEndDate());
        
        // Apply filters
        userExpenses = applyExpenseFilters(userExpenses, request);
        userBudgets = applyBudgetFilters(userBudgets, request);
        
        // Set basic data
        data.setExpenses(convertToExpenseItems(userExpenses));
        data.setBudgets(convertToBudgetItems(userBudgets));
        data.setCategorySummaries(calculateCategorySummaries(userExpenses));
        
        // Generate monthly summaries
        List<ReportResponse.MonthlySummary> monthlySummaries = generateMonthlySummaries(userExpenses);
        data.setMonthlySummaries(monthlySummaries);
        
        // Generate spending patterns analysis
        List<ReportResponse.TrendAnalysis> spendingTrends = generateUserSpendingTrends(userExpenses);
        data.setTrendAnalysis(spendingTrends);
        
        // Generate budget compliance analysis
        List<ReportResponse.VarianceAnalysis> budgetCompliance = generateUserBudgetCompliance(userExpenses, userBudgets);
        data.setVarianceAnalysis(budgetCompliance);
        
        return data;
    }

    private List<ReportResponse.MonthlySummary> generateMonthlySummaries(List<Expense> expenses) {
        Map<String, List<Expense>> expensesByMonth = expenses.stream()
                .collect(Collectors.groupingBy(e -> 
                        e.getExpenseDate().getYear() + "-" + String.format("%02d", e.getExpenseDate().getMonthValue())));
        
        return expensesByMonth.entrySet().stream()
                .map(entry -> {
                    String[] yearMonth = entry.getKey().split("-");
                    int year = Integer.parseInt(yearMonth[0]);
                    int month = Integer.parseInt(yearMonth[1]);
                    String monthName = java.time.Month.of(month).name();
                    
                    List<Expense> monthExpenses = entry.getValue();
                    BigDecimal totalAmount = monthExpenses.stream()
                            .filter(e -> e.getAmount() != null)
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    ReportResponse.MonthlySummary summary = new ReportResponse.MonthlySummary(
                            monthName, year, totalAmount, (long) monthExpenses.size());
                    
                    // Add approval metrics
                    int approvalCount = (int) monthExpenses.stream()
                            .filter(e -> e.getStatus() == ExpenseStatus.APPROVED)
                            .count();
                    summary.setApprovalCount(approvalCount);
                    
                    return summary;
                })
                .sorted((a, b) -> {
                    int yearCompare = a.getYear().compareTo(b.getYear());
                    return yearCompare != 0 ? yearCompare : a.getMonth().compareTo(b.getMonth());
                })
                .collect(Collectors.toList());
    }

    private List<ReportResponse.TrendAnalysis> generateUserSpendingTrends(List<Expense> expenses) {
        List<ReportResponse.TrendAnalysis> trends = new ArrayList<>();
        
        // Analyze spending by category over time
        Map<String, List<Expense>> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(e -> 
                        e.getCategory() != null ? e.getCategory().getName() : "Unknown"));
        
        for (Map.Entry<String, List<Expense>> entry : expensesByCategory.entrySet()) {
            String category = entry.getKey();
            List<Expense> categoryExpenses = entry.getValue();
            
            BigDecimal totalAmount = categoryExpenses.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate trend based on expense frequency and amounts
            String trend = "STABLE";
            if (categoryExpenses.size() > 3) {
                // Simple trend analysis based on recent vs older expenses
                List<Expense> sortedExpenses = categoryExpenses.stream()
                        .sorted((a, b) -> a.getExpenseDate().compareTo(b.getExpenseDate()))
                        .collect(Collectors.toList());
                
                int midPoint = sortedExpenses.size() / 2;
                BigDecimal firstHalfSum = sortedExpenses.subList(0, midPoint).stream()
                        .filter(e -> e.getAmount() != null)
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal secondHalfSum = sortedExpenses.subList(midPoint, sortedExpenses.size()).stream()
                        .filter(e -> e.getAmount() != null)
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (secondHalfSum.compareTo(firstHalfSum.multiply(BigDecimal.valueOf(1.1))) > 0) {
                    trend = "INCREASING";
                } else if (secondHalfSum.compareTo(firstHalfSum.multiply(BigDecimal.valueOf(0.9))) < 0) {
                    trend = "DECREASING";
                }
            }
            
            ReportResponse.TrendAnalysis trendAnalysis = new ReportResponse.TrendAnalysis(
                    category, totalAmount, (long) categoryExpenses.size(), BigDecimal.ZERO, trend);
            trends.add(trendAnalysis);
        }
        
        return trends;
    }

    private List<ReportResponse.VarianceAnalysis> generateUserBudgetCompliance(List<Expense> expenses, List<Budget> budgets) {
        List<ReportResponse.VarianceAnalysis> compliance = new ArrayList<>();
        
        // Group expenses by category
        Map<String, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().getName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, 
                                e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, 
                                BigDecimal::add)));
        
        // Analyze each budget
        for (Budget budget : budgets) {
            String categoryName = budget.getCategory() != null ? budget.getCategory().getName() : "Unknown";
            BigDecimal actualSpending = expensesByCategory.getOrDefault(categoryName, BigDecimal.ZERO);
            BigDecimal budgetAmount = budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO;
            
            ReportResponse.VarianceAnalysis variance = new ReportResponse.VarianceAnalysis(
                    categoryName, budgetAmount, actualSpending);
            
            // Add recommendations
            if ("OVER_BUDGET".equals(variance.getStatus())) {
                variance.setRecommendation("Consider reducing spending in this category or increasing budget allocation");
            } else if ("UNDER_BUDGET".equals(variance.getStatus())) {
                variance.setRecommendation("Good budget control - consider reallocating unused budget to other categories");
            } else {
                variance.setRecommendation("On track with budget - maintain current spending patterns");
            }
            
            compliance.add(variance);
        }
        
        return compliance;
    }

    private ReportResponse.ReportSummary generateUserAnalysisSummary(ReportResponse.ReportData data, User user) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalCount = 0;
        
        if (data.getExpenses() != null) {
            totalAmount = data.getExpenses().stream()
                    .map(ReportResponse.ExpenseItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalCount = data.getExpenses().size();
        }
        
        BigDecimal averageAmount = totalCount > 0 ?
                totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        
        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalAmount, totalCount, averageAmount);
        
        // Find top category and amounts
        if (data.getCategorySummaries() != null && !data.getCategorySummaries().isEmpty()) {
            ReportResponse.CategorySummary topCategory = data.getCategorySummaries().stream()
                    .max((a, b) -> a.getTotalAmount().compareTo(b.getTotalAmount()))
                    .orElse(null);
            
            if (topCategory != null) {
                summary.setTopCategory(topCategory.getCategoryName());
            }
            
            BigDecimal maxAmount = data.getExpenses().stream()
                    .map(ReportResponse.ExpenseItem::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal minAmount = data.getExpenses().stream()
                    .map(ReportResponse.ExpenseItem::getAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            summary.setMaxAmount(maxAmount);
            summary.setMinAmount(minAmount);
        }
        
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("userName", user.getFirstName() + " " + user.getLastName());
        additionalMetrics.put("totalCategories", data.getCategorySummaries() != null ? data.getCategorySummaries().size() : 0);
        additionalMetrics.put("activeBudgets", data.getBudgets() != null ? data.getBudgets().size() : 0);
        additionalMetrics.put("monthsAnalyzed", data.getMonthlySummaries() != null ? data.getMonthlySummaries().size() : 0);
        summary.setAdditionalMetrics(additionalMetrics);
        
        return summary;
    }

    private List<ReportResponse.ReportChart> generateUserAnalysisCharts(ReportResponse.ReportData data) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();
        
        // Category spending pie chart
        if (data.getCategorySummaries() != null) {
            List<ReportResponse.ChartDataPoint> categoryPoints = data.getCategorySummaries().stream()
                    .map(cat -> new ReportResponse.ChartDataPoint(cat.getCategoryName(), cat.getTotalAmount()))
                    .collect(Collectors.toList());
            charts.add(new ReportResponse.ReportChart("PIE", "Spending by Category", categoryPoints));
        }
        
        // Monthly spending trend
        if (data.getMonthlySummaries() != null) {
            List<ReportResponse.ChartDataPoint> monthlyPoints = data.getMonthlySummaries().stream()
                    .map(month -> new ReportResponse.ChartDataPoint(
                            month.getMonth() + " " + month.getYear(), month.getTotalAmount()))
                    .collect(Collectors.toList());
            charts.add(new ReportResponse.ReportChart("LINE", "Monthly Spending Trend", monthlyPoints));
        }
        
        return charts;
    }

    private ReportResponse generateFinancialSummaryReport(ReportRequest request, String username) {
        try {
            logger.info("Generating financial summary report for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("FINANCIAL");
            report.setReportName("Financial Summary Report");
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));
            
            // Generate comprehensive financial data
            ReportResponse.ReportData data = generateFinancialSummaryData(user, request);
            report.setData(data);
            
            // Calculate financial summary
            ReportResponse.ReportSummary summary = generateFinancialSummary(data);
            report.setSummary(summary);
            
            // Generate financial charts
            if ("PDF".equals(request.getFormat()) || Boolean.TRUE.equals(request.getIncludeCharts())) {
                List<ReportResponse.ReportChart> charts = generateFinancialCharts(data);
                report.setCharts(charts);
            }
            
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");
            
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);
            
            logger.info("Successfully generated financial summary report: {}", reportId);
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating financial summary report", e);
            throw new RuntimeException("Failed to generate financial summary report: " + e.getMessage(), e);
        }
    }

    private ReportResponse.ReportData generateFinancialSummaryData(User user, ReportRequest request) {
        ReportResponse.ReportData data = new ReportResponse.ReportData();
        
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        
        // Get all financial data
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                user.getId(), startDateTime, endDateTime);
        List<Budget> budgets = budgetRepository.findByUserIdAndPeriodOverlap(
                user.getId(), request.getStartDate(), request.getEndDate());
        
        // Apply filters
        expenses = applyExpenseFilters(expenses, request);
        budgets = applyBudgetFilters(budgets, request);
        
        // Set basic data
        data.setExpenses(convertToExpenseItems(expenses));
        data.setBudgets(convertToBudgetItems(budgets));
        data.setCategorySummaries(calculateCategorySummaries(expenses));
        data.setMonthlySummaries(generateMonthlySummaries(expenses));
        
        // Generate financial metrics
        List<ReportResponse.FinancialMetrics> financialMetrics = generateFinancialMetrics(expenses, budgets);
        data.setFinancialMetrics(financialMetrics);
        
        // Generate cash flow analysis
        List<ReportResponse.ComparisonAnalysis> cashFlowAnalysis = generateCashFlowAnalysis(expenses, request);
        data.setComparisonAnalysis(cashFlowAnalysis);
        
        // Generate budget vs actual analysis
        List<ReportResponse.VarianceAnalysis> budgetAnalysis = generateUserBudgetCompliance(expenses, budgets);
        data.setVarianceAnalysis(budgetAnalysis);
        
        return data;
    }

    private List<ReportResponse.FinancialMetrics> generateFinancialMetrics(List<Expense> expenses, List<Budget> budgets) {
        List<ReportResponse.FinancialMetrics> metrics = new ArrayList<>();
        
        // Total expenses
        BigDecimal totalExpenses = expenses.stream()
                .filter(e -> e.getAmount() != null)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Total Expenses", "AMOUNT", totalExpenses, "USD", "EXPENSES", 
                "Total amount spent during the period", "GOOD"));
        
        // Total budgets
        BigDecimal totalBudgets = budgets.stream()
                .filter(b -> b.getTotalAmount() != null)
                .map(Budget::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Total Budget Allocated", "AMOUNT", totalBudgets, "USD", "BUDGETS",
                "Total budget allocated for the period", "GOOD"));
        
        // Budget utilization rate
        BigDecimal utilizationRate = totalBudgets.compareTo(BigDecimal.ZERO) > 0 ?
                totalExpenses.divide(totalBudgets, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;
        
        String utilizationStatus = "GOOD";
        if (utilizationRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            utilizationStatus = "CRITICAL"; // Over budget
        } else if (utilizationRate.compareTo(BigDecimal.valueOf(90)) > 0) {
            utilizationStatus = "WARNING"; // Near budget limit
        }
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Budget Utilization Rate", "PERCENTAGE", utilizationRate, "PERCENTAGE", "BUDGETS",
                "Percentage of budget used", utilizationStatus));
        
        // Average expense amount
        BigDecimal avgExpense = expenses.size() > 0 ?
                totalExpenses.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Average Expense Amount", "AMOUNT", avgExpense, "USD", "EXPENSES",
                "Average amount per expense", "GOOD"));
        
        // Number of categories used
        long categoriesUsed = expenses.stream()
                .map(e -> e.getCategory() != null ? e.getCategory().getName() : "Unknown")
                .distinct()
                .count();
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Categories Used", "COUNT", BigDecimal.valueOf(categoriesUsed), "COUNT", "CATEGORIES",
                "Number of different expense categories", "GOOD"));
        
        // Expense frequency (expenses per day)
        long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(
                expenses.stream().map(Expense::getExpenseDate).min(LocalDate::compareTo).orElse(LocalDate.now()),
                expenses.stream().map(Expense::getExpenseDate).max(LocalDate::compareTo).orElse(LocalDate.now())) + 1;
        
        BigDecimal expenseFrequency = daysInPeriod > 0 ?
                BigDecimal.valueOf(expenses.size()).divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Daily Expense Frequency", "RATIO", expenseFrequency, "EXPENSES_PER_DAY", "EXPENSES",
                "Average number of expenses per day", "GOOD"));
        
        // Largest single expense
        BigDecimal largestExpense = expenses.stream()
                .filter(e -> e.getAmount() != null)
                .map(Expense::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Largest Single Expense", "AMOUNT", largestExpense, "USD", "EXPENSES",
                "Highest single expense amount", "GOOD"));
        
        // Budget variance
        BigDecimal budgetVariance = totalBudgets.subtract(totalExpenses);
        String varianceStatus = budgetVariance.compareTo(BigDecimal.ZERO) >= 0 ? "GOOD" : "CRITICAL";
        
        metrics.add(new ReportResponse.FinancialMetrics(
                "Budget Variance", "AMOUNT", budgetVariance, "USD", "BUDGETS",
                "Difference between budget and actual spending", varianceStatus));
        
        return metrics;
     }

     private List<ReportResponse.ComparisonAnalysis> generateCashFlowAnalysis(List<Expense> expenses, ReportRequest request) {
        List<ReportResponse.ComparisonAnalysis> cashFlow = new ArrayList<>();
        
        // Compare current period with previous period of same length
        long periodLengthDays = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        LocalDate previousPeriodStart = request.getStartDate().minusDays(periodLengthDays);
        LocalDate previousPeriodEnd = request.getStartDate().minusDays(1);
        
        try {
            // Get previous period expenses for comparison
            LocalDateTime prevStartDateTime = previousPeriodStart.atStartOfDay();
            LocalDateTime prevEndDateTime = previousPeriodEnd.plusDays(1).atStartOfDay();
            
            List<Expense> previousExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    expenses.get(0).getUser().getId(), prevStartDateTime, prevEndDateTime);
            
            // Current period total
            BigDecimal currentTotal = expenses.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Previous period total
            BigDecimal previousTotal = previousExpenses.stream()
                    .filter(e -> e.getAmount() != null)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Overall cash flow comparison
            ReportResponse.ComparisonAnalysis overallComparison = new ReportResponse.ComparisonAnalysis(
                    "PERIOD_OVER_PERIOD", "Total Expenses", currentTotal, previousTotal);
            overallComparison.setCurrentPeriodStart(request.getStartDate());
            overallComparison.setCurrentPeriodEnd(request.getEndDate());
            overallComparison.setComparisonPeriodStart(previousPeriodStart);
            overallComparison.setComparisonPeriodEnd(previousPeriodEnd);
            overallComparison.setDescription("Comparison with previous period of same length");
            cashFlow.add(overallComparison);
            
            // Category-wise cash flow comparison
            Map<String, BigDecimal> currentCategoryTotals = expenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCategory() != null ? e.getCategory().getName() : "Unknown",
                            Collectors.reducing(BigDecimal.ZERO, 
                                    e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, 
                                    BigDecimal::add)));
            
            Map<String, BigDecimal> previousCategoryTotals = previousExpenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCategory() != null ? e.getCategory().getName() : "Unknown",
                            Collectors.reducing(BigDecimal.ZERO, 
                                    e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, 
                                    BigDecimal::add)));
            
            Set<String> allCategories = new HashSet<>();
            allCategories.addAll(currentCategoryTotals.keySet());
            allCategories.addAll(previousCategoryTotals.keySet());
            
            for (String category : allCategories) {
                BigDecimal currentCategoryTotal = currentCategoryTotals.getOrDefault(category, BigDecimal.ZERO);
                BigDecimal previousCategoryTotal = previousCategoryTotals.getOrDefault(category, BigDecimal.ZERO);
                
                ReportResponse.ComparisonAnalysis categoryComparison = new ReportResponse.ComparisonAnalysis(
                        "CATEGORY_PERIOD_COMPARISON", category, currentCategoryTotal, previousCategoryTotal);
                categoryComparison.setCurrentPeriodStart(request.getStartDate());
                categoryComparison.setCurrentPeriodEnd(request.getEndDate());
                categoryComparison.setComparisonPeriodStart(previousPeriodStart);
                categoryComparison.setComparisonPeriodEnd(previousPeriodEnd);
                categoryComparison.setDescription("Category spending comparison with previous period");
                cashFlow.add(categoryComparison);
            }
            
        } catch (Exception e) {
            logger.error("Error generating cash flow analysis", e);
            // Add a basic current period analysis if comparison fails
            BigDecimal currentTotal = expenses.stream()
                    .filter(exp -> exp.getAmount() != null)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            ReportResponse.ComparisonAnalysis basicAnalysis = new ReportResponse.ComparisonAnalysis(
                    "CURRENT_PERIOD_ONLY", "Total Expenses", currentTotal, BigDecimal.ZERO);
            basicAnalysis.setDescription("Current period analysis (no comparison data available)");
            cashFlow.add(basicAnalysis);
        }
        
        return cashFlow;
     }

     private ReportResponse.ReportSummary generateFinancialSummary(ReportResponse.ReportData data) {
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalBudgets = BigDecimal.ZERO;
        long expenseCount = 0;
        
        if (data.getExpenses() != null) {
            totalExpenses = data.getExpenses().stream()
                    .map(ReportResponse.ExpenseItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            expenseCount = data.getExpenses().size();
        }
        
        if (data.getBudgets() != null) {
            totalBudgets = data.getBudgets().stream()
                    .map(ReportResponse.BudgetItem::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        BigDecimal averageExpense = expenseCount > 0 ?
                totalExpenses.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        
        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalExpenses, expenseCount, averageExpense);
        
        // Calculate financial health indicators
        BigDecimal budgetUtilization = totalBudgets.compareTo(BigDecimal.ZERO) > 0 ?
                totalExpenses.divide(totalBudgets, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;
        
        BigDecimal remainingBudget = totalBudgets.subtract(totalExpenses);
        
        // Find most expensive category
        String topCategory = "N/A";
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        if (data.getCategorySummaries() != null && !data.getCategorySummaries().isEmpty()) {
            ReportResponse.CategorySummary topCat = data.getCategorySummaries().stream()
                    .max((a, b) -> a.getTotalAmount().compareTo(b.getTotalAmount()))
                    .orElse(null);
            if (topCat != null) {
                topCategory = topCat.getCategoryName();
                topCategoryAmount = topCat.getTotalAmount();
            }
        }
        
        summary.setTopCategory(topCategory);
        
        // Find min and max expenses
        if (data.getExpenses() != null && !data.getExpenses().isEmpty()) {
            BigDecimal maxAmount = data.getExpenses().stream()
                    .map(ReportResponse.ExpenseItem::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal minAmount = data.getExpenses().stream()
                    .map(ReportResponse.ExpenseItem::getAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            summary.setMaxAmount(maxAmount);
            summary.setMinAmount(minAmount);
        }
        
        // Additional financial metrics
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("totalBudgetAllocated", totalBudgets);
        additionalMetrics.put("budgetUtilizationPercentage", budgetUtilization);
        additionalMetrics.put("remainingBudget", remainingBudget);
        additionalMetrics.put("topCategoryAmount", topCategoryAmount);
        additionalMetrics.put("categoriesUsed", data.getCategorySummaries() != null ? data.getCategorySummaries().size() : 0);
        additionalMetrics.put("activeBudgets", data.getBudgets() != null ? data.getBudgets().size() : 0);
        additionalMetrics.put("financialHealthScore", calculateFinancialHealthScore(budgetUtilization, expenseCount));
        
        // Financial status determination
        String financialStatus = "HEALTHY";
        if (budgetUtilization.compareTo(BigDecimal.valueOf(100)) > 0) {
            financialStatus = "OVER_BUDGET";
        } else if (budgetUtilization.compareTo(BigDecimal.valueOf(90)) > 0) {
            financialStatus = "WARNING";
        } else if (budgetUtilization.compareTo(BigDecimal.valueOf(50)) < 0) {
            financialStatus = "UNDER_UTILIZED";
        }
        additionalMetrics.put("financialStatus", financialStatus);
        
        summary.setAdditionalMetrics(additionalMetrics);
        
        return summary;
     }

     private BigDecimal calculateFinancialHealthScore(BigDecimal budgetUtilization, long expenseCount) {
        // Simple financial health scoring algorithm
        BigDecimal score = BigDecimal.valueOf(100);
        
        // Penalize over-budget scenarios
        if (budgetUtilization.compareTo(BigDecimal.valueOf(100)) > 0) {
            BigDecimal overBudgetPenalty = budgetUtilization.subtract(BigDecimal.valueOf(100));
            score = score.subtract(overBudgetPenalty.multiply(BigDecimal.valueOf(0.5)));
        }
        
        // Reward good budget utilization (70-90%)
        if (budgetUtilization.compareTo(BigDecimal.valueOf(70)) >= 0 && 
            budgetUtilization.compareTo(BigDecimal.valueOf(90)) <= 0) {
            score = score.add(BigDecimal.valueOf(10));
        }
        
        // Penalize very low utilization (under-spending might indicate poor planning)
        if (budgetUtilization.compareTo(BigDecimal.valueOf(30)) < 0) {
            score = score.subtract(BigDecimal.valueOf(15));
        }
        
        // Factor in expense frequency (moderate frequency is good)
        if (expenseCount >= 5 && expenseCount <= 50) {
            score = score.add(BigDecimal.valueOf(5));
        } else if (expenseCount > 100) {
            score = score.subtract(BigDecimal.valueOf(10)); // Too many small expenses
        }
        
        // Ensure score is between 0 and 100
        if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) score = BigDecimal.valueOf(100);
        
        return score.setScale(1, RoundingMode.HALF_UP);
     }

     private List<ReportResponse.ReportChart> generateFinancialCharts(ReportResponse.ReportData data) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();
        
        // Budget vs Actual comparison chart
        if (data.getVarianceAnalysis() != null && !data.getVarianceAnalysis().isEmpty()) {
            List<ReportResponse.ChartDataPoint> budgetActualPoints = new ArrayList<>();
            
            for (ReportResponse.VarianceAnalysis variance : data.getVarianceAnalysis()) {
                // Add budget amounts
                budgetActualPoints.add(new ReportResponse.ChartDataPoint(
                        variance.getCategory() + " (Budget)", variance.getBudgetedAmount(), "#2196F3"));
                // Add actual amounts
                budgetActualPoints.add(new ReportResponse.ChartDataPoint(
                        variance.getCategory() + " (Actual)", variance.getActualAmount(), "#FF9800"));
            }
            
            charts.add(new ReportResponse.ReportChart("BAR", "Budget vs Actual by Category", budgetActualPoints));
        }
        
        // Monthly spending trend
        if (data.getMonthlySummaries() != null && !data.getMonthlySummaries().isEmpty()) {
            List<ReportResponse.ChartDataPoint> monthlyTrendPoints = data.getMonthlySummaries().stream()
                    .map(month -> new ReportResponse.ChartDataPoint(
                            month.getMonth() + " " + month.getYear(), month.getTotalAmount()))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("LINE", "Monthly Spending Trend", monthlyTrendPoints));
        }
        
        // Category distribution pie chart
        if (data.getCategorySummaries() != null && !data.getCategorySummaries().isEmpty()) {
            List<ReportResponse.ChartDataPoint> categoryPoints = data.getCategorySummaries().stream()
                    .map(cat -> new ReportResponse.ChartDataPoint(cat.getCategoryName(), cat.getTotalAmount()))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("PIE", "Expense Distribution by Category", categoryPoints));
        }
        
        // Financial metrics dashboard
        if (data.getFinancialMetrics() != null && !data.getFinancialMetrics().isEmpty()) {
            List<ReportResponse.ChartDataPoint> metricsPoints = data.getFinancialMetrics().stream()
                    .filter(metric -> "PERCENTAGE".equals(metric.getMetricType()) || "RATIO".equals(metric.getMetricType()))
                    .map(metric -> new ReportResponse.ChartDataPoint(metric.getMetricName(), metric.getValue()))
                    .collect(Collectors.toList());
            
            if (!metricsPoints.isEmpty()) {
                charts.add(new ReportResponse.ReportChart("BAR", "Key Financial Metrics", metricsPoints));
            }
        }
        
        return charts;
     }

    // Helper methods with null safety
    private List<Budget> getBudgetsForReport(Long userId, ReportRequest.BudgetReportRequest request) {
        try {
            List<Budget> budgets = budgetRepository.findByUserIdAndPeriodOverlap(
                    userId, request.getStartDate(), request.getEndDate());
            
            if (budgets == null) {
                budgets = new ArrayList<>();
            }
            
            // Apply filters
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                budgets = budgets.stream()
                        .filter(b -> b.getCategory() != null && request.getCategoryIds().contains(b.getCategory().getId()))
                        .collect(Collectors.toList());
            }
            
            if (request.getTeamIds() != null && !request.getTeamIds().isEmpty()) {
                budgets = budgets.stream()
                        .filter(b -> b.getTeam() != null && request.getTeamIds().contains(b.getTeam().getId()))
                        .collect(Collectors.toList());
            }
            
            return budgets;
        } catch (Exception e) {
            logger.error("Error fetching budgets for report", e);
            return new ArrayList<>();
        }
    }

    private List<ApprovalWorkflow> getApprovalWorkflowsForReport(Long userId, ReportRequest.ApprovalReportRequest request) {
        try {
            LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
            LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
            
            List<ApprovalWorkflow> workflows = approvalWorkflowRepository.findBySubmittedAtBetween(startDateTime, endDateTime);
            
            if (workflows == null) {
                workflows = new ArrayList<>();
            }
            
            // Filter by user involvement
            workflows = workflows.stream()
                    .filter(w -> userId.equals(w.getSubmittedBy()) || 
                               userId.equals(w.getCurrentApproverId()) || 
                               userId.equals(w.getFinalApproverId()))
                    .collect(Collectors.toList());
            
            return workflows;
        } catch (Exception e) {
            logger.error("Error fetching approval workflows for report", e);
            return new ArrayList<>();
        }
    }

    private List<Expense> getTeamExpensesForReport(Long teamId, ReportRequest.TeamReportRequest request) {
        try {
            List<Expense> expenses = expenseRepository.findByTeamId(teamId);
            
            if (expenses == null) {
                expenses = new ArrayList<>();
            }
            
            return expenses.stream()
                    .filter(e -> !e.getExpenseDate().isBefore(request.getStartDate()) &&
                               !e.getExpenseDate().isAfter(request.getEndDate()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching team expenses for report", e);
            return new ArrayList<>();
        }
    }

    private List<Budget> getTeamBudgetsForReport(Long teamId, ReportRequest.TeamReportRequest request) {
        try {
            List<Budget> budgets = budgetRepository.findByTeamIdAndIsActiveTrue(teamId);
            
            if (budgets == null) {
                budgets = new ArrayList<>();
            }
            
            return budgets.stream()
                    .filter(b -> !b.getStartDate().isAfter(request.getEndDate()) &&
                               !b.getEndDate().isBefore(request.getStartDate()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching team budgets for report", e);
            return new ArrayList<>();
        }
    }

    // Calculation methods with null safety
    private ReportResponse.ReportSummary calculateBudgetSummary(List<Budget> budgets) {
        if (budgets == null || budgets.isEmpty()) {
            return new ReportResponse.ReportSummary(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }

        BigDecimal totalBudgeted = budgets.stream()
                .filter(b -> b.getTotalAmount() != null)
                .map(Budget::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = budgets.stream()
                .filter(b -> b.getSpentAmount() != null)
                .map(Budget::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = budgets.size();

        BigDecimal averageBudget = totalCount > 0 ? 
                totalBudgeted.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;

        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalBudgeted, totalCount, averageBudget);
        
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("totalSpent", totalSpent);
        additionalMetrics.put("totalRemaining", totalBudgeted.subtract(totalSpent));
        additionalMetrics.put("utilizationPercentage", 
                totalBudgeted.compareTo(BigDecimal.ZERO) > 0 ? 
                        totalSpent.divide(totalBudgeted, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
                        BigDecimal.ZERO);
        summary.setAdditionalMetrics(additionalMetrics);

        return summary;
    }

    private ReportResponse.ReportSummary calculateApprovalSummary(List<ApprovalWorkflow> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            return new ReportResponse.ReportSummary(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }

        BigDecimal totalAmount = workflows.stream()
                .filter(w -> w.getExpenseAmount() != null)
                .map(ApprovalWorkflow::getExpenseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = workflows.size();

        BigDecimal averageAmount = totalCount > 0 ? 
                totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;

        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalAmount, totalCount, averageAmount);
        
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("approvedCount", workflows.stream().filter(w -> "APPROVED".equals(w.getStatus().name())).count());
        additionalMetrics.put("rejectedCount", workflows.stream().filter(w -> "REJECTED".equals(w.getStatus().name())).count());
        additionalMetrics.put("pendingCount", workflows.stream().filter(w -> "PENDING".equals(w.getStatus().name())).count());
        summary.setAdditionalMetrics(additionalMetrics);

        return summary;
    }

    private ReportResponse.ReportSummary calculateTeamSummary(List<Expense> expenses, List<Budget> budgets) {
        ReportResponse.ReportSummary expenseSummary = calculateExpenseSummary(expenses);
        ReportResponse.ReportSummary budgetSummary = calculateBudgetSummary(budgets);
        
        Map<String, Object> teamMetrics = new HashMap<>();
        teamMetrics.put("budgetCount", budgets != null ? budgets.size() : 0);
        teamMetrics.put("totalBudgeted", budgetSummary.getTotalAmount());
        if (budgetSummary.getAdditionalMetrics() != null) {
            teamMetrics.put("budgetUtilization", budgetSummary.getAdditionalMetrics().get("utilizationPercentage"));
        }
        
        expenseSummary.setAdditionalMetrics(teamMetrics);
        return expenseSummary;
    }

    private List<ReportResponse.BudgetItem> convertToBudgetItems(List<Budget> budgets) {
        if (budgets == null) {
            return new ArrayList<>();
        }
        
        return budgets.stream()
                .map(budget -> new ReportResponse.BudgetItem(
                        budget.getId(),
                        budget.getName() != null ? budget.getName() : "Unknown",
                        budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO,
                        budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO,
                        budget.getCategory() != null ? budget.getCategory().getName() : "Unknown",
                        Boolean.TRUE.equals(budget.getIsActive()) ? "ACTIVE" : "INACTIVE"
                ))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.ApprovalItem> convertToApprovalItems(List<ApprovalWorkflow> workflows) {
        if (workflows == null) {
            return new ArrayList<>();
        }
        
        return workflows.stream()
                .map(workflow -> new ReportResponse.ApprovalItem(
                        workflow.getId(),
                        workflow.getExpenseId(),
                        workflow.getExpense() != null ? workflow.getExpense().getTitle() : "Unknown",
                        workflow.getExpenseAmount() != null ? workflow.getExpenseAmount() : BigDecimal.ZERO,
                        workflow.getSubmittedByUser() != null ? workflow.getSubmittedByUser().getUsername() : "Unknown",
                        workflow.getStatus() != null ? workflow.getStatus().name() : "UNKNOWN"
                ))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.UserSummary> calculateTeamMemberSummaries(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, List<Expense>> userGroups = expenses.stream()
                .filter(e -> e.getUser() != null)
                .collect(Collectors.groupingBy(e -> e.getUser().getUsername()));

        return userGroups.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    List<Expense> userExpenses = entry.getValue();
                    
                    BigDecimal totalAmount = userExpenses.stream()
                            .filter(e -> e.getAmount() != null)
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    String fullName = userExpenses.get(0).getUser() != null ? 
                            userExpenses.get(0).getUser().getFirstName() + " " + userExpenses.get(0).getUser().getLastName() : 
                            "Unknown";
                    
                    return new ReportResponse.UserSummary(username, fullName, totalAmount, (long) userExpenses.size());
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    // Implement remaining interface methods
    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(String reportId, String username) {
        if (!validateReportAccess(reportId, username)) {
            throw new ForbiddenException("Access denied to report: " + reportId);
        }

        ReportResponse report = reportStorage.get(reportId);
        if (report == null) {
            throw new ResourceNotFoundException("Report not found: " + reportId);
        }

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getUserReports(String username) {
        return reportStorage.values().stream()
                .filter(report -> username.equals(report.getGeneratedBy()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getUserReports(String username, Pageable pageable) {
        List<ReportResponse> userReports = getUserReports(username);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userReports.size());
        
        List<ReportResponse> pageContent = start < end ? userReports.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, userReports.size());
    }

    @Override
    public void deleteReport(String reportId, String username) {
        if (!validateReportAccess(reportId, username)) {
            throw new ForbiddenException("Access denied to report: " + reportId);
        }

        reportStorage.remove(reportId);
        reportFiles.remove(reportId);

        logger.info("Deleted report: {} by user: {}", reportId, username);
    }

    @Override
    public byte[] downloadReport(String reportId, String username) {
        if (!validateReportAccess(reportId, username)) {
            throw new ForbiddenException("Access denied to report: " + reportId);
        }

        byte[] reportFile = reportFiles.get(reportId);
        if (reportFile == null) {
            throw new ResourceNotFoundException("Report file not found: " + reportId);
        }

        return reportFile;
    }

    @Override
    public String getReportDownloadUrl(String reportId, String username) {
        if (!validateReportAccess(reportId, username)) {
            throw new ForbiddenException("Access denied to report: " + reportId);
        }

        return "/api/reports/" + reportId + "/download";
    }

    @Override
    public ReportResponse.ScheduledReportInfo createScheduledReport(ReportRequest.ScheduledReportRequest request, String username) {
        try {
            logger.info("Creating scheduled report: {} for user: {}", request.getReportName(), username);
            
            // Get user to validate existence
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            Long scheduleId = System.currentTimeMillis();
            LocalDateTime nextScheduled = calculateNextScheduledTime(request.getFrequency());
            
            ReportResponse.ScheduledReportInfo scheduleInfo = new ReportResponse.ScheduledReportInfo(
                    scheduleId, request.getReportName(), request.getFrequency(),
                    request.getIsActive(), nextScheduled);
            
            scheduleInfo.setEmailRecipients(request.getEmailRecipients());
            scheduleInfo.setDescription(request.getDescription());
            scheduleInfo.setStatus(Boolean.TRUE.equals(request.getIsActive()) ? "ACTIVE" : "INACTIVE");
            scheduleInfo.setExecutionCount(0);
            
            // Store the scheduled report for the user
            List<ReportResponse.ScheduledReportInfo> userReports = userScheduledReports.getOrDefault(username, new ArrayList<>());
            userReports.add(scheduleInfo);
            userScheduledReports.put(username, userReports);
            
            // Also store the owner mapping for validation
            scheduleOwnerMap.put(scheduleId, username);
            
            logger.info("Successfully created scheduled report: {} with ID: {} for user: {}", 
                    request.getReportName(), scheduleId, username);
            
            return scheduleInfo;
            
        } catch (ResourceNotFoundException e) {
            logger.error("Error creating scheduled report: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error creating scheduled report for user: {}", username, e);
            throw new RuntimeException("Failed to create scheduled report: " + e.getMessage(), e);
        }
    }
    
 // Helper method to get the owner of a scheduled report
    private String getScheduledReportOwner(Long scheduleId) {
        return scheduleOwnerMap.get(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse.ScheduledReportInfo> getScheduledReports(String username) {
        try {
            logger.info("Retrieving scheduled reports for user: {}", username);
            
            // Get user to validate existence
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            // Return scheduled reports for this user
            List<ReportResponse.ScheduledReportInfo> userReports = userScheduledReports.getOrDefault(username, new ArrayList<>());
            
            logger.info("Found {} scheduled reports for user: {}", userReports.size(), username);
            return userReports;
            
        } catch (Exception e) {
            logger.error("Error retrieving scheduled reports for user: {}", username, e);
            return new ArrayList<>();
        }
    }

    @Override
    public ReportResponse.ScheduledReportInfo updateScheduledReport(Long scheduleId, ReportRequest.ScheduledReportRequest request, String username) {
        try {
            logger.info("Updating scheduled report: {} for user: {}", scheduleId, username);
            
            // Get user to validate existence
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            // Get user's scheduled reports
            List<ReportResponse.ScheduledReportInfo> userReports = userScheduledReports.getOrDefault(username, new ArrayList<>());
            
            // Find the scheduled report to update
            ReportResponse.ScheduledReportInfo existingReport = userReports.stream()
                    .filter(report -> scheduleId.equals(report.getScheduleId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found: " + scheduleId));
            
            // Validate that the user owns this scheduled report
            if (!username.equals(getScheduledReportOwner(scheduleId))) {
                throw new ForbiddenException("Access denied to scheduled report: " + scheduleId);
            }
            
            // Update the scheduled report
            existingReport.setReportName(request.getReportName());
            existingReport.setFrequency(request.getFrequency());
            existingReport.setEmailRecipients(request.getEmailRecipients());
            existingReport.setIsActive(request.getIsActive());
            existingReport.setDescription(request.getDescription());
            
            // Calculate next scheduled time based on new frequency
            LocalDateTime nextScheduled = calculateNextScheduledTime(request.getFrequency());
            existingReport.setNextScheduled(nextScheduled);
            
            // Update status and execution count
            existingReport.setStatus(Boolean.TRUE.equals(request.getIsActive()) ? "ACTIVE" : "INACTIVE");
            
            // Save the updated report back to storage
            userScheduledReports.put(username, userReports);
            
            logger.info("Successfully updated scheduled report: {} for user: {}", scheduleId, username);
            return existingReport;
            
        } catch (ResourceNotFoundException | ForbiddenException e) {
            logger.error("Error updating scheduled report: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating scheduled report: {} for user: {}", scheduleId, username, e);
            throw new RuntimeException("Failed to update scheduled report: " + e.getMessage(), e);
        }
    }
    
    

    @Override
    public void deleteScheduledReport(Long scheduleId, String username) {
        try {
            logger.info("Deleting scheduled report: {} by user: {}", scheduleId, username);
            
            // Get user to validate existence
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            // Get user's scheduled reports
            List<ReportResponse.ScheduledReportInfo> userReports = userScheduledReports.getOrDefault(username, new ArrayList<>());
            
            // Find the scheduled report to delete
            ReportResponse.ScheduledReportInfo reportToDelete = userReports.stream()
                    .filter(report -> scheduleId.equals(report.getScheduleId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found: " + scheduleId));
            
            // Validate that the user owns this scheduled report
            if (!username.equals(getScheduledReportOwner(scheduleId))) {
                throw new ForbiddenException("Access denied to scheduled report: " + scheduleId);
            }
            
            // Remove the scheduled report from user's list
            userReports.removeIf(report -> scheduleId.equals(report.getScheduleId()));
            
            // Update the storage
            if (userReports.isEmpty()) {
                userScheduledReports.remove(username);
            } else {
                userScheduledReports.put(username, userReports);
            }
            
            logger.info("Successfully deleted scheduled report: {} by user: {}", scheduleId, username);
            
        } catch (ResourceNotFoundException | ForbiddenException e) {
            logger.error("Error deleting scheduled report: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting scheduled report: {} by user: {}", scheduleId, username, e);
            throw new RuntimeException("Failed to delete scheduled report: " + e.getMessage(), e);
        }
    }

    @Override
    public void executeScheduledReports() {
        try {
            logger.info("Executing scheduled reports");
            
            LocalDateTime now = LocalDateTime.now();
            int executedCount = 0;
            
            // Iterate through all users' scheduled reports
            for (Map.Entry<String, List<ReportResponse.ScheduledReportInfo>> entry : userScheduledReports.entrySet()) {
                String username = entry.getKey();
                List<ReportResponse.ScheduledReportInfo> userReports = entry.getValue();
                
                for (ReportResponse.ScheduledReportInfo scheduledReport : userReports) {
                    // Check if report is active and due for execution
                    if (Boolean.TRUE.equals(scheduledReport.getIsActive()) && 
                        scheduledReport.getNextScheduled() != null &&
                        scheduledReport.getNextScheduled().isBefore(now)) {
                        
                        try {
                            // Execute the scheduled report
                            executeIndividualScheduledReport(scheduledReport, username);
                            
                            // Update next scheduled time
                            LocalDateTime nextScheduled = calculateNextScheduledTime(scheduledReport.getFrequency());
                            scheduledReport.setNextScheduled(nextScheduled);
                            scheduledReport.setLastGenerated(now);
                            scheduledReport.setExecutionCount(
                                (scheduledReport.getExecutionCount() != null ? scheduledReport.getExecutionCount() : 0) + 1
                            );
                            
                            executedCount++;
                            logger.info("Successfully executed scheduled report: {} for user: {}", 
                                    scheduledReport.getReportName(), username);
                            
                        } catch (Exception e) {
                            logger.error("Error executing scheduled report: {} for user: {}", 
                                    scheduledReport.getReportName(), username, e);
                            scheduledReport.setStatus("ERROR");
                        }
                    }
                }
            }
            
            logger.info("Completed executing scheduled reports. Total executed: {}", executedCount);
            
        } catch (Exception e) {
            logger.error("Error during scheduled reports execution", e);
        }
    }

    // Helper method to execute an individual scheduled report
    private void executeIndividualScheduledReport(ReportResponse.ScheduledReportInfo scheduledReport, String username) {
        try {
            logger.info("Executing individual scheduled report: {} for user: {}", 
                    scheduledReport.getReportName(), username);
            
            // For now, create a simple expense report as default
            // In a real implementation, you would use the stored report configuration
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(1); // Default to last month
            
            ReportRequest.ExpenseReportRequest request = new ReportRequest.ExpenseReportRequest(
                    startDate, endDate, "PDF");
            
            // Generate the report
            ReportResponse report = generateExpenseReport(request, username);
            
            // Email the report to recipients if email service is available
            if (emailService != null && scheduledReport.getEmailRecipients() != null && 
                !scheduledReport.getEmailRecipients().isEmpty()) {
                
                byte[] reportData = downloadReport(report.getReportId(), username);
                
                for (String recipient : scheduledReport.getEmailRecipients()) {
                    try {
                        emailService.sendReportEmail(recipient, report, reportData);
                        logger.info("Emailed scheduled report to: {}", recipient);
                    } catch (Exception e) {
                        logger.error("Error emailing scheduled report to: {}", recipient, e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error executing individual scheduled report", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportRequest> getReportTemplates() {
        List<ReportRequest> templates = new ArrayList<>();
        
        ReportRequest monthlyExpense = new ReportRequest("EXPENSE", 
                LocalDate.now().withDayOfMonth(1), LocalDate.now(), "PDF");
        monthlyExpense.setReportName("Monthly Expense Report");
        monthlyExpense.setDescription("Comprehensive monthly expense analysis");
        monthlyExpense.setGroupBy("CATEGORY");
        templates.add(monthlyExpense);
        
        ReportRequest budgetActual = new ReportRequest("BUDGET", 
                LocalDate.now().withDayOfMonth(1), LocalDate.now(), "PDF");
        budgetActual.setReportName("Budget vs Actual Report");
        budgetActual.setDescription("Budget performance analysis");
        budgetActual.setGroupBy("CATEGORY");
        templates.add(budgetActual);
        
        return templates;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportRequest getReportTemplate(String templateName) {
        return getReportTemplates().stream()
                .filter(template -> templateName.equals(template.getReportName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateName));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse.ReportSummary getReportSummary(ReportRequest request, String username) {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if ("EXPENSE".equals(request.getReportType())) {
            ReportRequest.ExpenseReportRequest expenseReq = new ReportRequest.ExpenseReportRequest(
                    request.getStartDate(), request.getEndDate(), "JSON");
            List<Expense> expenses = getExpensesForReport(user.getId(), expenseReq);
            return calculateExpenseSummary(expenses);
        }

        throw new UnsupportedOperationException("Report summary not supported for type: " + request.getReportType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse.ReportChart> getReportCharts(ReportRequest request, String username) {
        User user = userRepository.findByUsernameOrEmail(username)
        		.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if ("EXPENSE".equals(request.getReportType())) {
            ReportRequest.ExpenseReportRequest expenseReq = new ReportRequest.ExpenseReportRequest(
                    request.getStartDate(), request.getEndDate(), "JSON");
            List<Expense> expenses = getExpensesForReport(user.getId(), expenseReq);
            return generateExpenseCharts(expenses, request.getGroupBy());
        }

        return new ArrayList<>();
    }

    @Override
    public void emailReport(String reportId, List<String> recipients, String username) {
        try {
            logger.info("Attempting to email report: {} to {} recipients by user: {}", reportId, recipients.size(), username);
            
            ReportResponse report = getReportById(reportId, username);
            byte[] reportFile = downloadReport(reportId, username);

            if (emailService != null) {
                for (String recipient : recipients) {
                    try {
                        emailService.sendReportEmail(recipient, report, reportFile);
                        logger.info("Successfully emailed report to: {}", recipient);
                    } catch (Exception e) {
                        logger.error("Failed to email report to: {}", recipient, e);
                        // Continue with other recipients even if one fails
                    }
                }
                logger.info("Completed emailing report: {} to {} recipients by user: {}", reportId, recipients.size(), username);
            } else {
                logger.warn("Email service not available - simulating email send for report: {}", reportId);
                // Simulate successful email sending when email service is not available
                for (String recipient : recipients) {
                    simulateEmailSending(recipient, report, reportFile);
                }
            }

        } catch (Exception e) {
            logger.error("Error emailing report: {} by user: {}", reportId, username, e);
            throw new RuntimeException("Failed to email report: " + e.getMessage(), e);
        }
    }

    // Add this helper method for email simulation
    private void simulateEmailSending(String recipient, ReportResponse report, byte[] reportFile) {
        logger.info("SIMULATED EMAIL SEND:");
        logger.info("To: {}", recipient);
        logger.info("Subject: Report - {}", report.getReportName());
        logger.info("Report Type: {}", report.getReportType());
        logger.info("Report Format: {}", report.getFormat());
        logger.info("File Size: {} bytes", reportFile.length);
        logger.info("Generated At: {}", report.getGeneratedAt());
    }

    @Override
    public void shareReport(String reportId, List<String> usernames, String sharedBy) {
        try {
            logger.info("Sharing report: {} with {} users by: {}", reportId, usernames.size(), sharedBy);
            
            // Validate that the report exists and the user has access
            ReportResponse report = getReportById(reportId, sharedBy);
            
            // Validate that all target users exist
            List<String> validUsernames = new ArrayList<>();
            for (String username : usernames) {
                try {
                    User user = userRepository.findByUsernameOrEmail(username)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
                    validUsernames.add(user.getUsername());
                    logger.info("Validated user for sharing: {}", username);
                } catch (ResourceNotFoundException e) {
                    logger.warn("Skipping invalid user for sharing: {}", username);
                    // Continue with other users, just skip invalid ones
                }
            }
            
            if (validUsernames.isEmpty()) {
                throw new IllegalArgumentException("No valid users found to share the report with");
            }
            
            // Store shared report information
            List<String> currentShares = sharedReports.getOrDefault(reportId, new ArrayList<>());
            
            // Add new users to share list (avoid duplicates)
            for (String username : validUsernames) {
                if (!currentShares.contains(username) && !username.equals(sharedBy)) {
                    currentShares.add(username);
                    logger.info("Added user to shared report list: {}", username);
                }
            }
            
            sharedReports.put(reportId, currentShares);
            
            // Log sharing activity
            logger.info("Report {} shared with users: {} by: {}", reportId, validUsernames, sharedBy);
            
            // If email service is available, notify users about the shared report
            if (emailService != null) {
                notifyUsersAboutSharedReport(report, validUsernames, sharedBy);
            } else {
                // Simulate notification
                for (String username : validUsernames) {
                    simulateShareNotification(report, username, sharedBy);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error sharing report: {} by user: {}", reportId, sharedBy, e);
            throw new RuntimeException("Failed to share report: " + e.getMessage(), e);
        }
    }

    // Helper method to notify users about shared reports
    private void notifyUsersAboutSharedReport(ReportResponse report, List<String> usernames, String sharedBy) {
        for (String username : usernames) {
            try {
                // Get user email
                User user = userRepository.findByUsernameOrEmail(username).orElse(null);
                if (user != null && user.getEmail() != null) {
                    // Send notification email (simplified)
                    String subject = "Report Shared with You - " + report.getReportName();
                    String message = String.format(
                        "Hello %s,\n\n%s has shared a report with you.\n\nReport: %s\nType: %s\nGenerated: %s\n\nYou can access this report in your dashboard.",
                        user.getFirstName() != null ? user.getFirstName() : username,
                        sharedBy,
                        report.getReportName(),
                        report.getReportType(),
                        report.getGeneratedAt()
                    );
                    
                    // Note: This would require an email notification method in EmailService
                    logger.info("Would send share notification email to: {}", user.getEmail());
                }
            } catch (Exception e) {
                logger.error("Error notifying user about shared report: {}", username, e);
            }
        }
    }

    // Helper method to simulate share notification
    private void simulateShareNotification(ReportResponse report, String username, String sharedBy) {
        logger.info("SIMULATED SHARE NOTIFICATION:");
        logger.info("To User: {}", username);
        logger.info("Shared By: {}", sharedBy);
        logger.info("Report: {}", report.getReportName());
        logger.info("Report Type: {}", report.getReportType());
        logger.info("Generated At: {}", report.getGeneratedAt());
    }

    // Add method to get users a report is shared with
    public List<String> getReportSharedUsers(String reportId) {
        return sharedReports.getOrDefault(reportId, new ArrayList<>());
    }

    // Add method to get reports shared with a user
    public List<ReportResponse> getSharedReports(String username) {
        List<ReportResponse> sharedReportsList = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : sharedReports.entrySet()) {
            String reportId = entry.getKey();
            List<String> sharedUsers = entry.getValue();
            
            if (sharedUsers.contains(username)) {
                try {
                    ReportResponse report = reportStorage.get(reportId);
                    if (report != null) {
                        sharedReportsList.add(report);
                    }
                } catch (Exception e) {
                    logger.error("Error getting shared report: {}", reportId, e);
                }
            }
        }
        
        return sharedReportsList;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse.ReportData aggregateExpenseData(LocalDate startDate, LocalDate endDate, String username) {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        ReportRequest.ExpenseReportRequest request = new ReportRequest.ExpenseReportRequest(startDate, endDate, "JSON");
        List<Expense> expenses = getExpensesForReport(user.getId(), request);

        ReportResponse.ReportData data = new ReportResponse.ReportData();
        data.setExpenses(convertToExpenseItems(expenses));
        data.setCategorySummaries(calculateCategorySummaries(expenses));
        
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse.ReportData aggregateBudgetData(LocalDate startDate, LocalDate endDate, String username) {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        ReportRequest.BudgetReportRequest request = new ReportRequest.BudgetReportRequest(startDate, endDate, "JSON");
        List<Budget> budgets = getBudgetsForReport(user.getId(), request);

        ReportResponse.ReportData data = new ReportResponse.ReportData();
        data.setBudgets(convertToBudgetItems(budgets));
        
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse.ReportData aggregateApprovalData(LocalDate startDate, LocalDate endDate, String username) {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        ReportRequest.ApprovalReportRequest request = new ReportRequest.ApprovalReportRequest(startDate, endDate, "JSON");
        List<ApprovalWorkflow> workflows = getApprovalWorkflowsForReport(user.getId(), request);

        ReportResponse.ReportData data = new ReportResponse.ReportData();
        data.setApprovals(convertToApprovalItems(workflows));
        
        return data;
    }

    @Override
    public boolean validateReportAccess(String reportId, String username) {
        ReportResponse report = reportStorage.get(reportId);
        if (report == null) {
            return false;
        }
        
        // Check if user is the owner
        if (username.equals(report.getGeneratedBy())) {
            return true;
        }
        
        // Check if report is shared with the user
        List<String> sharedUsers = sharedReports.getOrDefault(reportId, new ArrayList<>());
        return sharedUsers.contains(username);
    }

    @Override
    public void cleanupExpiredReports() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Clean up expired reports
            List<String> expiredReportIds = reportStorage.entrySet().stream()
                    .filter(entry -> entry.getValue().getExpiresAt().isBefore(now))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (String reportId : expiredReportIds) {
                reportStorage.remove(reportId);
                reportFiles.remove(reportId);
            }

            // Clean up inactive scheduled reports older than 90 days
            LocalDateTime cutoffDate = now.minusDays(90);
            int removedScheduledReports = 0;
            
            for (Map.Entry<String, List<ReportResponse.ScheduledReportInfo>> entry : userScheduledReports.entrySet()) {
                String username = entry.getKey();
                List<ReportResponse.ScheduledReportInfo> userReports = entry.getValue();
                
                List<ReportResponse.ScheduledReportInfo> activeReports = userReports.stream()
                        .filter(report -> Boolean.TRUE.equals(report.getIsActive()) || 
                                (report.getLastGenerated() != null && report.getLastGenerated().isAfter(cutoffDate)))
                        .collect(Collectors.toList());
                
                if (activeReports.size() != userReports.size()) {
                    removedScheduledReports += (userReports.size() - activeReports.size());
                    
                    if (activeReports.isEmpty()) {
                        userScheduledReports.remove(username);
                    } else {
                        userScheduledReports.put(username, activeReports);
                    }
                }
            }

            logger.info("Cleaned up {} expired reports and {} old scheduled reports", 
                    expiredReportIds.size(), removedScheduledReports);
            
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    @Override
    public List<String> getSupportedFormats() {
        return Arrays.asList("PDF", "CSV", "XLSX", "JSON");
    }

    @Override
    public Long getReportFileSize(String reportId) {
        byte[] reportFile = reportFiles.get(reportId);
        return reportFile != null ? (long) reportFile.length : 0L;
    }

    // Utility methods
    private String generateReportId() {
        return "RPT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private LocalDateTime calculateNextScheduledTime(String frequency) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (frequency.toUpperCase()) {
            case "DAILY":
                return now.plusDays(1);
            case "WEEKLY":
                return now.plusWeeks(1);
            case "MONTHLY":
                return now.plusMonths(1);
            case "QUARTERLY":
                return now.plusMonths(3);
            default:
                return now.plusDays(1);
        }
    }
    
 // Additional utility methods for scheduled reports management

    /**
     * Get scheduled report by ID for a specific user
     */
    public ReportResponse.ScheduledReportInfo getScheduledReportById(Long scheduleId, String username) {
        List<ReportResponse.ScheduledReportInfo> userReports = userScheduledReports.getOrDefault(username, new ArrayList<>());
        
        return userReports.stream()
                .filter(report -> scheduleId.equals(report.getScheduleId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found: " + scheduleId));
    }

    /**
     * Toggle scheduled report active status
     */
    public ReportResponse.ScheduledReportInfo toggleScheduledReportStatus(Long scheduleId, String username) {
        try {
            logger.info("Toggling status for scheduled report: {} by user: {}", scheduleId, username);
            
            ReportResponse.ScheduledReportInfo scheduledReport = getScheduledReportById(scheduleId, username);
            
            // Toggle the active status
            boolean newStatus = !Boolean.TRUE.equals(scheduledReport.getIsActive());
            scheduledReport.setIsActive(newStatus);
            scheduledReport.setStatus(newStatus ? "ACTIVE" : "INACTIVE");
            
            if (newStatus) {
                // If activating, calculate next scheduled time
                LocalDateTime nextScheduled = calculateNextScheduledTime(scheduledReport.getFrequency());
                scheduledReport.setNextScheduled(nextScheduled);
            }
            
            logger.info("Scheduled report: {} status changed to: {} for user: {}", 
                    scheduleId, newStatus ? "ACTIVE" : "INACTIVE", username);
            
            return scheduledReport;
            
        } catch (Exception e) {
            logger.error("Error toggling scheduled report status", e);
            throw new RuntimeException("Failed to toggle scheduled report status: " + e.getMessage(), e);
        }
    }

    /**
     * Get scheduled reports count for a user
     */
    public Map<String, Object> getScheduledReportsStats(String username) {
        try {
            List<ReportResponse.ScheduledReportInfo> userReports = userScheduledReports.getOrDefault(username, new ArrayList<>());
            
            long activeCount = userReports.stream()
                    .filter(report -> Boolean.TRUE.equals(report.getIsActive()))
                    .count();
            
            long inactiveCount = userReports.size() - activeCount;
            
            // Calculate total executions
            int totalExecutions = userReports.stream()
                    .mapToInt(report -> report.getExecutionCount() != null ? report.getExecutionCount() : 0)
                    .sum();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalScheduledReports", userReports.size());
            stats.put("activeReports", activeCount);
            stats.put("inactiveReports", inactiveCount);
            stats.put("totalExecutions", totalExecutions);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting scheduled reports stats for user: {}", username, e);
            return new HashMap<>();
        }
    }
 }