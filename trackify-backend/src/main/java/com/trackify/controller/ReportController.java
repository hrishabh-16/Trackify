package com.trackify.controller;

import com.trackify.dto.request.ReportRequest;
import com.trackify.dto.response.ReportResponse;
import com.trackify.service.ReportService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    // Generate expense report
    @PostMapping("/expense")
    public ResponseEntity<ReportResponse> generateExpenseReport(
            @Valid @RequestBody ReportRequest.ExpenseReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating expense report for user: {}", authentication.getName());
        
        ReportResponse report = reportService.generateExpenseReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Generate budget report
    @PostMapping("/budget")
    public ResponseEntity<ReportResponse> generateBudgetReport(
            @Valid @RequestBody ReportRequest.BudgetReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating budget report for user: {}", authentication.getName());
        
        ReportResponse report = reportService.generateBudgetReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Generate approval report
    @PostMapping("/approval")
    public ResponseEntity<ReportResponse> generateApprovalReport(
            @Valid @RequestBody ReportRequest.ApprovalReportRequest request,
            Authentication authentication) {
        
        logger.info("Generating approval report for user: {}", authentication.getName());
        
        ReportResponse report = reportService.generateApprovalReport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // Generate team report
    @PostMapping("/team")
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
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable String reportId,
            Authentication authentication) {
        
        logger.info("Retrieving report: {} for user: {}", reportId, authentication.getName());
        
        ReportResponse report = reportService.getReportById(reportId, authentication.getName());
        return ResponseEntity.ok(report);
    }

    // Get user reports
    @GetMapping
    public ResponseEntity<List<ReportResponse>> getUserReports(Authentication authentication) {
        logger.info("Retrieving reports for user: {}", authentication.getName());
        
        List<ReportResponse> reports = reportService.getUserReports(authentication.getName());
        return ResponseEntity.ok(reports);
    }

    // Get user reports with pagination
    @GetMapping("/paginated")
    public ResponseEntity<Page<ReportResponse>> getUserReportsPaginated(
            @PageableDefault(size = 10, sort = "generatedAt") Pageable pageable,
            Authentication authentication) {
        
        logger.info("Retrieving paginated reports for user: {}", authentication.getName());
        
        Page<ReportResponse> reports = reportService.getUserReports(authentication.getName(), pageable);
        return ResponseEntity.ok(reports);
    }

    // Download report
    @GetMapping("/{reportId}/download")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String reportId,
            Authentication authentication) {
        
        logger.info("Downloading report: {} for user: {}", reportId, authentication.getName());
        
        ReportResponse report = reportService.getReportById(reportId, authentication.getName());
        byte[] reportData = reportService.downloadReport(reportId, authentication.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaTypeForFormat(report.getFormat()));
        headers.setContentDispositionFormData("attachment", 
                generateFileName(report.getReportName(), report.getFormat()));
        headers.setContentLength(reportData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportData);
    }

    // Get report download URL
    @GetMapping("/{reportId}/download-url")
    public ResponseEntity<Map<String, String>> getReportDownloadUrl(
            @PathVariable String reportId,
            Authentication authentication) {
        
        String downloadUrl = reportService.getReportDownloadUrl(reportId, authentication.getName());
        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }

    // Delete report
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Map<String, String>> deleteReport(
            @PathVariable String reportId,
            Authentication authentication) {
        
        logger.info("Deleting report: {} by user: {}", reportId, authentication.getName());
        
        reportService.deleteReport(reportId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
    }

    // Create scheduled report
    @PostMapping("/scheduled")
    public ResponseEntity<ReportResponse.ScheduledReportInfo> createScheduledReport(
            @Valid @RequestBody ReportRequest.ScheduledReportRequest request,
            Authentication authentication) {
        
        logger.info("Creating scheduled report: {} for user: {}", 
                request.getReportName(), authentication.getName());
        
        ReportResponse.ScheduledReportInfo scheduleInfo = reportService.createScheduledReport(
                request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleInfo);
    }

    // Get scheduled reports
    @GetMapping("/scheduled")
    public ResponseEntity<List<ReportResponse.ScheduledReportInfo>> getScheduledReports(
            Authentication authentication) {
        
        logger.info("Retrieving scheduled reports for user: {}", authentication.getName());
        
        List<ReportResponse.ScheduledReportInfo> scheduledReports = 
                reportService.getScheduledReports(authentication.getName());
        return ResponseEntity.ok(scheduledReports);
    }

    // Update scheduled report
    @PutMapping("/scheduled/{scheduleId}")
    public ResponseEntity<ReportResponse.ScheduledReportInfo> updateScheduledReport(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ReportRequest.ScheduledReportRequest request,
            Authentication authentication) {
        
        logger.info("Updating scheduled report: {} for user: {}", 
                scheduleId, authentication.getName());
        
        ReportResponse.ScheduledReportInfo updatedSchedule = reportService.updateScheduledReport(
                scheduleId, request, authentication.getName());
        return ResponseEntity.ok(updatedSchedule);
    }

    // Delete scheduled report
    @DeleteMapping("/scheduled/{scheduleId}")
    public ResponseEntity<Map<String, String>> deleteScheduledReport(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        
        logger.info("Deleting scheduled report: {} by user: {}", 
                scheduleId, authentication.getName());
        
        reportService.deleteScheduledReport(scheduleId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Scheduled report deleted successfully"));
    }

    // Get report templates
    @GetMapping("/templates")
    public ResponseEntity<List<ReportRequest>> getReportTemplates() {
        logger.info("Retrieving report templates");
        
        List<ReportRequest> templates = reportService.getReportTemplates();
        return ResponseEntity.ok(templates);
    }

    // Get specific report template
    @GetMapping("/templates/{templateName}")
    public ResponseEntity<ReportRequest> getReportTemplate(@PathVariable String templateName) {
        logger.info("Retrieving report template: {}", templateName);
        
        ReportRequest template = reportService.getReportTemplate(templateName);
        return ResponseEntity.ok(template);
    }

    // Get report summary
    @PostMapping("/summary")
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
    public ResponseEntity<Map<String, String>> emailReport(
            @PathVariable String reportId,
            @RequestBody Map<String, List<String>> emailRequest,
            Authentication authentication) {
        
        List<String> recipients = emailRequest.get("recipients");
        logger.info("Emailing report: {} to {} recipients by user: {}", 
                reportId, recipients.size(), authentication.getName());
        
        reportService.emailReport(reportId, recipients, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Report emailed successfully"));
    }

    // Share report
    @PostMapping("/{reportId}/share")
    public ResponseEntity<Map<String, String>> shareReport(
            @PathVariable String reportId,
            @RequestBody Map<String, List<String>> shareRequest,
            Authentication authentication) {
        
        List<String> usernames = shareRequest.get("usernames");
        logger.info("Sharing report: {} with {} users by: {}", 
                reportId, usernames.size(), authentication.getName());
        
        reportService.shareReport(reportId, usernames, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Report shared successfully"));
    }

    // Aggregate expense data
    @GetMapping("/data/expenses")
    public ResponseEntity<ReportResponse.ReportData> aggregateExpenseData(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {
        
        logger.info("Aggregating expense data from {} to {} for user: {}", 
                startDate, endDate, authentication.getName());
        
        ReportResponse.ReportData data = reportService.aggregateExpenseData(
                startDate, endDate, authentication.getName());
        return ResponseEntity.ok(data);
    }

    // Aggregate budget data
    @GetMapping("/data/budgets")
    public ResponseEntity<ReportResponse.ReportData> aggregateBudgetData(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {
        
        logger.info("Aggregating budget data from {} to {} for user: {}", 
                startDate, endDate, authentication.getName());
        
        ReportResponse.ReportData data = reportService.aggregateBudgetData(
                startDate, endDate, authentication.getName());
        return ResponseEntity.ok(data);
    }

    // Aggregate approval data
    @GetMapping("/data/approvals")
    public ResponseEntity<ReportResponse.ReportData> aggregateApprovalData(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {
        
        logger.info("Aggregating approval data from {} to {} for user: {}", 
                startDate, endDate, authentication.getName());
        
        ReportResponse.ReportData data = reportService.aggregateApprovalData(
                startDate, endDate, authentication.getName());
        return ResponseEntity.ok(data);
    }

    // Get supported formats
    @GetMapping("/formats")
    public ResponseEntity<List<String>> getSupportedFormats() {
        List<String> formats = reportService.getSupportedFormats();
        return ResponseEntity.ok(formats);
    }

    // Get report file size
    @GetMapping("/{reportId}/size")
    public ResponseEntity<Map<String, Long>> getReportFileSize(@PathVariable String reportId) {
        Long fileSize = reportService.getReportFileSize(reportId);
        return ResponseEntity.ok(Map.of("fileSizeBytes", fileSize));
    }

    // Execute scheduled reports (admin endpoint)
    @PostMapping("/scheduled/execute")
    public ResponseEntity<Map<String, String>> executeScheduledReports() {
        logger.info("Executing scheduled reports");
        
        reportService.executeScheduledReports();
        return ResponseEntity.ok(Map.of("message", "Scheduled reports execution initiated"));
    }

    // Cleanup expired reports (admin endpoint)
    @PostMapping("/cleanup")
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
        String sanitizedName = reportName.replaceAll("[^a-zA-Z0-9\\-_\\s]", "")
                .replaceAll("\\s+", "_");
        return sanitizedName + "." + format.toLowerCase();
    }

    // Exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("Error in ReportController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}