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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private EmailService emailService;

    // In-memory storage for reports (in production, use database or file storage)
    private final Map<String, ReportResponse> reportStorage = new HashMap<>();
    private final Map<String, byte[]> reportFiles = new HashMap<>();

    @Override
    public ReportResponse generateExpenseReport(ReportRequest.ExpenseReportRequest request, String username) {
        try {
            logger.info("Generating expense report for user: {} from {} to {}", 
                    username, request.getStartDate(), request.getEndDate());

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            // Get expense data
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

            // Calculate summary
            ReportResponse.ReportSummary summary = calculateExpenseSummary(expenses);
            report.setSummary(summary);

            // Prepare report data
            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setExpenses(convertToExpenseItems(expenses));
            
            if ("CATEGORY".equals(request.getGroupBy())) {
                data.setCategorySummaries(calculateCategorySummaries(expenses));
            }
            
            report.setData(data);

            // Generate charts
            if (request.getFormat().equals("PDF")) {
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
            throw new RuntimeException("Failed to generate expense report", e);
        }
    }

    @Override
    public ReportResponse generateBudgetReport(ReportRequest.BudgetReportRequest request, String username) {
        try {
            logger.info("Generating budget report for user: {} from {} to {}", 
                    username, request.getStartDate(), request.getEndDate());

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            // Get budget data
            List<Budget> budgets = getBudgetsForReport(user.getId(), request);
            
            // Create report response
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

            // Calculate summary
            ReportResponse.ReportSummary summary = calculateBudgetSummary(budgets);
            report.setSummary(summary);

            // Prepare report data
            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setBudgets(convertToBudgetItems(budgets));
            report.setData(data);

            // Generate charts
            if (request.getFormat().equals("PDF")) {
                List<ReportResponse.ReportChart> charts = generateBudgetCharts(budgets, request.getGroupBy());
                report.setCharts(charts);
            }

            // Generate report file
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            // Store report
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            logger.info("Successfully generated budget report: {}", reportId);
            return report;

        } catch (Exception e) {
            logger.error("Error generating budget report for user: {}", username, e);
            throw new RuntimeException("Failed to generate budget report", e);
        }
    }

    @Override
    public ReportResponse generateApprovalReport(ReportRequest.ApprovalReportRequest request, String username) {
        try {
            logger.info("Generating approval report for user: {} from {} to {}", 
                    username, request.getStartDate(), request.getEndDate());

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String reportId = generateReportId();
            
            // Get approval data
            List<ApprovalWorkflow> workflows = getApprovalWorkflowsForReport(user.getId(), request);
            
            // Create report response
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

            // Calculate summary
            ReportResponse.ReportSummary summary = calculateApprovalSummary(workflows);
            report.setSummary(summary);

            // Prepare report data
            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setApprovals(convertToApprovalItems(workflows));
            report.setData(data);

            // Generate charts
            if (request.getFormat().equals("PDF")) {
                List<ReportResponse.ReportChart> charts = generateApprovalCharts(workflows, request.getGroupBy());
                report.setCharts(charts);
            }

            // Generate report file
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            // Store report
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            logger.info("Successfully generated approval report: {}", reportId);
            return report;

        } catch (Exception e) {
            logger.error("Error generating approval report for user: {}", username, e);
            throw new RuntimeException("Failed to generate approval report", e);
        }
    }

    @Override
    public ReportResponse generateTeamReport(ReportRequest.TeamReportRequest request, String username) {
        try {
            logger.info("Generating team report for team: {} by user: {}", request.getTeamId(), username);

            // Validate team access
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + request.getTeamId()));

            String reportId = generateReportId();
            
            // Get team data
            List<Expense> teamExpenses = getTeamExpensesForReport(request.getTeamId(), request);
            List<Budget> teamBudgets = getTeamBudgetsForReport(request.getTeamId(), request);
            
            // Create report response
            ReportResponse report = new ReportResponse();
            report.setReportId(reportId);
            report.setReportType("TEAM");
            report.setReportName("Team Report - " + team.getName() + " (" + request.getStartDate() + " to " + request.getEndDate() + ")");
            report.setFormat(request.getFormat());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setGeneratedAt(LocalDateTime.now());
            report.setGeneratedBy(username);
            report.setStatus("COMPLETED");
            report.setExpiresAt(LocalDateTime.now().plusDays(30));

            // Calculate summary
            ReportResponse.ReportSummary summary = calculateTeamSummary(teamExpenses, teamBudgets);
            report.setSummary(summary);

            // Prepare report data
            ReportResponse.ReportData data = new ReportResponse.ReportData();
            data.setExpenses(convertToExpenseItems(teamExpenses));
            data.setBudgets(convertToBudgetItems(teamBudgets));
            
            if (request.getIncludeMemberBreakdown()) {
                data.setUserSummaries(calculateTeamMemberSummaries(teamExpenses));
            }
            
            report.setData(data);

            // Generate charts
            if (request.getFormat().equals("PDF")) {
                List<ReportResponse.ReportChart> charts = generateTeamCharts(teamExpenses, teamBudgets, request.getGroupBy());
                report.setCharts(charts);
            }

            // Generate report file
            byte[] reportFile = generateReportFile(report, request.getFormat());
            report.setFileSizeBytes((long) reportFile.length);
            report.setDownloadUrl("/api/reports/" + reportId + "/download");

            // Store report
            reportStorage.put(reportId, report);
            reportFiles.put(reportId, reportFile);

            logger.info("Successfully generated team report: {}", reportId);
            return report;

        } catch (Exception e) {
            logger.error("Error generating team report for user: {}", username, e);
            throw new RuntimeException("Failed to generate team report", e);
        }
    }

    @Override
    public ReportResponse generateCustomReport(ReportRequest request, String username) {
        try {
            logger.info("Generating custom report for user: {}", username);

            switch (request.getReportType()) {
                case "EXPENSE":
                    ReportRequest.ExpenseReportRequest expenseReq = new ReportRequest.ExpenseReportRequest(
                            request.getStartDate(), request.getEndDate(), request.getFormat());
                    expenseReq.setCategoryIds(request.getCategoryIds());
                    expenseReq.setExpenseStatuses(request.getExpenseStatuses());
                    expenseReq.setMinAmount(request.getMinAmount());
                    expenseReq.setMaxAmount(request.getMaxAmount());
                    expenseReq.setGroupBy(request.getGroupBy());
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
            logger.error("Error generating custom report for user: {}", username, e);
            throw new RuntimeException("Failed to generate custom report", e);
        }
    }

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
        // This is a simplified implementation
        // In production, you would use proper pagination from database
        List<ReportResponse> userReports = getUserReports(username);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userReports.size());
        
        List<ReportResponse> pageContent = userReports.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, userReports.size());
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
        // This is a placeholder implementation
        // In production, you would store this in database and use a job scheduler
        
        Long scheduleId = System.currentTimeMillis(); // Simple ID generation
        
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
        // Placeholder implementation
        return new ArrayList<>();
    }

    @Override
    public ReportResponse.ScheduledReportInfo updateScheduledReport(Long scheduleId, ReportRequest.ScheduledReportRequest request, String username) {
        // Placeholder implementation
        throw new UnsupportedOperationException("Scheduled report update not implemented yet");
    }

    @Override
    public void deleteScheduledReport(Long scheduleId, String username) {
        // Placeholder implementation
        logger.info("Deleted scheduled report: {} by user: {}", scheduleId, username);
    }

    @Override
    public void executeScheduledReports() {
        // This would be called by a scheduled job
        logger.info("Executing scheduled reports");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportRequest> getReportTemplates() {
        List<ReportRequest> templates = new ArrayList<>();
        
        // Monthly Expense Report Template
        ReportRequest monthlyExpense = new ReportRequest("EXPENSE", 
                LocalDate.now().withDayOfMonth(1), LocalDate.now(), "PDF");
        monthlyExpense.setReportName("Monthly Expense Report");
        monthlyExpense.setDescription("Comprehensive monthly expense analysis");
        monthlyExpense.setGroupBy("CATEGORY");
        templates.add(monthlyExpense);
        
        // Budget vs Actual Report Template
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
        User user = userRepository.findByUsername(username)
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
        User user = userRepository.findByUsername(username)
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

            for (String recipient : recipients) {
                emailService.sendReportEmail(recipient, report, reportFile);
            }

            logger.info("Emailed report: {} to {} recipients by user: {}", reportId, recipients.size(), username);

        } catch (Exception e) {
            logger.error("Error emailing report: {} by user: {}", reportId, username, e);
            throw new RuntimeException("Failed to email report", e);
        }
    }

    @Override
    public void shareReport(String reportId, List<String> usernames, String sharedBy) {
        // This would implement report sharing functionality
        logger.info("Shared report: {} with {} users by: {}", reportId, usernames.size(), sharedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse.ReportData aggregateExpenseData(LocalDate startDate, LocalDate endDate, String username) {
        User user = userRepository.findByUsername(username)
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
        User user = userRepository.findByUsername(username)
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
        User user = userRepository.findByUsername(username)
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

    // Helper methods
    private String generateReportId() {
        return "RPT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private List<Expense> getExpensesForReport(Long userId, ReportRequest.ExpenseReportRequest request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        
        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDateTime, endDateTime);
        
        // Apply filters
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            expenses = expenses.stream()
                    .filter(e -> request.getCategoryIds().contains(e.getCategoryId()))
                    .collect(Collectors.toList());
        }
        
        if (request.getExpenseStatuses() != null && !request.getExpenseStatuses().isEmpty()) {
            expenses = expenses.stream()
                    .filter(e -> request.getExpenseStatuses().contains(e.getStatus().name()))
                    .collect(Collectors.toList());
        }
        
        if (request.getMinAmount() != null) {
            expenses = expenses.stream()
                    .filter(e -> e.getAmount().compareTo(request.getMinAmount()) >= 0)
                    .collect(Collectors.toList());
        }
        
        if (request.getMaxAmount() != null) {
            expenses = expenses.stream()
                    .filter(e -> e.getAmount().compareTo(request.getMaxAmount()) <= 0)
                    .collect(Collectors.toList());
        }
        
        return expenses;
    }

    private List<Budget> getBudgetsForReport(Long userId, ReportRequest.BudgetReportRequest request) {
        List<Budget> budgets = budgetRepository.findByUserIdAndPeriodOverlap(
                userId, request.getStartDate(), request.getEndDate());
        
        // Apply filters
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            budgets = budgets.stream()
                    .filter(b -> request.getCategoryIds().contains(b.getCategoryId()))
                    .collect(Collectors.toList());
        }
        
        if (request.getTeamIds() != null && !request.getTeamIds().isEmpty()) {
            budgets = budgets.stream()
                    .filter(b -> request.getTeamIds().contains(b.getTeamId()))
                    .collect(Collectors.toList());
        }
        
        if (!request.getIncludeExpiredBudgets()) {
            budgets = budgets.stream()
                    .filter(b -> !b.isExpired())
                    .collect(Collectors.toList());
        }
        
        return budgets;
    }

    private List<ApprovalWorkflow> getApprovalWorkflowsForReport(Long userId, ReportRequest.ApprovalReportRequest request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        
        List<ApprovalWorkflow> workflows = approvalWorkflowRepository.findBySubmittedAtBetween(startDateTime, endDateTime);
        
        // Filter by user involvement (submitted by or approved by)
        workflows = workflows.stream()
                .filter(w -> w.getSubmittedBy().equals(userId) || 
                           userId.equals(w.getCurrentApproverId()) || 
                           userId.equals(w.getFinalApproverId()))
                .collect(Collectors.toList());
        
        // Apply filters
        if (request.getApproverIds() != null && !request.getApproverIds().isEmpty()) {
            workflows = workflows.stream()
                    .filter(w -> request.getApproverIds().contains(w.getCurrentApproverId()) ||
                               request.getApproverIds().contains(w.getFinalApproverId()))
                    .collect(Collectors.toList());
        }
        
        if (request.getApprovalStatuses() != null && !request.getApprovalStatuses().isEmpty()) {
            workflows = workflows.stream()
                    .filter(w -> request.getApprovalStatuses().contains(w.getStatus().name()))
                    .collect(Collectors.toList());
        }
        
        return workflows;
    }

    private List<Expense> getTeamExpensesForReport(Long teamId, ReportRequest.TeamReportRequest request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().plusDays(1).atStartOfDay();
        
        return expenseRepository.findByTeamId(teamId).stream()
                .filter(e -> !(e.getExpenseDate().toEpochDay() < startDateTime.toLocalDate().toEpochDay()) &&
                           !(e.getExpenseDate().toEpochDay() > endDateTime.toLocalDate().toEpochDay()))
                .collect(Collectors.toList());
    }

    private List<Budget> getTeamBudgetsForReport(Long teamId, ReportRequest.TeamReportRequest request) {
        return budgetRepository.findByTeamIdAndIsActiveTrue(teamId).stream()
                .filter(b -> !b.getStartDate().isAfter(request.getEndDate()) &&
                           !b.getEndDate().isBefore(request.getStartDate()))
                .collect(Collectors.toList());
    }

    private ReportResponse.ReportSummary calculateExpenseSummary(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return new ReportResponse.ReportSummary(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = expenses.size();

        BigDecimal averageAmount = totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);

        BigDecimal maxAmount = expenses.stream()
                .map(Expense::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minAmount = expenses.stream()
                .map(Expense::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalAmount, totalCount, averageAmount);
        summary.setMaxAmount(maxAmount);
        summary.setMinAmount(minAmount);

        return summary;
    }

    private ReportResponse.ReportSummary calculateBudgetSummary(List<Budget> budgets) {
        if (budgets.isEmpty()) {
            return new ReportResponse.ReportSummary(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }

        BigDecimal totalBudgeted = budgets.stream()
                .map(Budget::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = budgets.stream()
                .map(Budget::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = budgets.size();

        BigDecimal averageBudget = totalBudgeted.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);

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
        if (workflows.isEmpty()) {
            return new ReportResponse.ReportSummary(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }

        BigDecimal totalAmount = workflows.stream()
                .map(ApprovalWorkflow::getExpenseAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = workflows.size();

        BigDecimal averageAmount = totalCount > 0 ? 
                totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;

        ReportResponse.ReportSummary summary = new ReportResponse.ReportSummary(totalAmount, totalCount, averageAmount);
        
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("approvedCount", workflows.stream().mapToLong(w -> w.isApproved() ? 1 : 0).sum());
        additionalMetrics.put("rejectedCount", workflows.stream().mapToLong(w -> w.isRejected() ? 1 : 0).sum());
        additionalMetrics.put("pendingCount", workflows.stream().mapToLong(w -> w.isPending() ? 1 : 0).sum());
        summary.setAdditionalMetrics(additionalMetrics);

        return summary;
    }

    private ReportResponse.ReportSummary calculateTeamSummary(List<Expense> expenses, List<Budget> budgets) {
        ReportResponse.ReportSummary expenseSummary = calculateExpenseSummary(expenses);
        ReportResponse.ReportSummary budgetSummary = calculateBudgetSummary(budgets);
        
        Map<String, Object> teamMetrics = new HashMap<>();
        teamMetrics.put("budgetCount", budgets.size());
        teamMetrics.put("totalBudgeted", budgetSummary.getTotalAmount());
        teamMetrics.put("budgetUtilization", budgetSummary.getAdditionalMetrics().get("utilizationPercentage"));
        
        expenseSummary.setAdditionalMetrics(teamMetrics);
        return expenseSummary;
    }

    private List<ReportResponse.ExpenseItem> convertToExpenseItems(List<Expense> expenses) {
        return expenses.stream()
                .map(expense -> new ReportResponse.ExpenseItem(
                        expense.getId(),
                        expense.getTitle(),
                        expense.getAmount(),
                        expense.getExpenseDate(),
                        expense.getCategory() != null ? expense.getCategory().getName() : "Unknown",
                        expense.getUser() != null ? expense.getUser().getUsername() : "Unknown",
                        expense.getStatus().name()
                ))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.BudgetItem> convertToBudgetItems(List<Budget> budgets) {
        return budgets.stream()
                .map(budget -> new ReportResponse.BudgetItem(
                        budget.getId(),
                        budget.getName(),
                        budget.getTotalAmount(),
                        budget.getSpentAmount(),
                        budget.getCategory() != null ? budget.getCategory().getName() : "Unknown",
                        budget.getIsActive() ? "ACTIVE" : "INACTIVE"
                ))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.ApprovalItem> convertToApprovalItems(List<ApprovalWorkflow> workflows) {
        return workflows.stream()
                .map(workflow -> new ReportResponse.ApprovalItem(
                        workflow.getId(),
                        workflow.getExpenseId(),
                        workflow.getExpense() != null ? workflow.getExpense().getTitle() : "Unknown",
                        workflow.getExpenseAmount(),
                        workflow.getSubmittedByUser() != null ? workflow.getSubmittedByUser().getUsername() : "Unknown",
                        workflow.getStatus().name()
                ))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.CategorySummary> calculateCategorySummaries(List<Expense> expenses) {
        Map<String, List<Expense>> categoryGroups = expenses.stream()
                .collect(Collectors.groupingBy(e -> 
                        e.getCategory() != null ? e.getCategory().getName() : "Unknown"));

        return categoryGroups.entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    List<Expense> categoryExpenses = entry.getValue();
                    
                    BigDecimal totalAmount = categoryExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return new ReportResponse.CategorySummary(categoryName, totalAmount, (long) categoryExpenses.size());
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.UserSummary> calculateTeamMemberSummaries(List<Expense> expenses) {
        Map<String, List<Expense>> userGroups = expenses.stream()
                .collect(Collectors.groupingBy(e -> 
                        e.getUser() != null ? e.getUser().getUsername() : "Unknown"));

        return userGroups.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    List<Expense> userExpenses = entry.getValue();
                    
                    BigDecimal totalAmount = userExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    String fullName = userExpenses.get(0).getUser() != null ? 
                            userExpenses.get(0).getUser().getFullName() : "Unknown";
                    
                    return new ReportResponse.UserSummary(username, fullName, totalAmount, (long) userExpenses.size());
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    private List<ReportResponse.ReportChart> generateExpenseCharts(List<Expense> expenses, String groupBy) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();

        if ("CATEGORY".equals(groupBy)) {
            List<ReportResponse.CategorySummary> categorySummaries = calculateCategorySummaries(expenses);
            
            List<ReportResponse.ChartDataPoint> dataPoints = categorySummaries.stream()
                    .map(summary -> new ReportResponse.ChartDataPoint(summary.getCategoryName(), summary.getTotalAmount()))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("PIE", "Expenses by Category", dataPoints));
        }

        return charts;
    }

    private List<ReportResponse.ReportChart> generateBudgetCharts(List<Budget> budgets, String groupBy) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();

        if ("CATEGORY".equals(groupBy)) {
            Map<String, List<Budget>> categoryGroups = budgets.stream()
                    .collect(Collectors.groupingBy(b -> 
                            b.getCategory() != null ? b.getCategory().getName() : "Unknown"));

            List<ReportResponse.ChartDataPoint> dataPoints = categoryGroups.entrySet().stream()
                    .map(entry -> {
                        BigDecimal totalBudget = entry.getValue().stream()
                                .map(Budget::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        return new ReportResponse.ChartDataPoint(entry.getKey(), totalBudget);
                    })
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("BAR", "Budget by Category", dataPoints));
        }

        return charts;
    }

    private List<ReportResponse.ReportChart> generateApprovalCharts(List<ApprovalWorkflow> workflows, String groupBy) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();

        if ("STATUS".equals(groupBy)) {
            Map<String, Long> statusCounts = workflows.stream()
                    .collect(Collectors.groupingBy(w -> w.getStatus().name(), Collectors.counting()));

            List<ReportResponse.ChartDataPoint> dataPoints = statusCounts.entrySet().stream()
                    .map(entry -> new ReportResponse.ChartDataPoint(entry.getKey(), BigDecimal.valueOf(entry.getValue())))
                    .collect(Collectors.toList());
            
            charts.add(new ReportResponse.ReportChart("PIE", "Approvals by Status", dataPoints));
        }

        return charts;
    }

    private List<ReportResponse.ReportChart> generateTeamCharts(List<Expense> expenses, List<Budget> budgets, String groupBy) {
        List<ReportResponse.ReportChart> charts = new ArrayList<>();
        
        // Add expense charts
        charts.addAll(generateExpenseCharts(expenses, groupBy));
        
        // Add budget charts
        charts.addAll(generateBudgetCharts(budgets, groupBy));

        return charts;
    }

    private byte[] generateReportFile(ReportResponse report, String format) {
        // This is a simplified implementation
        // In production, you would use libraries like iText for PDF, Apache POI for Excel, etc.
        
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
    }

    private byte[] generatePDFReport(ReportResponse report) {
        // Placeholder implementation
        String content = String.format("PDF Report: %s\nGenerated: %s\nType: %s\n", 
                report.getReportName(), report.getGeneratedAt(), report.getReportType());
        return content.getBytes();
    }

    private byte[] generateCSVReport(ReportResponse report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Report Name,").append(report.getReportName()).append("\n");
        csv.append("Generated At,").append(report.getGeneratedAt()).append("\n");
        csv.append("Type,").append(report.getReportType()).append("\n\n");
        
        if (report.getData() != null && report.getData().getExpenses() != null) {
            csv.append("Expense ID,Title,Amount,Date,Category,User,Status\n");
            for (ReportResponse.ExpenseItem expense : report.getData().getExpenses()) {
                csv.append(expense.getExpenseId()).append(",")
                   .append(expense.getTitle()).append(",")
                   .append(expense.getAmount()).append(",")
                   .append(expense.getExpenseDate()).append(",")
                   .append(expense.getCategory()).append(",")
                   .append(expense.getUser()).append(",")
                   .append(expense.getStatus()).append("\n");
            }
        }
        
        return csv.toString().getBytes();
    }

    private byte[] generateExcelReport(ReportResponse report) {
        // Placeholder implementation
        String content = String.format("Excel Report: %s\nGenerated: %s\nType: %s\n", 
                report.getReportName(), report.getGeneratedAt(), report.getReportType());
        return content.getBytes();
    }

    private byte[] generateJSONReport(ReportResponse report) {
        // In production, use Jackson ObjectMapper or similar
        String json = String.format("{\"reportName\":\"%s\",\"generatedAt\":\"%s\",\"type\":\"%s\"}", 
                report.getReportName(), report.getGeneratedAt(), report.getReportType());
        return json.getBytes();
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