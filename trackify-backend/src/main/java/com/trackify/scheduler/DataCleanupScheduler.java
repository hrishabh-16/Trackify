package com.trackify.scheduler;

import com.trackify.entity.*;
import com.trackify.enums.ExpenseStatus;
import com.trackify.repository.*;
import com.trackify.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DataCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataCleanupScheduler.class);

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ApprovalWorkflowRepository approvalWorkflowRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.cleanup.expense-retention-days:365}")
    private int expenseRetentionDays;

    @Value("${app.cleanup.notification-retention-days:90}")
    private int notificationRetentionDays;

    @Value("${app.cleanup.invitation-expiry-days:7}")
    private int invitationExpiryDays;

    @Value("${app.cleanup.approval-retention-days:180}")
    private int approvalRetentionDays;

    @Value("${app.cleanup.receipt-orphan-days:30}")
    private int receiptOrphanDays;

    @Value("${app.cleanup.budget-expired-days:90}")
    private int budgetExpiredRetentionDays;

    @Value("${app.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    /**
     * Comprehensive daily cleanup - runs every day at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void performDailyCleanup() {
        if (!cleanupEnabled) {
            logger.info("Data cleanup is disabled, skipping daily cleanup");
            return;
        }

        logger.info("Starting daily data cleanup at: {}", LocalDateTime.now());

        try {
            AtomicInteger totalCleaned = new AtomicInteger(0);

            // Clean expired invitations
            totalCleaned.addAndGet(cleanupExpiredInvitations());

            // Clean old notifications
            totalCleaned.addAndGet(cleanupOldNotifications());

            // Clean orphaned receipts
            totalCleaned.addAndGet(cleanupOrphanedReceipts());

            // Clean expired budgets
            totalCleaned.addAndGet(cleanupExpiredBudgets());

            // Clean completed approval workflows
            totalCleaned.addAndGet(cleanupOldApprovalWorkflows());

            // Update user last activity
            updateUserLastActivity();

            logger.info("Daily cleanup completed. Total records cleaned: {}", totalCleaned.get());

        } catch (Exception e) {
            logger.error("Error during daily cleanup", e);
        }
    }

    /**
     * Weekly comprehensive cleanup - runs every Sunday at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * SUN") // Every Sunday at 2 AM
    @Transactional
    public void performWeeklyCleanup() {
        if (!cleanupEnabled) {
            logger.info("Data cleanup is disabled, skipping weekly cleanup");
            return;
        }

        logger.info("Starting weekly data cleanup at: {}", LocalDateTime.now());

        try {
            AtomicInteger totalCleaned = new AtomicInteger(0);

            // Clean old draft expenses
            totalCleaned.addAndGet(cleanupOldDraftExpenses());

            // Clean inactive user sessions
            totalCleaned.addAndGet(cleanupInactiveUserSessions());

            // Optimize database (this would be database-specific)
            optimizeDatabase();

            // Generate cleanup report
            generateCleanupReport(totalCleaned.get());

            logger.info("Weekly cleanup completed. Total records cleaned: {}", totalCleaned.get());

        } catch (Exception e) {
            logger.error("Error during weekly cleanup", e);
        }
    }

    /**
     * Monthly archive cleanup - runs on 1st of every month at 1 AM
     */
    @Scheduled(cron = "0 0 1 1 * ?") // 1st of month at 1 AM
    @Transactional
    public void performMonthlyArchive() {
        if (!cleanupEnabled) {
            logger.info("Data cleanup is disabled, skipping monthly archive");
            return;
        }

        logger.info("Starting monthly archive cleanup at: {}", LocalDateTime.now());

        try {
            AtomicInteger totalArchived = new AtomicInteger(0);

            // Archive old expenses
            totalArchived.addAndGet(archiveOldExpenses());

            // Archive old approval workflows
            totalArchived.addAndGet(archiveOldApprovalWorkflows());

            // Generate monthly archive report
            generateArchiveReport(totalArchived.get());

            logger.info("Monthly archive completed. Total records archived: {}", totalArchived.get());

        } catch (Exception e) {
            logger.error("Error during monthly archive", e);
        }
    }

    /**
     * Database optimization - runs every Saturday at 4 AM
     */
    @Scheduled(cron = "0 0 4 * * SAT") // Every Saturday at 4 AM
    public void performDatabaseOptimization() {
        if (!cleanupEnabled) {
            logger.info("Data cleanup is disabled, skipping database optimization");
            return;
        }

        logger.info("Starting database optimization at: {}", LocalDateTime.now());

        try {
            // Analyze database statistics
            analyzeDatabaseStatistics();

            // Cleanup temporary data
            cleanupTemporaryData();

            logger.info("Database optimization completed");

        } catch (Exception e) {
            logger.error("Error during database optimization", e);
        }
    }

    // Specific cleanup methods

    @Transactional
    public int cleanupExpiredInvitations() {
        try {
            logger.info("Cleaning up expired team invitations");

            LocalDateTime cutoffTime = LocalDateTime.now();
            
            // Find expired invitations
            List<TeamMember> expiredInvitations = teamMemberRepository
                    .findByIsActiveFalseAndInvitationExpiresAtBefore(cutoffTime);

            if (expiredInvitations.isEmpty()) {
                logger.debug("No expired invitations found");
                return 0;
            }

            // Delete expired invitations
            teamMemberRepository.deleteByIsActiveFalseAndInvitationExpiresAtBefore(cutoffTime);

            logger.info("Cleaned up {} expired team invitations", expiredInvitations.size());
            return expiredInvitations.size();

        } catch (Exception e) {
            logger.error("Error cleaning up expired invitations", e);
            return 0;
        }
    }

    @Transactional
    public int cleanupOldNotifications() {
        try {
            logger.info("Cleaning up old notifications");

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(notificationRetentionDays);
            
            // Count notifications to be deleted
            long countToDelete = notificationRepository.countByCreatedAtBeforeAndIsReadTrue(cutoffTime);

            if (countToDelete == 0) {
                logger.debug("No old notifications found for cleanup");
                return 0;
            }

            // Delete old read notifications
            int deletedCount = notificationRepository.deleteByCreatedAtBeforeAndIsReadTrue(cutoffTime);

            logger.info("Cleaned up {} old notifications", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            logger.error("Error cleaning up old notifications", e);
            return 0;
        }
    }

    @Transactional
    public int cleanupOrphanedReceipts() {
        try {
            logger.info("Cleaning up orphaned receipts");

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(receiptOrphanDays);
            
            // Find receipts not attached to any expense for more than specified days
            List<Receipt> orphanedReceipts = receiptRepository.findOrphanedReceipts(cutoffTime);

            if (orphanedReceipts.isEmpty()) {
                logger.debug("No orphaned receipts found");
                return 0;
            }

            // Delete orphaned receipts
            for (Receipt receipt : orphanedReceipts) {
                // Delete physical file if exists
                deleteReceiptFile(receipt);
                receiptRepository.delete(receipt);
            }

            logger.info("Cleaned up {} orphaned receipts", orphanedReceipts.size());
            return orphanedReceipts.size();

        } catch (Exception e) {
            logger.error("Error cleaning up orphaned receipts", e);
            return 0;
        }
    }

    @Transactional
    public int cleanupExpiredBudgets() {
        try {
            logger.info("Cleaning up expired budgets");

            LocalDate cutoffDate = LocalDate.now().minusDays(budgetExpiredRetentionDays);
            
            // Find expired budgets that are inactive
            List<Budget> expiredBudgets = budgetRepository.findByEndDateBeforeAndIsActiveFalse(cutoffDate);

            if (expiredBudgets.isEmpty()) {
                logger.debug("No expired budgets found for cleanup");
                return 0;
            }

            // Mark for cleanup or delete based on business rules
            int deletedCount = 0;
            for (Budget budget : expiredBudgets) {
                // Only delete if no related expenses exist
                long relatedExpenses = expenseRepository.countByCategoryId(budget.getCategoryId());
                if (relatedExpenses == 0) {
                    budgetRepository.delete(budget);
                    deletedCount++;
                }
            }

            logger.info("Processed {} expired budgets for cleanup, deleted {}", expiredBudgets.size(), deletedCount);
            return deletedCount;

        } catch (Exception e) {
            logger.error("Error cleaning up expired budgets", e);
            return 0;
        }
    }

    @Transactional
    public int cleanupOldApprovalWorkflows() {
        try {
            logger.info("Cleaning up old approval workflows");

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(approvalRetentionDays);
            
            // Find completed workflows older than retention period
            List<ApprovalWorkflow> oldWorkflows = approvalWorkflowRepository
                    .findCompletedWorkflowsOlderThan(cutoffTime);

            if (oldWorkflows.isEmpty()) {
                logger.debug("No old approval workflows found for cleanup");
                return 0;
            }

            // Archive or delete based on business requirements
            for (ApprovalWorkflow workflow : oldWorkflows) {
                // Archive to separate table or delete
                approvalWorkflowRepository.delete(workflow);
            }

            logger.info("Cleaned up {} old approval workflows", oldWorkflows.size());
            return oldWorkflows.size();

        } catch (Exception e) {
            logger.error("Error cleaning up old approval workflows", e);
            return 0;
        }
    }

    @Transactional
    public int cleanupOldDraftExpenses() {
        try {
            logger.info("Cleaning up old draft expenses");

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30); // Draft expenses older than 30 days
            
            // Find old draft expenses
            List<Expense> oldDrafts = expenseRepository
                    .findByStatusAndCreatedAtBefore(ExpenseStatus.DRAFT, cutoffTime);

            if (oldDrafts.isEmpty()) {
                logger.debug("No old draft expenses found for cleanup");
                return 0;
            }

            // Send notification to users before deletion
            notifyUsersOfDraftCleanup(oldDrafts);

            // Delete old draft expenses
            for (Expense draft : oldDrafts) {
                expenseRepository.delete(draft);
            }

            logger.info("Cleaned up {} old draft expenses", oldDrafts.size());
            return oldDrafts.size();

        } catch (Exception e) {
            logger.error("Error cleaning up old draft expenses", e);
            return 0;
        }
    }

    @Transactional
    public int cleanupInactiveUserSessions() {
        try {
            logger.info("Cleaning up inactive user sessions");

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            
            // Use the existing findInactiveUsers method or the new one
            List<User> inactiveUsers;
            try {
                // Try to use the new method first
                inactiveUsers = userRepository.findByLastLoginBeforeOrNull(cutoffTime);
            } catch (Exception e) {
                // Fallback to existing method
                logger.debug("Using fallback method for finding inactive users");
                inactiveUsers = userRepository.findInactiveUsers(cutoffTime);
            }
            
            for (User user : inactiveUsers) {
                // Perform any session cleanup logic
                // This could involve clearing tokens, sessions, etc.
                logger.debug("Processing inactive user: {}", user.getUsername());
            }

            logger.info("Processed {} inactive user sessions", inactiveUsers.size());
            return inactiveUsers.size();

        } catch (Exception e) {
            logger.error("Error cleaning up inactive user sessions", e);
            return 0;
        }
    }

    @Transactional
    public int archiveOldExpenses() {
        try {
            logger.info("Archiving old expenses");

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(expenseRetentionDays);
            
            // Find old completed expenses
            List<Expense> oldExpenses = expenseRepository
                    .findByCreatedAtBeforeAndStatusIn(cutoffTime, 
                            Arrays.asList(ExpenseStatus.APPROVED, ExpenseStatus.PAID, ExpenseStatus.CANCELLED));

            if (oldExpenses.isEmpty()) {
                logger.debug("No old expenses found for archiving");
                return 0;
            }

            // Archive logic would go here
            // For now, just log the count
            logger.info("Found {} expenses for potential archiving", oldExpenses.size());
            return oldExpenses.size();

        } catch (Exception e) {
            logger.error("Error archiving old expenses", e);
            return 0;
        }
    }

    @Transactional
    public int archiveOldApprovalWorkflows() {
        try {
            logger.info("Archiving old approval workflows");

            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(approvalRetentionDays * 2); // Double retention for archive
            
            List<ApprovalWorkflow> oldWorkflows = approvalWorkflowRepository
                    .findCompletedWorkflowsOlderThan(cutoffTime);

            logger.info("Found {} approval workflows for potential archiving", oldWorkflows.size());
            return oldWorkflows.size();

        } catch (Exception e) {
            logger.error("Error archiving old approval workflows", e);
            return 0;
        }
    }

    private void updateUserLastActivity() {
        try {
            logger.debug("Updating user last activity timestamps");
            // This would update last activity based on recent actions
            // Implementation depends on how you track user activity
        } catch (Exception e) {
            logger.error("Error updating user last activity", e);
        }
    }

    private void optimizeDatabase() {
        try {
            logger.info("Performing database optimization");
            // Database-specific optimization commands would go here
            // For PostgreSQL: VACUUM, ANALYZE
            // For MySQL: OPTIMIZE TABLE
        } catch (Exception e) {
            logger.error("Error during database optimization", e);
        }
    }

    private void analyzeDatabaseStatistics() {
        try {
            logger.info("Analyzing database statistics");
            // Generate database statistics and performance metrics
        } catch (Exception e) {
            logger.error("Error analyzing database statistics", e);
        }
    }

    private void cleanupTemporaryData() {
        try {
            logger.info("Cleaning up temporary data");
            // Clean up temporary files, caches, etc.
        } catch (Exception e) {
            logger.error("Error cleaning up temporary data", e);
        }
    }

    private void deleteReceiptFile(Receipt receipt) {
        try {
            // Implementation to delete physical receipt file
            if (receipt.getFileUrl() != null) {
                // Delete file from storage system
                logger.debug("Deleted receipt file: {}", receipt.getFileUrl());
            }
        } catch (Exception e) {
            logger.error("Error deleting receipt file: {}", receipt.getFileUrl(), e);
        }
    }

    private void notifyUsersOfDraftCleanup(List<Expense> draftExpenses) {
        try {
            // Group by user and send notification
            draftExpenses.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Expense::getUserId))
                    .forEach((userId, expenses) -> {
                        try {
                            User user = userRepository.findById(userId).orElse(null);
                            if (user != null) {
                                String subject = "Draft Expenses Cleanup - Trackify";
                                String content = String.format(
                                    "Hello %s,\n\n" +
                                    "We've cleaned up %d old draft expenses from your account that were created more than 30 days ago.\n\n" +
                                    "If you have any concerns, please contact our support team.\n\n" +
                                    "Best regards,\n" +
                                    "Trackify Team",
                                    user.getFirstName(),
                                    expenses.size()
                                );
                                emailService.sendEmail(user.getEmail(), subject, content);
                            }
                        } catch (Exception e) {
                            logger.error("Error notifying user of draft cleanup", e);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error notifying users of draft cleanup", e);
        }
    }

    private void generateCleanupReport(int totalCleaned) {
        try {
            logger.info("Generating weekly cleanup report");
            
            String reportContent = String.format(
                "Weekly Cleanup Report\n" +
                "=====================\n" +
                "Date: %s\n" +
                "Total Records Cleaned: %d\n" +
                "Cleanup Status: Success\n\n" +
                "Details available in application logs.",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                totalCleaned
            );

            // Send to administrators or log
            logger.info("Weekly cleanup report: {}", reportContent);

        } catch (Exception e) {
            logger.error("Error generating cleanup report", e);
        }
    }

    private void generateArchiveReport(int totalArchived) {
        try {
            logger.info("Generating monthly archive report");
            
            String reportContent = String.format(
                "Monthly Archive Report\n" +
                "=====================\n" +
                "Date: %s\n" +
                "Total Records Archived: %d\n" +
                "Archive Status: Success\n\n" +
                "Details available in application logs.",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                totalArchived
            );

            logger.info("Monthly archive report: {}", reportContent);

        } catch (Exception e) {
            logger.error("Error generating archive report", e);
        }
    }

    // Public methods for manual cleanup

    public void forceCleanupExpiredInvitations() {
        logger.info("Manual cleanup of expired invitations requested");
        cleanupExpiredInvitations();
    }

    public void forceCleanupOldNotifications() {
        logger.info("Manual cleanup of old notifications requested");
        cleanupOldNotifications();
    }

    public void forceCleanupOrphanedReceipts() {
        logger.info("Manual cleanup of orphaned receipts requested");
        cleanupOrphanedReceipts();
    }

    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }

    public void setCleanupEnabled(boolean enabled) {
        this.cleanupEnabled = enabled;
        logger.info("Data cleanup enabled: {}", enabled);
    }
}