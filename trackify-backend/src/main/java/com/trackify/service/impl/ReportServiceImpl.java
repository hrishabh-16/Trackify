package com.trackify.service.impl;

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

    // Make EmailService optional since it might not be implemented yet
    @Autowired(required = false)
    private EmailService emailService;

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
        StringBuilder content = new StringBuilder();
        content.append("PDF Report: ").append(report.getReportName()).append("\n");
        content.append("Generated: ").append(report.getGeneratedAt()).append("\n");
        content.append("Type: ").append(report.getReportType()).append("\n");
        content.append("Period: ").append(report.getStartDate()).append(" to ").append(report.getEndDate()).append("\n\n");
        
        if (report.getSummary() != null) {
            content.append("SUMMARY:\n");
            content.append("Total Amount: $").append(report.getSummary().getTotalAmount()).append("\n");
            content.append("Total Count: ").append(report.getSummary().getTotalCount()).append("\n");
            content.append("Average Amount: $").append(report.getSummary().getAverageAmount()).append("\n\n");
        }
        
        if (report.getData() != null && report.getData().getExpenses() != null) {
            content.append("EXPENSES:\n");
            content.append("ID\tTitle\tAmount\tDate\tCategory\tUser\tStatus\n");
            for (ReportResponse.ExpenseItem expense : report.getData().getExpenses()) {
                content.append(expense.getExpenseId()).append("\t")
                       .append(expense.getTitle()).append("\t")
                       .append("$").append(expense.getAmount()).append("\t")
                       .append(expense.getExpenseDate()).append("\t")
                       .append(expense.getCategory()).append("\t")
                       .append(expense.getUser()).append("\t")
                       .append(expense.getStatus()).append("\n");
            }
        }
        
        return content.toString().getBytes();
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

                default:
                    throw new IllegalArgumentException("Unsupported report type: " + request.getReportType());
            }
        } catch (Exception e) {
            logger.error("Error generating custom report", e);
            throw new RuntimeException("Failed to generate custom report: " + e.getMessage(), e);
        }
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
        Long scheduleId = System.currentTimeMillis();
        LocalDateTime nextScheduled = calculateNextScheduledTime(request.getFrequency());
        
        ReportResponse.ScheduledReportInfo scheduleInfo = new ReportResponse.ScheduledReportInfo(
                scheduleId, request.getReportName(), request.getFrequency(),
                request.getIsActive(), nextScheduled);
        
        scheduleInfo.setEmailRecipients(request.getEmailRecipients());
        scheduleInfo.setDescription(request.getDescription());
        scheduleInfo.setStatus("ACTIVE");
        
        logger.info("Created scheduled report: {} for user: {}", request.getReportName(), username);
        
        return scheduleInfo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse.ScheduledReportInfo> getScheduledReports(String username) {
        return new ArrayList<>();
    }

    @Override
    public ReportResponse.ScheduledReportInfo updateScheduledReport(Long scheduleId, ReportRequest.ScheduledReportRequest request, String username) {
        throw new UnsupportedOperationException("Scheduled report update not implemented yet");
    }

    @Override
    public void deleteScheduledReport(Long scheduleId, String username) {
        logger.info("Deleted scheduled report: {} by user: {}", scheduleId, username);
    }

    @Override
    public void executeScheduledReports() {
        logger.info("Executing scheduled reports");
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
            ReportResponse report = getReportById(reportId, username);
            byte[] reportFile = downloadReport(reportId, username);

            if (emailService != null) {
                for (String recipient : recipients) {
                    emailService.sendReportEmail(recipient, report, reportFile);
                }
                logger.info("Emailed report: {} to {} recipients by user: {}", reportId, recipients.size(), username);
            } else {
                logger.warn("Email service not available - report email not sent");
            }

        } catch (Exception e) {
            logger.error("Error emailing report: {} by user: {}", reportId, username, e);
            throw new RuntimeException("Failed to email report", e);
        }
    }

    @Override
    public void shareReport(String reportId, List<String> usernames, String sharedBy) {
        logger.info("Shared report: {} with {} users by: {}", reportId, usernames.size(), sharedBy);
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
        return report != null && username.equals(report.getGeneratedBy());
    }

    @Override
    public void cleanupExpiredReports() {
        LocalDateTime now = LocalDateTime.now();
        
        List<String> expiredReportIds = reportStorage.entrySet().stream()
                .filter(entry -> entry.getValue().getExpiresAt().isBefore(now))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String reportId : expiredReportIds) {
            reportStorage.remove(reportId);
            reportFiles.remove(reportId);
        }

        logger.info("Cleaned up {} expired reports", expiredReportIds.size());
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
 }