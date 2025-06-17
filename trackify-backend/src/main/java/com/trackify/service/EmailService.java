package com.trackify.service;

import com.trackify.dto.response.ReportResponse;
import com.trackify.entity.User;
import com.trackify.entity.Notification;

public interface EmailService {
    
    // Email verification
    void sendEmailVerification(String email, String token);
    void sendEmailVerification(User user);
    
    // Password reset
    void sendPasswordResetEmail(String email, String token);
    void sendPasswordResetConfirmation(String email);
    
    // Welcome emails
    void sendWelcomeEmail(User user);
    void sendRegistrationConfirmation(User user);
    
    // Expense related emails
    void sendExpenseApprovalNotification(String email, String expenseName, String approverName);
    void sendExpenseRejectionNotification(String email, String expenseName, String reason);
    void sendExpenseSubmissionNotification(String email, String expenseName, String submitterName);
    
    // Budget alerts
    void sendBudgetAlertEmail(String email, String budgetName, double usedPercentage);
    void sendBudgetExceededEmail(String email, String budgetName, double amount);
    
    // Team notifications
    void sendTeamInvitationEmail(String email, String teamName, String inviterName, String token);
    void sendTeamJoinNotification(String email, String teamName, String newMemberName);
    
    // Reports
    void sendWeeklyReportEmail(String email, String reportContent);
    void sendMonthlyReportEmail(String email, String reportContent);
    void sendReportEmail(String email, ReportResponse report, byte[] reportFile);
    
    // Account notifications
    void sendAccountStatusChangeEmail(String email, boolean isEnabled);
    void sendRoleChangeNotification(String email, String oldRole, String newRole);
    
    // Notification email - ADDED METHOD TO FIX ERROR
    void sendNotificationEmail(Notification notification);
    
    // Generic email method
    void sendEmail(String to, String subject, String content);
    void sendHtmlEmail(String to, String subject, String htmlContent);
    void sendEmailWithAttachment(String to, String subject, String content, String attachmentPath, String attachmentName);
    
    // Scheduled report emails
    void sendScheduledReportEmail(String email, Object scheduledReportConfig, ReportResponse report, byte[] reportFile);
    void sendScheduledReportFailureNotification(Object scheduledReportConfig, Exception error);
}