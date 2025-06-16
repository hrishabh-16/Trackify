package com.trackify.service;

import com.trackify.dto.request.ReportRequest;
import com.trackify.dto.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    
    // Report generation
    ReportResponse generateExpenseReport(ReportRequest.ExpenseReportRequest request, String username);
    ReportResponse generateBudgetReport(ReportRequest.BudgetReportRequest request, String username);
    ReportResponse generateApprovalReport(ReportRequest.ApprovalReportRequest request, String username);
    ReportResponse generateTeamReport(ReportRequest.TeamReportRequest request, String username);
    ReportResponse generateCustomReport(ReportRequest request, String username);
    
    // Report management
    ReportResponse getReportById(String reportId, String username);
    List<ReportResponse> getUserReports(String username);
    Page<ReportResponse> getUserReports(String username, Pageable pageable);
    void deleteReport(String reportId, String username);
    
    // Report download
    byte[] downloadReport(String reportId, String username);
    String getReportDownloadUrl(String reportId, String username);
    
    // Scheduled reports
    ReportResponse.ScheduledReportInfo createScheduledReport(ReportRequest.ScheduledReportRequest request, String username);
    List<ReportResponse.ScheduledReportInfo> getScheduledReports(String username);
    ReportResponse.ScheduledReportInfo updateScheduledReport(Long scheduleId, ReportRequest.ScheduledReportRequest request, String username);
    void deleteScheduledReport(Long scheduleId, String username);
    void executeScheduledReports();
    
    // Report templates
    List<ReportRequest> getReportTemplates();
    ReportRequest getReportTemplate(String templateName);
    
    // Report analytics
    ReportResponse.ReportSummary getReportSummary(ReportRequest request, String username);
    List<ReportResponse.ReportChart> getReportCharts(ReportRequest request, String username);
    
    // Export and sharing
    void emailReport(String reportId, List<String> recipients, String username);
    void shareReport(String reportId, List<String> usernames, String sharedBy);
    
    // Report data aggregation
    ReportResponse.ReportData aggregateExpenseData(LocalDate startDate, LocalDate endDate, String username);
    ReportResponse.ReportData aggregateBudgetData(LocalDate startDate, LocalDate endDate, String username);
    ReportResponse.ReportData aggregateApprovalData(LocalDate startDate, LocalDate endDate, String username);
    
    // Utility methods
    boolean validateReportAccess(String reportId, String username);
    void cleanupExpiredReports();
    List<String> getSupportedFormats();
    Long getReportFileSize(String reportId);
}