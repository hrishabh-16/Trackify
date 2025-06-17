package com.trackify.scheduler;

import com.trackify.dto.request.ReportRequest;
import com.trackify.dto.response.ReportResponse;
import com.trackify.service.ReportService;
import com.trackify.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReportScheduler.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private EmailService emailService;

    // In-memory storage for scheduled reports (in production, use database)
    private final Map<Long, ScheduledReportConfig> scheduledReports = new ConcurrentHashMap<>();
    private Long nextScheduleId = 1L;

    /**
     * Execute scheduled reports every hour
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour at the top of the hour
    public void executeScheduledReports() {
        logger.info("Starting scheduled reports execution at: {}", LocalDateTime.now());

        try {
            LocalDateTime now = LocalDateTime.now();
            List<ScheduledReportConfig> reportsToExecute = getReportsToExecute(now);

            if (reportsToExecute.isEmpty()) {
                logger.debug("No scheduled reports to execute at this time");
                return;
            }

            logger.info("Found {} scheduled reports to execute", reportsToExecute.size());

            for (ScheduledReportConfig config : reportsToExecute) {
                try {
                    executeScheduledReport(config);
                    updateNextExecutionTime(config);
                } catch (Exception e) {
                    logger.error("Failed to execute scheduled report: {} for user: {}", 
                            config.getReportName(), config.getCreatedBy(), e);
                    handleReportExecutionFailure(config, e);
                }
            }

            logger.info("Completed scheduled reports execution");

        } catch (Exception e) {
            logger.error("Error during scheduled reports execution", e);
        }
    }

    /**
     * Cleanup expired reports every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupExpiredReports() {
        logger.info("Starting cleanup of expired reports at: {}", LocalDateTime.now());

        try {
            reportService.cleanupExpiredReports();

            // Also cleanup old scheduled report executions
            cleanupOldScheduledReportExecutions();

            logger.info("Completed cleanup of expired reports");

        } catch (Exception e) {
            logger.error("Error during expired reports cleanup", e);
        }
    }

    /**
     * Generate weekly summary reports every Monday at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * MON") // Every Monday at 9 AM
    public void generateWeeklySummaryReports() {
        logger.info("Starting weekly summary reports generation at: {}", LocalDateTime.now());

        try {
            // Generate weekly summaries for active users
            generateAutomaticWeeklySummaries();

            logger.info("Completed weekly summary reports generation");

        } catch (Exception e) {
            logger.error("Error during weekly summary reports generation", e);
        }
    }

    /**
     * Generate monthly summary reports on the 1st of each month at 8 AM
     */
    @Scheduled(cron = "0 0 8 1 * ?") // 1st day of month at 8 AM
    public void generateMonthlySummaryReports() {
        logger.info("Starting monthly summary reports generation at: {}", LocalDateTime.now());

        try {
            // Generate monthly summaries for active users
            generateAutomaticMonthlySummaries();

            logger.info("Completed monthly summary reports generation");

        } catch (Exception e) {
            logger.error("Error during monthly summary reports generation", e);
        }
    }

    /**
     * Health check for scheduled reports every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public void healthCheckScheduledReports() {
        try {
            logger.debug("Performing health check on scheduled reports");

            // Check for failed reports that need retry
            retryFailedReports();

            // Validate scheduled report configurations
            validateScheduledReportConfigurations();

            logger.debug("Completed health check on scheduled reports");

        } catch (Exception e) {
            logger.error("Error during scheduled reports health check", e);
        }
    }

    // Public methods for managing scheduled reports

    public ReportResponse.ScheduledReportInfo createScheduledReport(
            ReportRequest.ScheduledReportRequest request, String username) {
        
        logger.info("Creating scheduled report: {} for user: {}", request.getReportName(), username);

        ScheduledReportConfig config = new ScheduledReportConfig();
        config.setScheduleId(nextScheduleId++);
        config.setReportName(request.getReportName());
        config.setReportConfig(request.getReportConfig());
        config.setFrequency(request.getFrequency());
        config.setEmailRecipients(new ArrayList<>(request.getEmailRecipients()));
        config.setIsActive(request.getIsActive());
        config.setDescription(request.getDescription());
        config.setCreatedBy(username);
        config.setCreatedAt(LocalDateTime.now());
        config.setLastExecuted(null);
        config.setNextExecution(calculateNextExecutionTime(request.getFrequency()));
        config.setExecutionCount(0);
        config.setStatus("ACTIVE");

        scheduledReports.put(config.getScheduleId(), config);

        logger.info("Successfully created scheduled report with ID: {}", config.getScheduleId());

        return convertToScheduledReportInfo(config);
    }

    public List<ReportResponse.ScheduledReportInfo> getScheduledReports(String username) {
        return scheduledReports.values().stream()
                .filter(config -> username.equals(config.getCreatedBy()))
                .map(this::convertToScheduledReportInfo)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    public ReportResponse.ScheduledReportInfo updateScheduledReport(
            Long scheduleId, ReportRequest.ScheduledReportRequest request, String username) {
        
        ScheduledReportConfig config = scheduledReports.get(scheduleId);
        if (config == null) {
            throw new RuntimeException("Scheduled report not found: " + scheduleId);
        }

        if (!username.equals(config.getCreatedBy())) {
            throw new RuntimeException("Access denied to scheduled report: " + scheduleId);
        }

        logger.info("Updating scheduled report: {} for user: {}", scheduleId, username);

        config.setReportName(request.getReportName());
        config.setReportConfig(request.getReportConfig());
        config.setFrequency(request.getFrequency());
        config.setEmailRecipients(new ArrayList<>(request.getEmailRecipients()));
        config.setIsActive(request.getIsActive());
        config.setDescription(request.getDescription());
        config.setUpdatedAt(LocalDateTime.now());

        // Recalculate next execution time if frequency changed
        config.setNextExecution(calculateNextExecutionTime(request.getFrequency()));

        logger.info("Successfully updated scheduled report: {}", scheduleId);

        return convertToScheduledReportInfo(config);
    }

    public void deleteScheduledReport(Long scheduleId, String username) {
        ScheduledReportConfig config = scheduledReports.get(scheduleId);
        if (config == null) {
            throw new RuntimeException("Scheduled report not found: " + scheduleId);
        }

        if (!username.equals(config.getCreatedBy())) {
            throw new RuntimeException("Access denied to scheduled report: " + scheduleId);
        }

        logger.info("Deleting scheduled report: {} for user: {}", scheduleId, username);

        scheduledReports.remove(scheduleId);

        logger.info("Successfully deleted scheduled report: {}", scheduleId);
    }

    public void pauseScheduledReport(Long scheduleId, String username) {
        ScheduledReportConfig config = scheduledReports.get(scheduleId);
        if (config == null) {
            throw new RuntimeException("Scheduled report not found: " + scheduleId);
        }

        if (!username.equals(config.getCreatedBy())) {
            throw new RuntimeException("Access denied to scheduled report: " + scheduleId);
        }

        logger.info("Pausing scheduled report: {} for user: {}", scheduleId, username);

        config.setIsActive(false);
        config.setStatus("PAUSED");
        config.setUpdatedAt(LocalDateTime.now());

        logger.info("Successfully paused scheduled report: {}", scheduleId);
    }

    public void resumeScheduledReport(Long scheduleId, String username) {
        ScheduledReportConfig config = scheduledReports.get(scheduleId);
        if (config == null) {
            throw new RuntimeException("Scheduled report not found: " + scheduleId);
        }

        if (!username.equals(config.getCreatedBy())) {
            throw new RuntimeException("Access denied to scheduled report: " + scheduleId);
        }

        logger.info("Resuming scheduled report: {} for user: {}", scheduleId, username);

        config.setIsActive(true);
        config.setStatus("ACTIVE");
        config.setUpdatedAt(LocalDateTime.now());
        config.setNextExecution(calculateNextExecutionTime(config.getFrequency()));

        logger.info("Successfully resumed scheduled report: {}", scheduleId);
    }

    // Private helper methods

    private List<ScheduledReportConfig> getReportsToExecute(LocalDateTime now) {
        return scheduledReports.values().stream()
                .filter(config -> config.getIsActive())
                .filter(config -> "ACTIVE".equals(config.getStatus()))
                .filter(config -> config.getNextExecution() != null && 
                                !config.getNextExecution().isAfter(now))
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    private void executeScheduledReport(ScheduledReportConfig config) {
        logger.info("Executing scheduled report: {} for user: {}", 
                config.getReportName(), config.getCreatedBy());

        try {
            // Update date range for the report based on frequency
            updateReportDateRange(config);

            // Generate the report
            ReportResponse report = generateReport(config);

            // Send email notifications
            sendReportNotifications(config, report);

            // Update execution statistics
            config.setLastExecuted(LocalDateTime.now());
            config.setExecutionCount(config.getExecutionCount() + 1);
            config.setStatus("ACTIVE");
            config.setLastError(null);

            logger.info("Successfully executed scheduled report: {} (execution #{})", 
                    config.getReportName(), config.getExecutionCount());

        } catch (Exception e) {
            logger.error("Failed to execute scheduled report: {}", config.getReportName(), e);
            config.setStatus("FAILED");
            config.setLastError(e.getMessage());
            config.setFailureCount(config.getFailureCount() + 1);
            throw e;
        }
    }

    private void updateReportDateRange(ScheduledReportConfig config) {
        ReportRequest reportConfig = config.getReportConfig();
        LocalDate now = LocalDate.now();

        switch (config.getFrequency().toUpperCase()) {
            case "DAILY":
                reportConfig.setStartDate(now.minusDays(1));
                reportConfig.setEndDate(now.minusDays(1));
                break;
            case "WEEKLY":
                reportConfig.setStartDate(now.minusWeeks(1).with(java.time.DayOfWeek.MONDAY));
                reportConfig.setEndDate(now.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY));
                break;
            case "MONTHLY":
                reportConfig.setStartDate(now.minusMonths(1).withDayOfMonth(1));
                reportConfig.setEndDate(now.minusMonths(1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()));
                break;
            case "QUARTERLY":
                // Calculate previous quarter
                int currentQuarter = (now.getMonthValue() - 1) / 3;
                int previousQuarter = currentQuarter == 0 ? 3 : currentQuarter - 1;
                int quarterStartMonth = previousQuarter * 3 + 1;
                LocalDate quarterStart = now.withMonth(quarterStartMonth).withDayOfMonth(1);
                if (currentQuarter == 0) {
                    quarterStart = quarterStart.minusYears(1);
                }
                LocalDate quarterEnd = quarterStart.plusMonths(2).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                
                reportConfig.setStartDate(quarterStart);
                reportConfig.setEndDate(quarterEnd);
                break;
            default:
                logger.warn("Unknown frequency: {}, using daily range", config.getFrequency());
                reportConfig.setStartDate(now.minusDays(1));
                reportConfig.setEndDate(now.minusDays(1));
        }

        logger.debug("Updated report date range for {}: {} to {}", 
                config.getFrequency(), reportConfig.getStartDate(), reportConfig.getEndDate());
    }

    private ReportResponse generateReport(ScheduledReportConfig config) {
        ReportRequest reportConfig = config.getReportConfig();
        String username = config.getCreatedBy();

        // Set report name with timestamp
        String timestampedName = config.getReportName() + " - " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        reportConfig.setReportName(timestampedName);

        // Generate report based on type
        switch (reportConfig.getReportType().toUpperCase()) {
            case "EXPENSE":
                ReportRequest.ExpenseReportRequest expenseReq = new ReportRequest.ExpenseReportRequest(
                        reportConfig.getStartDate(), reportConfig.getEndDate(), reportConfig.getFormat());
                expenseReq.setCategoryIds(reportConfig.getCategoryIds());
                expenseReq.setExpenseStatuses(reportConfig.getExpenseStatuses());
                expenseReq.setMinAmount(reportConfig.getMinAmount());
                expenseReq.setMaxAmount(reportConfig.getMaxAmount());
                expenseReq.setGroupBy(reportConfig.getGroupBy());
                return reportService.generateExpenseReport(expenseReq, username);

            case "BUDGET":
                ReportRequest.BudgetReportRequest budgetReq = new ReportRequest.BudgetReportRequest(
                        reportConfig.getStartDate(), reportConfig.getEndDate(), reportConfig.getFormat());
                budgetReq.setCategoryIds(reportConfig.getCategoryIds());
                budgetReq.setTeamIds(reportConfig.getTeamIds());
                budgetReq.setGroupBy(reportConfig.getGroupBy());
                return reportService.generateBudgetReport(budgetReq, username);

            case "APPROVAL":
                ReportRequest.ApprovalReportRequest approvalReq = new ReportRequest.ApprovalReportRequest(
                        reportConfig.getStartDate(), reportConfig.getEndDate(), reportConfig.getFormat());
                approvalReq.setApprovalStatuses(reportConfig.getExpenseStatuses());
                approvalReq.setGroupBy(reportConfig.getGroupBy());
                return reportService.generateApprovalReport(approvalReq, username);

            default:
                return reportService.generateCustomReport(reportConfig, username);
        }
    }

    private void sendReportNotifications(ScheduledReportConfig config, ReportResponse report) {
        try {
            if (config.getEmailRecipients() != null && !config.getEmailRecipients().isEmpty()) {
                byte[] reportFile = reportService.downloadReport(report.getReportId(), config.getCreatedBy());
                
                for (String recipient : config.getEmailRecipients()) {
                    try {
                        emailService.sendScheduledReportEmail(recipient, config, report, reportFile);
                        logger.debug("Sent scheduled report email to: {}", recipient);
                    } catch (Exception e) {
                        logger.error("Failed to send scheduled report email to: {}", recipient, e);
                    }
                }

                logger.info("Sent scheduled report notifications to {} recipients", 
                        config.getEmailRecipients().size());
            }
        } catch (Exception e) {
            logger.error("Error sending report notifications for scheduled report: {}", 
                    config.getReportName(), e);
        }
    }

    private void updateNextExecutionTime(ScheduledReportConfig config) {
        LocalDateTime nextExecution = calculateNextExecutionTime(config.getFrequency());
        config.setNextExecution(nextExecution);
        
        logger.debug("Updated next execution time for {}: {}", 
                config.getReportName(), nextExecution);
    }

    private LocalDateTime calculateNextExecutionTime(String frequency) {
        LocalDateTime now = LocalDateTime.now();

        switch (frequency.toUpperCase()) {
            case "DAILY":
                return now.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
            case "WEEKLY":
                return now.plusWeeks(1).with(java.time.DayOfWeek.MONDAY)
                        .withHour(8).withMinute(0).withSecond(0).withNano(0);
            case "MONTHLY":
                return now.plusMonths(1).withDayOfMonth(1)
                        .withHour(8).withMinute(0).withSecond(0).withNano(0);
            case "QUARTERLY":
                return now.plusMonths(3).withDayOfMonth(1)
                        .withHour(8).withMinute(0).withSecond(0).withNano(0);
            default:
                logger.warn("Unknown frequency: {}, defaulting to daily", frequency);
                return now.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        }
    }

    private void handleReportExecutionFailure(ScheduledReportConfig config, Exception error) {
        config.setStatus("FAILED");
        config.setLastError(error.getMessage());
        config.setFailureCount(config.getFailureCount() + 1);

        // Disable if too many failures
        if (config.getFailureCount() >= 5) {
            config.setIsActive(false);
            config.setStatus("DISABLED_DUE_TO_FAILURES");
            
            logger.warn("Disabled scheduled report {} due to {} consecutive failures", 
                    config.getReportName(), config.getFailureCount());

            // Send notification about disabled report
            try {
                emailService.sendScheduledReportFailureNotification(config, error);
            } catch (Exception e) {
                logger.error("Failed to send failure notification for scheduled report: {}", 
                        config.getReportName(), e);
            }
        }
    }

    private void cleanupOldScheduledReportExecutions() {
        // Remove execution history older than 6 months
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
        
        scheduledReports.values().forEach(config -> {
            if (config.getLastExecuted() != null && config.getLastExecuted().isBefore(cutoffDate) && 
                !config.getIsActive()) {
                // Could implement cleanup logic here
                logger.debug("Old inactive scheduled report found: {}", config.getReportName());
            }
        });
    }

    private void generateAutomaticWeeklySummaries() {
        // This could be implemented to generate automatic weekly summaries for active users
        logger.info("Generating automatic weekly summaries");
    }

    private void generateAutomaticMonthlySummaries() {
        // This could be implemented to generate automatic monthly summaries for active users
        logger.info("Generating automatic monthly summaries");
    }

    private void retryFailedReports() {
        LocalDateTime now = LocalDateTime.now();
        
        scheduledReports.values().stream()
                .filter(config -> "FAILED".equals(config.getStatus()))
                .filter(config -> config.getIsActive())
                .filter(config -> config.getFailureCount() < 3)
                .filter(config -> config.getLastExecuted() == null || 
                               config.getLastExecuted().plusHours(1).isBefore(now))
                .forEach(config -> {
                    try {
                        logger.info("Retrying failed scheduled report: {}", config.getReportName());
                        executeScheduledReport(config);
                        updateNextExecutionTime(config);
                    } catch (Exception e) {
                        logger.error("Retry failed for scheduled report: {}", config.getReportName(), e);
                    }
                });
    }

    private void validateScheduledReportConfigurations() {
        scheduledReports.values().forEach(config -> {
            try {
                // Validate email recipients
                if (config.getEmailRecipients() == null || config.getEmailRecipients().isEmpty()) {
                    logger.warn("Scheduled report {} has no email recipients", config.getReportName());
                }

                // Validate report configuration
                if (config.getReportConfig() == null) {
                    logger.error("Scheduled report {} has null report configuration", config.getReportName());
                    config.setIsActive(false);
                    config.setStatus("INVALID_CONFIG");
                }

                // Validate frequency
                if (!Arrays.asList("DAILY", "WEEKLY", "MONTHLY", "QUARTERLY")
                        .contains(config.getFrequency().toUpperCase())) {
                    logger.error("Scheduled report {} has invalid frequency: {}", 
                            config.getReportName(), config.getFrequency());
                    config.setIsActive(false);
                    config.setStatus("INVALID_FREQUENCY");
                }

            } catch (Exception e) {
                logger.error("Error validating scheduled report configuration: {}", 
                        config.getReportName(), e);
            }
        });
    }

    private ReportResponse.ScheduledReportInfo convertToScheduledReportInfo(ScheduledReportConfig config) {
        ReportResponse.ScheduledReportInfo info = new ReportResponse.ScheduledReportInfo();
        info.setScheduleId(config.getScheduleId());
        info.setReportName(config.getReportName());
        info.setFrequency(config.getFrequency());
        info.setIsActive(config.getIsActive());
        info.setEmailRecipients(config.getEmailRecipients());
        info.setLastGenerated(config.getLastExecuted());
        info.setNextScheduled(config.getNextExecution());
        info.setStatus(config.getStatus());
        info.setExecutionCount(config.getExecutionCount());
        info.setDescription(config.getDescription());
        return info;
    }

    // Inner class for scheduled report configuration
    private static class ScheduledReportConfig {
        private Long scheduleId;
        private String reportName;
        private ReportRequest reportConfig;
        private String frequency;
        private List<String> emailRecipients;
        private Boolean isActive;
        private String description;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastExecuted;
        private LocalDateTime nextExecution;
        private Integer executionCount = 0;
        private Integer failureCount = 0;
        private String status;
        private String lastError;

        // Getters and setters
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

        public String getReportName() { return reportName; }
        public void setReportName(String reportName) { this.reportName = reportName; }

        public ReportRequest getReportConfig() { return reportConfig; }
        public void setReportConfig(ReportRequest reportConfig) { this.reportConfig = reportConfig; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public List<String> getEmailRecipients() { return emailRecipients; }
        public void setEmailRecipients(List<String> emailRecipients) { this.emailRecipients = emailRecipients; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public LocalDateTime getLastExecuted() { return lastExecuted; }
        public void setLastExecuted(LocalDateTime lastExecuted) { this.lastExecuted = lastExecuted; }

        public LocalDateTime getNextExecution() { return nextExecution; }
        public void setNextExecution(LocalDateTime nextExecution) { this.nextExecution = nextExecution; }

        public Integer getExecutionCount() { return executionCount; }
        public void setExecutionCount(Integer executionCount) { this.executionCount = executionCount; }

        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
    }
}