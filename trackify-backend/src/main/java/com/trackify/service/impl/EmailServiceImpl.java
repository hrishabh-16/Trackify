package com.trackify.service.impl;

import com.trackify.entity.User;
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
 }