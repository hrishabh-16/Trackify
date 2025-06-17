package com.trackify.service.impl;
import java.io.ByteArrayInputStream;

import com.trackify.dto.response.ReportResponse;
import com.trackify.entity.Notification;
import com.trackify.entity.User;
import com.trackify.repository.UserRepository;
import com.trackify.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
	
	private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private  JavaMailSender mailSender;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private  TemplateEngine templateEngine;
    
    @Value("${app.email.from-address:noreply@trackify.com}")
    private String fromAddress;
    
    @Value("${app.email.from-name:Trackify Support}")
    private String fromName;
    
    @Value("${app.email.base-url:http://localhost:4200}")
    private String baseUrl;
    
    @Override
    public void sendEmailVerification(String email, String token) {
        logger.info("Sending email verification to: {}", email);
        
        try {
            Context context = new Context();
            context.setVariable("verificationUrl", baseUrl + "/verify-email?token=" + token);
            context.setVariable("baseUrl", baseUrl);
            
            String htmlContent = templateEngine.process("email/welcome", context);
            
            sendHtmlEmail(email, "Verify Your Email - Trackify", htmlContent);
            
            logger.info("Email verification sent successfully to: {}", email);
        } catch (Exception e) {
        	logger.error("Failed to send email verification to: {}", email, e);
            throw new RuntimeException("Failed to send email verification", e);
        }
    }
    
    @Override
    public void sendEmailVerification(User user) {
        sendEmailVerification(user.getEmail(), user.getEmailVerificationToken());
    }
    
    @Override
    public void sendPasswordResetEmail(String email, String token) {
    	logger.info("Sending password reset email to: {}", email);
        
        try {
            Context context = new Context();
            context.setVariable("resetUrl", baseUrl + "/reset-password?token=" + token);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("expiryTime", "24 hours");
            
            String htmlContent = templateEngine.process("email/password-reset", context);
            
            sendHtmlEmail(email, "Reset Your Password - Trackify", htmlContent);
            
            logger.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
        	logger.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    @Override
    public void sendPasswordResetConfirmation(String email) {
    	logger.info("Sending password reset confirmation to: {}", email);
        
        String subject = "Password Reset Successful - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Your password has been successfully reset. If you did not make this change, " +
            "please contact our support team immediately.\n\n" +
            "Best regards,\n" +
            "Trackify Team\n\n" +
            "Time: %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendWelcomeEmail(User user) {
    	logger.info("Sending welcome email to: {}", user.getEmail());
        
        try {
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("fullName", user.getFullName());
            context.setVariable("email", user.getEmail());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("loginUrl", baseUrl + "/login");
            
            String htmlContent = templateEngine.process("email/welcome", context);
            
            sendHtmlEmail(user.getEmail(), "Welcome to Trackify!", htmlContent);
            
            logger.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
        	logger.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }
    
    @Override
    public void sendRegistrationConfirmation(User user) {
    	logger.info("Sending registration confirmation to: {}", user.getEmail());
        
        String subject = "Registration Successful - Trackify";
        String content = String.format(
            "Hello %s,\n\n" +
            "Welcome to Trackify! Your account has been successfully created.\n\n" +
            "Email: %s\n" +
            "Registration Date: %s\n\n" +
            "You can now start tracking your expenses and managing your finances.\n\n" +
            "Login here: %s/login\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            user.getFirstName(),
            user.getEmail(),
            user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            baseUrl
        );
        
        sendEmail(user.getEmail(), subject, content);
    }
    
    @Override
    public void sendExpenseApprovalNotification(String email, String expenseName, String approverName) {
    	logger.info("Sending expense approval notification to: {}", email);
        
        String subject = "Expense Approved - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Your expense '%s' has been approved by %s.\n\n" +
            "You can view the details in your dashboard: %s/dashboard\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            expenseName,
            approverName,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendExpenseRejectionNotification(String email, String expenseName, String reason) {
    	logger.info("Sending expense rejection notification to: {}", email);
        
        String subject = "Expense Rejected - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Your expense '%s' has been rejected.\n\n" +
            "Reason: %s\n\n" +
            "Please review and resubmit if necessary: %s/expenses\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            expenseName,
            reason,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendExpenseSubmissionNotification(String email, String expenseName, String submitterName) {
    	logger.info("Sending expense submission notification to: {}", email);
        
        String subject = "New Expense Submitted for Approval - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "A new expense '%s' has been submitted by %s and requires your approval.\n\n" +
            "Please review: %s/approvals\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            expenseName,
            submitterName,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendBudgetAlertEmail(String email, String budgetName, double usedPercentage) {
    	logger.info("Sending budget alert email to: {}", email);
        
        String subject = "Budget Alert - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Your budget '%s' has reached %.1f%% of its limit.\n\n" +
            "Please review your spending: %s/budgets\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            budgetName,
            usedPercentage,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendBudgetExceededEmail(String email, String budgetName, double amount) {
    	logger.info("Sending budget exceeded email to: {}", email);
        
        String subject = "Budget Exceeded - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Your budget '%s' has been exceeded by $%.2f.\n\n" +
            "Please review your expenses: %s/budgets\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            budgetName,
            amount,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendTeamInvitationEmail(String email, String teamName, String inviterName, String token) {
    	logger.info("Sending team invitation email to: {}", email);
        
        try {
            Context context = new Context();
            context.setVariable("teamName", teamName);
            context.setVariable("inviterName", inviterName);
            context.setVariable("invitationUrl", baseUrl + "/team/join?token=" + token);
            context.setVariable("baseUrl", baseUrl);
            
            String htmlContent = templateEngine.process("email/team-invitation", context);
            
            sendHtmlEmail(email, "Team Invitation - " + teamName, htmlContent);
            
            logger.info("Team invitation email sent successfully to: {}", email);
        } catch (Exception e) {
        	logger.error("Failed to send team invitation email to: {}", email, e);
        }
    }
    
    @Override
    public void sendTeamJoinNotification(String email, String teamName, String newMemberName) {
    	logger.info("Sending team join notification to: {}", email);
        
        String subject = "New Team Member Joined - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "%s has joined your team '%s'.\n\n" +
            "View team members: %s/teams\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            newMemberName,
            teamName,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendWeeklyReportEmail(String email, String reportContent) {
    	logger.info("Sending weekly report email to: {}", email);
        
        String subject = "Weekly Expense Report - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Here's your weekly expense report:\n\n" +
            "%s\n\n" +
            "View detailed report: %s/reports\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            reportContent,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendMonthlyReportEmail(String email, String reportContent) {
    	logger.info("Sending monthly report email to: {}", email);
        
        String subject = "Monthly Expense Report - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Here's your monthly expense report:\n\n" +
            "%s\n\n" +
            "View detailed report: %s/reports\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            reportContent,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendAccountStatusChangeEmail(String email, boolean isEnabled) {
    	logger.info("Sending account status change email to: {}", email);
        
        String subject = "Account Status Changed - Trackify";
        String status = isEnabled ? "enabled" : "disabled";
        String content = String.format(
            "Hello,\n\n" +
            "Your account has been %s.\n\n" +
            "%s\n\n" +
            "If you have any questions, please contact our support team.\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            status,
            isEnabled ? "You can now access your account normally." : "Please contact support if you believe this is an error."
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendRoleChangeNotification(String email, String oldRole, String newRole) {
    	logger.info("Sending role change notification to: {}", email);
        
        String subject = "Role Changed - Trackify";
        String content = String.format(
            "Hello,\n\n" +
            "Your role has been changed from %s to %s.\n\n" +
            "Your new permissions are now active.\n\n" +
            "Login to see changes: %s/login\n\n" +
            "Best regards,\n" +
            "Trackify Team",
            oldRole,
            newRole,
            baseUrl
        );
        
        sendEmail(email, subject, content);
    }
    
    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
        	logger.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
        	logger.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        } catch (Exception e) {
        	logger.error("Unexpected error sending HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
    
    @Override
    public void sendEmailWithAttachment(String to, String subject, String content, String attachmentPath, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);
            
            FileSystemResource file = new FileSystemResource(new File(attachmentPath));
            helper.addAttachment(attachmentName, file);
            
            mailSender.send(message);
            logger.info("Email with attachment sent successfully to: {}", to);
        } catch (MessagingException e) {
        	logger.error("Failed to send email with attachment to: {}", to, e);
            throw new RuntimeException("Failed to send email with attachment", e);
        } catch (Exception e) {
        	logger.error("Unexpected error sending email with attachment to: {}", to, e);
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }

    @Override
    public void sendReportEmail(String email, ReportResponse report, byte[] reportFile) {
        logger.info("Sending report email to: {}", email);
        
        try {
            Context context = new Context();
            context.setVariable("reportName", report.getReportName());
            context.setVariable("reportType", report.getReportType());
            context.setVariable("generatedAt", report.getGeneratedAt());
            context.setVariable("generatedBy", report.getGeneratedBy());
            context.setVariable("baseUrl", baseUrl);
            
            String subject = String.format("%s Report - %s", 
                report.getReportType(), 
                report.getReportName());
            
            String htmlContent = templateEngine.process("email/report", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Attach the report file
            String fileName = String.format("%s-%s.%s", 
                report.getReportName().replaceAll("\\s+", "-").toLowerCase(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")),
                report.getFormat().toLowerCase());
            
            helper.addAttachment(fileName, () -> new ByteArrayInputStream(reportFile));
            
            mailSender.send(message);
            logger.info("Report email sent successfully to: {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to send report email to: {}", email, e);
            throw new RuntimeException("Failed to send report email", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending report email to: {}", email, e);
            throw new RuntimeException("Failed to send report email", e);
        }
    }

    @Override
    public void sendNotificationEmail(Notification notification) {
        logger.info("Sending notification email for notification ID: {}", notification.getId());
        
        try {
            // Get user details
            User user = userRepository.findById(notification.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + notification.getUserId()));
            
            // Create email content based on notification type
            String subject = createEmailSubject(notification);
            String htmlContent = createNotificationEmailContent(notification, user);
            
            // Send HTML email
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            
            logger.info("Notification email sent successfully to: {} for notification: {}", 
                       user.getEmail(), notification.getId());
            
        } catch (Exception e) {
            logger.error("Failed to send notification email for notification ID: {}", 
                        notification.getId(), e);
            throw new RuntimeException("Failed to send notification email", e);
        }
    }

    private String createEmailSubject(Notification notification) {
        String baseSubject = notification.getTitle();
        
        // Add prefix based on priority
        if ("URGENT".equals(notification.getPriority())) {
            return "[URGENT] " + baseSubject + " - Trackify";
        } else if ("HIGH".equals(notification.getPriority())) {
            return "[HIGH] " + baseSubject + " - Trackify";
        } else {
            return baseSubject + " - Trackify";
        }
    }

    private String createNotificationEmailContent(Notification notification, User user) {
        try {
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("fullName", user.getFullName());
            context.setVariable("notificationTitle", notification.getTitle());
            context.setVariable("notificationMessage", notification.getMessage());
            context.setVariable("notificationType", notification.getType().name());
            context.setVariable("priority", notification.getPriority());
            context.setVariable("category", notification.getCategory());
            context.setVariable("createdAt", notification.getCreatedAt());
            context.setVariable("baseUrl", baseUrl);
            
            // Add action button if available
            if (notification.getActionUrl() != null && notification.getActionText() != null) {
                context.setVariable("hasAction", true);
                context.setVariable("actionUrl", baseUrl + notification.getActionUrl());
                context.setVariable("actionText", notification.getActionText());
            } else {
                context.setVariable("hasAction", false);
            }
            
            // Add entity-specific information
            if (notification.getRelatedEntityType() != null && notification.getRelatedEntityId() != null) {
                context.setVariable("hasEntity", true);
                context.setVariable("entityType", notification.getRelatedEntityType());
                context.setVariable("entityId", notification.getRelatedEntityId());
                context.setVariable("entityUrl", baseUrl + "/" + 
                    notification.getRelatedEntityType().toLowerCase() + "s/" + 
                    notification.getRelatedEntityId());
            } else {
                context.setVariable("hasEntity", false);
            }
            
            // Use a generic notification template or create content directly
            return templateEngine.process("email/notification", context);
            
        } catch (Exception e) {
            logger.warn("Failed to process notification email template, using fallback content", e);
            return createFallbackEmailContent(notification, user);
        }
    }

    private String createFallbackEmailContent(Notification notification, User user) {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html><head><title>").append(notification.getTitle()).append("</title></head>");
        content.append("<body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>");
        
        // Header
        content.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px;'>");
        content.append("<h1 style='color: #333; margin: 0;'>").append(notification.getTitle()).append("</h1>");
        content.append("</div>");
        
        // Greeting
        content.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        
        // Message
        content.append("<div style='background-color: #ffffff; padding: 20px; border-left: 4px solid #007bff; margin: 20px 0;'>");
        content.append("<p style='margin: 0; font-size: 16px; line-height: 1.5;'>");
        content.append(notification.getMessage());
        content.append("</p>");
        content.append("</div>");
        
        // Priority indicator
        if ("URGENT".equals(notification.getPriority()) || "HIGH".equals(notification.getPriority())) {
            String priorityColor = "URGENT".equals(notification.getPriority()) ? "#dc3545" : "#fd7e14";
            content.append("<div style='background-color: ").append(priorityColor);
            content.append("; color: white; padding: 10px; border-radius: 4px; margin: 15px 0; text-align: center;'>");
            content.append("<strong>Priority: ").append(notification.getPriority()).append("</strong>");
            content.append("</div>");
        }
        
        // Action button
        if (notification.getActionUrl() != null && notification.getActionText() != null) {
            content.append("<div style='text-align: center; margin: 30px 0;'>");
            content.append("<a href='").append(baseUrl).append(notification.getActionUrl());
            content.append("' style='background-color: #007bff; color: white; padding: 12px 24px; ");
            content.append("text-decoration: none; border-radius: 4px; display: inline-block; font-weight: bold;'>");
            content.append(notification.getActionText());
            content.append("</a>");
            content.append("</div>");
        }
        
        // Footer
        content.append("<hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>");
        content.append("<p style='color: #666; font-size: 14px;'>");
        content.append("This notification was sent on ");
        content.append(notification.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        content.append("<br>");
        content.append("You can view all notifications in your <a href='").append(baseUrl);
        content.append("/notifications'>Trackify dashboard</a>.");
        content.append("</p>");
        
        content.append("<p style='color: #666; font-size: 12px;'>");
        content.append("Best regards,<br>Trackify Team");
        content.append("</p>");
        
        content.append("</body></html>");
        
        return content.toString();
    }
    
    
    @Override
    public void sendScheduledReportEmail(String email, Object scheduledReportConfig, ReportResponse report, byte[] reportFile) {
        logger.info("Sending scheduled report email to: {}", email);
        
        try {
            // Extract config details using reflection or casting
            String reportName = extractReportName(scheduledReportConfig);
            String frequency = extractFrequency(scheduledReportConfig);
            
            Context context = new Context();
            context.setVariable("reportName", reportName);
            context.setVariable("frequency", frequency);
            context.setVariable("reportType", report.getReportType());
            context.setVariable("generatedAt", report.getGeneratedAt());
            context.setVariable("startDate", report.getStartDate());
            context.setVariable("endDate", report.getEndDate());
            context.setVariable("baseUrl", baseUrl);
            
            String subject = String.format("Scheduled %s Report - %s", frequency, reportName);
            
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/scheduled-report", context);
            } catch (Exception e) {
                logger.warn("Failed to process scheduled report template, using fallback", e);
                htmlContent = createScheduledReportFallbackContent(reportName, frequency, report);
            }
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Attach the report file
            String fileName = String.format("%s-%s-%s.%s", 
                reportName.replaceAll("\\s+", "-").toLowerCase(),
                frequency.toLowerCase(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                report.getFormat().toLowerCase());
            
            helper.addAttachment(fileName, () -> new ByteArrayInputStream(reportFile));
            
            mailSender.send(message);
            logger.info("Scheduled report email sent successfully to: {}", email);
            
        } catch (Exception e) {
            logger.error("Failed to send scheduled report email to: {}", email, e);
            throw new RuntimeException("Failed to send scheduled report email", e);
        }
    }

    @Override
    public void sendScheduledReportFailureNotification(Object scheduledReportConfig, Exception error) {
        try {
            String reportName = extractReportName(scheduledReportConfig);
            String createdBy = extractCreatedBy(scheduledReportConfig);
            String frequency = extractFrequency(scheduledReportConfig);
            
            // Get user email
            User user = userRepository.findByUsername(createdBy)
                    .orElseThrow(() -> new RuntimeException("User not found: " + createdBy));
            
            logger.info("Sending scheduled report failure notification to: {}", user.getEmail());
            
            String subject = "Scheduled Report Failed - " + reportName;
            
            String content = String.format(
                "Hello %s,\n\n" +
                "Your scheduled %s report '%s' has failed to generate after multiple attempts.\n\n" +
                "Error: %s\n\n" +
                "The report has been temporarily disabled. Please check your report configuration " +
                "and re-enable it if needed.\n\n" +
                "View your scheduled reports: %s/reports/scheduled\n\n" +
                "If this issue persists, please contact our support team.\n\n" +
                "Best regards,\n" +
                "Trackify Team",
                user.getFirstName(),
                frequency,
                reportName,
                error.getMessage(),
                baseUrl
            );
            
            sendEmail(user.getEmail(), subject, content);
            
            logger.info("Scheduled report failure notification sent to: {}", user.getEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send scheduled report failure notification", e);
        }
    }

    private String extractReportName(Object config) {
        try {
            return (String) config.getClass().getMethod("getReportName").invoke(config);
        } catch (Exception e) {
            return "Unknown Report";
        }
    }

    private String extractFrequency(Object config) {
        try {
            return (String) config.getClass().getMethod("getFrequency").invoke(config);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String extractCreatedBy(Object config) {
        try {
            return (String) config.getClass().getMethod("getCreatedBy").invoke(config);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String createScheduledReportFallbackContent(String reportName, String frequency, ReportResponse report) {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html><head><title>Scheduled Report - ").append(reportName).append("</title></head>");
        content.append("<body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>");
        
        content.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px;'>");
        content.append("<h1 style='color: #333; margin: 0;'>Your ").append(frequency).append(" Report is Ready</h1>");
        content.append("</div>");
        
        content.append("<p>Hello,</p>");
        content.append("<p>Your scheduled ").append(frequency.toLowerCase()).append(" report '<strong>");
        content.append(reportName).append("</strong>' has been generated and is attached to this email.</p>");
        
        content.append("<div style='background-color: #ffffff; padding: 20px; border: 1px solid #dee2e6; border-radius: 4px; margin: 20px 0;'>");
        content.append("<h3 style='margin-top: 0;'>Report Details:</h3>");
        content.append("<ul>");
        content.append("<li><strong>Report Type:</strong> ").append(report.getReportType()).append("</li>");
        content.append("<li><strong>Period:</strong> ").append(report.getStartDate()).append(" to ").append(report.getEndDate()).append("</li>");
        content.append("<li><strong>Generated:</strong> ").append(report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</li>");
        content.append("<li><strong>Format:</strong> ").append(report.getFormat()).append("</li>");
        content.append("</ul>");
        content.append("</div>");
        
        content.append("<div style='text-align: center; margin: 30px 0;'>");
        content.append("<a href='").append(baseUrl).append("/reports' ");
        content.append("style='background-color: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;'>");
        content.append("View All Reports");
        content.append("</a>");
        content.append("</div>");
        
        content.append("<p style='color: #666; font-size: 14px;'>Best regards,<br>Trackify Team</p>");
        content.append("</body></html>");
        
        return content.toString();
    }
 }