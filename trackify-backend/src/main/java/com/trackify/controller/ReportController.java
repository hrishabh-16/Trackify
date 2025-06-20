package com.trackify.controller;

import com.trackify.dto.request.ReportRequest;
import com.trackify.dto.response.ReportResponse;
import com.trackify.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
@Tag(name = "Report Management", description = "APIs for generating and managing reports")
@Validated
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    // Generate expense report
    @PostMapping("/expense")
    @Operation(summary = "Generate expense report", description = "Generate a detailed expense report")
    public ResponseEntity<ReportResponse> generateExpenseReport(
            @Valid @RequestBody ReportRequest.ExpenseReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating expense report for user: {}", authentication.getName());
        
        ReportResponse report = reportService.generateExpenseReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Generate budget report
    @PostMapping("/budget")
    @Operation(summary = "Generate budget report", description = "Generate a budget vs actual report")
    public ResponseEntity<ReportResponse> generateBudgetReport(
            @Valid @RequestBody ReportRequest.BudgetReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating budget report for user: {}", authentication.getName());
        
        ReportResponse report = reportService.generateBudgetReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Generate approval report
    @PostMapping("/approval")
    @Operation(summary = "Generate approval report", description = "Generate an approval workflow report")
    public ResponseEntity<ReportResponse> generateApprovalReport(
            @Valid @RequestBody ReportRequest.ApprovalReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating approval report for user: {}", authentication.getName());
        
        ReportResponse report = reportService.generateApprovalReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Generate team report
    @PostMapping("/team")
    @Operation(summary = "Generate team report", description = "Generate a team performance report")
    public ResponseEntity<ReportResponse> generateTeamReport(
            @Valid @RequestBody ReportRequest.TeamReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating team report for team: {} by user: {}", 
                request.getTeamId(), authentication.getName());
        
        ReportResponse report = reportService.generateTeamReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Generate custom report
    @PostMapping("/custom")
    @Operation(summary = "Generate custom report", description = "Generate a custom report with advanced analysis")
    public ResponseEntity<ReportResponse> generateCustomReport(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating custom report type: {} for user: {}", 
                request.getReportType(), authentication.getName());
        
        ReportResponse report = reportService.generateCustomReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Get report by ID
    @GetMapping("/{reportId}")
    @Operation(summary = "Get report by ID", description = "Retrieve a specific report by its ID")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable String reportId,
            Authentication authentication) {
        
        logger.info("Retrieving report: {} for user: {}", reportId, authentication.getName());
        
        ReportResponse report = reportService.getReportById(reportId, authentication.getName());
        return ResponseEntity.ok(report);
    }

    // Get user reports
    @GetMapping
    @Operation(summary = "Get user reports", description = "Get all reports for the authenticated user")
    public ResponseEntity<List<ReportResponse>> getUserReports(Authentication authentication) {
        logger.info("Retrieving reports for user: {}", authentication.getName());
        
        List<ReportResponse> reports = reportService.getUserReports(authentication.getName());
        return ResponseEntity.ok(reports);
    }

    // Get user reports with pagination
    @GetMapping("/paginated")
    @Operation(summary = "Get paginated user reports", description = "Get paginated reports for the authenticated user")
    public ResponseEntity<Page<ReportResponse>> getUserReportsPaginated(
            @PageableDefault(size = 10, sort = "generatedAt") Pageable pageable,
            Authentication authentication) {
        
        logger.info("Retrieving paginated reports for user: {}", authentication.getName());
        
        Page<ReportResponse> reports = reportService.getUserReports(authentication.getName(), pageable);
        return ResponseEntity.ok(reports);
    }

    // Download report - FIXED
    @GetMapping("/{reportId}/download")
    @Operation(summary = "Download report", description = "Download a report file")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String reportId,
            Authentication authentication) {
        try {
            logger.info("Downloading report: {} for user: {}", reportId, authentication.getName());
            
            // Get report details to determine content type
            ReportResponse report = reportService.getReportById(reportId, authentication.getName());
            byte[] reportData = reportService.downloadReport(reportId, authentication.getName());
            
            // Determine content type and filename based on format
            MediaType contentType = getMediaTypeForFormat(report.getFormat());
            String filename = generateFileName(report.getReportName(), report.getFormat());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(contentType)
                    .body(reportData);
                    
        } catch (Exception e) {
            logger.error("Error downloading report: {}", reportId, e);
            throw e;
        }
    }

    // Get report download URL
    @GetMapping("/{reportId}/download-url")
    @Operation(summary = "Get download URL", description = "Get the download URL for a report")
    public ResponseEntity<Map<String, String>> getReportDownloadUrl(
            @PathVariable String reportId,
            Authentication authentication) {
        
        logger.info("Getting download URL for report: {} by user: {}", reportId, authentication.getName());
        
        String downloadUrl = reportService.getReportDownloadUrl(reportId, authentication.getName());
        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }

    // Delete report
    @DeleteMapping("/{reportId}")
    @Operation(summary = "Delete report", description = "Delete a specific report")
    public ResponseEntity<Map<String, String>> deleteReport(
            @PathVariable String reportId,
            Authentication authentication) {
        
        logger.info("Deleting report: {} by user: {}", reportId, authentication.getName());
        
        reportService.deleteReport(reportId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
    }

    // Create scheduled report - FIXED
    @PostMapping("/scheduled")
    @Operation(summary = "Create scheduled report", description = "Create a new scheduled report")
    public ResponseEntity<ReportResponse.ScheduledReportInfo> createScheduledReport(
            @Valid @RequestBody CreateScheduledReportRequest request,
            Authentication authentication) {
        try {
            logger.info("Creating scheduled report for user: {}", authentication.getName());
            
            // Convert to internal ScheduledReportRequest with proper mapping
            ReportRequest.ScheduledReportRequest scheduledRequest = new ReportRequest.ScheduledReportRequest();
            scheduledRequest.setReportName(request.getReportName());
            scheduledRequest.setFrequency(request.getScheduleFrequency());
            scheduledRequest.setEmailRecipients(request.getRecipients());
            scheduledRequest.setIsActive(true);
            scheduledRequest.setDescription(request.getDescription());
            
            // Create report config from request
            ReportRequest reportConfig = new ReportRequest();
            reportConfig.setReportType(request.getReportType());
            reportConfig.setStartDate(request.getStartDate());
            reportConfig.setEndDate(request.getEndDate());
            reportConfig.setFormat(request.getFormat());
            
            scheduledRequest.setReportConfig(reportConfig);
            
            ReportResponse.ScheduledReportInfo scheduledReport = reportService.createScheduledReport(
                    scheduledRequest, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(scheduledReport);
        } catch (Exception e) {
            logger.error("Error creating scheduled report", e);
            throw e;
        }
    }

    // Get scheduled reports
    @GetMapping("/scheduled")
    @Operation(summary = "Get scheduled reports", description = "Get all scheduled reports for the user")
    public ResponseEntity<List<ReportResponse.ScheduledReportInfo>> getScheduledReports(
            Authentication authentication) {
        try {
            logger.info("Retrieving scheduled reports for user: {}", authentication.getName());
            List<ReportResponse.ScheduledReportInfo> scheduledReports = reportService.getScheduledReports(
                    authentication.getName());
            return ResponseEntity.ok(scheduledReports);
        } catch (Exception e) {
            logger.error("Error retrieving scheduled reports", e);
            throw e;
        }
    }

    // Update scheduled report - FIXED
    @PutMapping("/scheduled/{scheduleId}")
    @Operation(summary = "Update scheduled report", description = "Update an existing scheduled report")
    public ResponseEntity<ReportResponse.ScheduledReportInfo> updateScheduledReport(
            @PathVariable Long scheduleId,
            @Valid @RequestBody CreateScheduledReportRequest request,
            Authentication authentication) {
        try {
            logger.info("Updating scheduled report: {} for user: {}", scheduleId, authentication.getName());
            
            // Convert to internal ScheduledReportRequest with proper mapping
            ReportRequest.ScheduledReportRequest scheduledRequest = new ReportRequest.ScheduledReportRequest();
            scheduledRequest.setReportName(request.getReportName());
            scheduledRequest.setFrequency(request.getScheduleFrequency());
            scheduledRequest.setEmailRecipients(request.getRecipients());
            scheduledRequest.setIsActive(true);
            scheduledRequest.setDescription(request.getDescription());
            
            // Create report config from request
            ReportRequest reportConfig = new ReportRequest();
            reportConfig.setReportType(request.getReportType());
            reportConfig.setStartDate(request.getStartDate());
            reportConfig.setEndDate(request.getEndDate());
            reportConfig.setFormat(request.getFormat());
            
            scheduledRequest.setReportConfig(reportConfig);
            
            ReportResponse.ScheduledReportInfo updatedReport = reportService.updateScheduledReport(
                    scheduleId, scheduledRequest, authentication.getName());
            return ResponseEntity.ok(updatedReport);
        } catch (Exception e) {
            logger.error("Error updating scheduled report", e);
            throw e;
        }
    }

    // Delete scheduled report
    @DeleteMapping("/scheduled/{scheduleId}")
    @Operation(summary = "Delete scheduled report", description = "Delete a scheduled report")
    public ResponseEntity<Map<String, String>> deleteScheduledReport(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        try {
            logger.info("Deleting scheduled report: {} by user: {}", scheduleId, authentication.getName());
            reportService.deleteScheduledReport(scheduleId, authentication.getName());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Scheduled report deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting scheduled report", e);
            throw e;
        }
    }

    // Get report templates
    @GetMapping("/templates")
    @Operation(summary = "Get report templates", description = "Get available report templates")
    public ResponseEntity<List<ReportRequest>> getReportTemplates() {
        try {
            logger.info("Retrieving report templates");
            List<ReportRequest> templates = reportService.getReportTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("Error retrieving report templates", e);
            throw e;
        }
    }

    // Get specific report template
    @GetMapping("/templates/{templateName}")
    @Operation(summary = "Get report template", description = "Get a specific report template by name")
    public ResponseEntity<ReportRequest> getReportTemplate(
            @PathVariable String templateName) {
        logger.info("Retrieving report template: {}", templateName);
        
        ReportRequest template = reportService.getReportTemplate(templateName);
        return ResponseEntity.ok(template);
    }

    // Get report summary
    @PostMapping("/summary")
    @Operation(summary = "Get report summary", description = "Get a quick summary for report parameters")
    public ResponseEntity<ReportResponse.ReportSummary> getReportSummary(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating report summary for user: {}", authentication.getName());
        
        ReportResponse.ReportSummary summary = reportService.getReportSummary(
                request, authentication.getName());
        return ResponseEntity.ok(summary);
    }

    // Get report charts
    @PostMapping("/charts")
    @Operation(summary = "Get report charts", description = "Generate charts for report data")
    public ResponseEntity<List<ReportResponse.ReportChart>> getReportCharts(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating report charts for user: {}", authentication.getName());
        
        List<ReportResponse.ReportChart> charts = reportService.getReportCharts(
                request, authentication.getName());
        return ResponseEntity.ok(charts);
    }

    // Email report
    @PostMapping("/{reportId}/email")
    @Operation(summary = "Email report", description = "Email a report to specified recipients")
    public ResponseEntity<Map<String, String>> emailReport(
            @PathVariable String reportId,
            @RequestBody Map<String, List<String>> emailRequest,
            Authentication authentication) {
        try {
            List<String> recipients = emailRequest.get("recipients");
            if (recipients == null || recipients.isEmpty()) {
                throw new IllegalArgumentException("Recipients are required");
            }
            
            logger.info("Emailing report: {} to {} recipients", reportId, recipients.size());
            reportService.emailReport(reportId, recipients, authentication.getName());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Report emailed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error emailing report: {}", reportId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // Share report
    @PostMapping("/{reportId}/share")
    @Operation(summary = "Share report", description = "Share a report with other users")
    public ResponseEntity<Map<String, String>> shareReport(
            @PathVariable String reportId,
            @RequestBody Map<String, Object> shareRequest,
            Authentication authentication) {
        try {
            @SuppressWarnings("unchecked")
            List<String> usernames = (List<String>) shareRequest.get("usernames");
            if (usernames == null || usernames.isEmpty()) {
                throw new IllegalArgumentException("Usernames are required");
            }
            
            logger.info("Sharing report: {} with {} users by: {}", 
                    reportId, usernames.size(), authentication.getName());
            
            reportService.shareReport(reportId, usernames, authentication.getName());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Report shared successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error sharing report: {}", reportId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Aggregate expense data
    @GetMapping("/data/expenses")
    @Operation(summary = "Aggregate expense data", description = "Get aggregated expense data for a date range")
    public ResponseEntity<ReportResponse.ReportData> aggregateExpenseData(
            @RequestParam @Parameter(description = "Start date") LocalDate startDate,
            @RequestParam @Parameter(description = "End date") LocalDate endDate,
            Authentication authentication) {
        
        logger.info("Aggregating expense data from {} to {} for user: {}", 
                startDate, endDate, authentication.getName());
        
        ReportResponse.ReportData data = reportService.aggregateExpenseData(
                startDate, endDate, authentication.getName());
        return ResponseEntity.ok(data);
    }

    // Aggregate budget data
    @GetMapping("/data/budgets")
    @Operation(summary = "Aggregate budget data", description = "Get aggregated budget data for a date range")
    public ResponseEntity<ReportResponse.ReportData> aggregateBudgetData(
            @RequestParam @Parameter(description = "Start date") LocalDate startDate,
            @RequestParam @Parameter(description = "End date") LocalDate endDate,
            Authentication authentication) {
        
        logger.info("Aggregating budget data from {} to {} for user: {}", 
                startDate, endDate, authentication.getName());
        
        ReportResponse.ReportData data = reportService.aggregateBudgetData(
                startDate, endDate, authentication.getName());
        return ResponseEntity.ok(data);
    }

    // Aggregate approval data
    @GetMapping("/data/approvals")
    @Operation(summary = "Aggregate approval data", description = "Get aggregated approval data for a date range")
    public ResponseEntity<ReportResponse.ReportData> aggregateApprovalData(
            @RequestParam @Parameter(description = "Start date") LocalDate startDate,
            @RequestParam @Parameter(description = "End date") LocalDate endDate,
            Authentication authentication) {
        
        logger.info("Aggregating approval data from {} to {} for user: {}", 
                startDate, endDate, authentication.getName());
        
        ReportResponse.ReportData data = reportService.aggregateApprovalData(
                startDate, endDate, authentication.getName());
        return ResponseEntity.ok(data);
    }

    // Get supported formats
    @GetMapping("/formats")
    @Operation(summary = "Get supported formats", description = "Get list of supported report formats")
    public ResponseEntity<List<String>> getSupportedFormats() {
        List<String> formats = reportService.getSupportedFormats();
        return ResponseEntity.ok(formats);
    }

    // Get report file size
    @GetMapping("/{reportId}/size")
    @Operation(summary = "Get report file size", description = "Get the file size of a generated report")
    public ResponseEntity<Map<String, Long>> getReportFileSize(
            @PathVariable String reportId) {
        Long fileSize = reportService.getReportFileSize(reportId);
        return ResponseEntity.ok(Map.of("fileSizeBytes", fileSize));
    }

    // Execute scheduled reports (admin endpoint)
    @PostMapping("/scheduled/execute")
    @Operation(summary = "Execute scheduled reports", description = "Manually trigger execution of scheduled reports")
    public ResponseEntity<Map<String, String>> executeScheduledReports() {
        logger.info("Executing scheduled reports");
        
        reportService.executeScheduledReports();
        return ResponseEntity.ok(Map.of("message", "Scheduled reports execution initiated"));
    }

    // Cleanup expired reports (admin endpoint)
    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup expired reports", description = "Remove expired reports from storage")
    public ResponseEntity<Map<String, String>> cleanupExpiredReports() {
        logger.info("Cleaning up expired reports");
        
        reportService.cleanupExpiredReports();
        return ResponseEntity.ok(Map.of("message", "Expired reports cleanup completed"));
    }

    // Helper methods
    private MediaType getMediaTypeForFormat(String format) {
        switch (format.toUpperCase()) {
            case "PDF":
                return MediaType.APPLICATION_PDF;
            case "CSV":
                return MediaType.parseMediaType("text/csv");
            case "XLSX":
                return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "JSON":
                return MediaType.APPLICATION_JSON;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String generateFileName(String reportName, String format) {
        if (reportName == null || reportName.trim().isEmpty()) {
            reportName = "report";
        }
        String sanitizedName = reportName.replaceAll("[^a-zA-Z0-9\\-_\\s]", "")
                .replaceAll("\\s+", "_");
        if (sanitizedName.isEmpty()) {
            sanitizedName = "report";
        }
        return sanitizedName + "." + format.toLowerCase();
    }

    // Exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("Error in ReportController", e);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // FIXED: DTOs for request bodies
    @Data
    public static class CreateScheduledReportRequest {
        @NotBlank(message = "Report name is required")
        private String reportName;
        
        @NotBlank(message = "Report type is required")
        @Pattern(regexp = "^(EXPENSE|BUDGET|APPROVAL|CATEGORY|TEAM|USER|FINANCIAL|CUSTOM)$", 
                message = "Invalid report type")
        private String reportType;
        
        @NotNull(message = "Start date is required")
        private LocalDate startDate;
        
        @NotNull(message = "End date is required")
        private LocalDate endDate;
        
        @NotBlank(message = "Schedule frequency is required")
        @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|QUARTERLY)$", message = "Invalid frequency")
        private String scheduleFrequency;
        
        @NotEmpty(message = "Recipients are required")
        private List<String> recipients;
        
        @Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
        private String format = "PDF";
        
        private String description;
        
        // Getters and setters
        public String getReportName() { return reportName; }
        public void setReportName(String reportName) { this.reportName = reportName; }
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getScheduleFrequency() { return scheduleFrequency; }
        public void setScheduleFrequency(String scheduleFrequency) { this.scheduleFrequency = scheduleFrequency; }
        public List<String> getRecipients() { return recipients; }
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    @Data
    public static class EmailReportRequest {
        @NotEmpty(message = "Recipients are required")
        private List<String> recipients;
        
        private String subject;
        private String message;
        
        // Getters and setters
        public List<String> getRecipients() { return recipients; }
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @Data
    public static class ShareReportRequest {
        @NotEmpty(message = "Usernames are required")
        private List<String> usernames;
        
        private String message;
        
        // Getters and setters
        public List<String> getUsernames() { return usernames; }
        public void setUsernames(List<String> usernames) { this.usernames = usernames; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}