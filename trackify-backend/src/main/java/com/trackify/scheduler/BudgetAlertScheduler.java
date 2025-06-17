package com.trackify.scheduler;

import com.trackify.dto.response.BudgetResponse;
import com.trackify.entity.Budget;
import com.trackify.entity.User;
import com.trackify.enums.NotificationType;
import com.trackify.repository.BudgetRepository;
import com.trackify.repository.UserRepository;
import com.trackify.service.BudgetService;
import com.trackify.service.EmailService;
import com.trackify.service.NotificationService;
import com.trackify.service.WebSocketService;
import com.trackify.dto.websocket.NotificationMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BudgetAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BudgetAlertScheduler.class);

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebSocketService webSocketService;

    @Value("${app.budget.alert-threshold:80.0}")
    private BigDecimal defaultAlertThreshold;

    @Value("${app.budget.notification-enabled:true}")
    private Boolean notificationEnabled;

    // Check budget alerts every hour
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds
    public void checkBudgetAlerts() {
        if (!notificationEnabled) {
            logger.debug("Budget notifications are disabled, skipping alert check");
            return;
        }

        try {
            logger.info("Starting scheduled budget alert check");

            // Find all budgets near threshold
            List<Budget> budgetsNearThreshold = budgetRepository.findBudgetsNearThreshold();
            processBudgetThresholdAlerts(budgetsNearThreshold);

            // Find all over-budget budgets
            List<Budget> overBudgets = budgetRepository.findOverBudgets();
            processOverBudgetAlerts(overBudgets);

            // Find budgets expiring soon (within 7 days)
            LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
            List<Budget> expiringSoonBudgets = budgetRepository.findActiveBudgets(LocalDate.now())
                    .stream()
                    .filter(budget -> budget.getEndDate().isBefore(sevenDaysFromNow) || 
                                     budget.getEndDate().isEqual(sevenDaysFromNow))
                    .collect(Collectors.toList());
            processExpiringBudgetAlerts(expiringSoonBudgets);

            logger.info("Completed scheduled budget alert check. Processed {} threshold alerts, {} over-budget alerts, {} expiring alerts",
                    budgetsNearThreshold.size(), overBudgets.size(), expiringSoonBudgets.size());

        } catch (Exception e) {
            logger.error("Error during scheduled budget alert check", e);
        }
    }

    // Check for budget alerts daily at 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyBudgetSummaryAlert() {
        if (!notificationEnabled) {
            return;
        }

        try {
            logger.info("Starting daily budget summary alert");

            List<User> users = userRepository.findAll();
            
            for (User user : users) {
                try {
                    sendDailyBudgetSummary(user);
                } catch (Exception e) {
                    logger.error("Error sending daily budget summary to user: {}", user.getUsername(), e);
                }
            }

            logger.info("Completed daily budget summary alert for {} users", users.size());

        } catch (Exception e) {
            logger.error("Error during daily budget summary alert", e);
        }
    }

    // Check for monthly budget reports on the 1st of each month at 10:00 AM
    @Scheduled(cron = "0 0 10 1 * *")
    public void monthlyBudgetReport() {
        if (!notificationEnabled) {
            return;
        }

        try {
            logger.info("Starting monthly budget report generation");

            List<User> users = userRepository.findAll();
            
            for (User user : users) {
                try {
                    sendMonthlyBudgetReport(user);
                } catch (Exception e) {
                    logger.error("Error sending monthly budget report to user: {}", user.getUsername(), e);
                }
            }

            logger.info("Completed monthly budget report for {} users", users.size());

        } catch (Exception e) {
            logger.error("Error during monthly budget report generation", e);
        }
    }

    // Process recurring budgets daily at 1:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    public void processRecurringBudgets() {
        try {
            logger.info("Starting recurring budget processing");
            budgetService.processRecurringBudgets();
            logger.info("Completed recurring budget processing");

        } catch (Exception e) {
            logger.error("Error processing recurring budgets", e);
        }
    }

    // Clean up expired budget alerts weekly on Sunday at 2:00 AM
    @Scheduled(cron = "0 0 2 * * 0")
    public void cleanupExpiredBudgets() {
        try {
            logger.info("Starting expired budget cleanup");

            List<Budget> expiredBudgets = budgetRepository.findExpiredBudgets(LocalDate.now().minusDays(30));
            
            for (Budget budget : expiredBudgets) {
                if (budget.getIsActive()) {
                    budget.setIsActive(false);
                    budgetRepository.save(budget);
                    logger.debug("Deactivated expired budget: {} for user: {}", budget.getId(), budget.getUserId());
                }
            }

            logger.info("Completed expired budget cleanup. Deactivated {} budgets", expiredBudgets.size());

        } catch (Exception e) {
            logger.error("Error during expired budget cleanup", e);
        }
    }

    private void processBudgetThresholdAlerts(List<Budget> budgets) {
        Map<Long, List<Budget>> budgetsByUser = budgets.stream()
                .collect(Collectors.groupingBy(Budget::getUserId));

        for (Map.Entry<Long, List<Budget>> entry : budgetsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Budget> userBudgets = entry.getValue();

            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    logger.warn("User not found for budget alerts: {}", userId);
                    continue;
                }

                for (Budget budget : userBudgets) {
                    sendBudgetThresholdAlert(user, budget);
                }

            } catch (Exception e) {
                logger.error("Error processing threshold alerts for user: {}", userId, e);
            }
        }
    }

    private void processOverBudgetAlerts(List<Budget> budgets) {
        Map<Long, List<Budget>> budgetsByUser = budgets.stream()
                .collect(Collectors.groupingBy(Budget::getUserId));

        for (Map.Entry<Long, List<Budget>> entry : budgetsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Budget> userBudgets = entry.getValue();

            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    logger.warn("User not found for over-budget alerts: {}", userId);
                    continue;
                }

                for (Budget budget : userBudgets) {
                    sendOverBudgetAlert(user, budget);
                }

            } catch (Exception e) {
                logger.error("Error processing over-budget alerts for user: {}", userId, e);
            }
        }
    }

    private void processExpiringBudgetAlerts(List<Budget> budgets) {
        Map<Long, List<Budget>> budgetsByUser = budgets.stream()
                .collect(Collectors.groupingBy(Budget::getUserId));

        for (Map.Entry<Long, List<Budget>> entry : budgetsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Budget> userBudgets = entry.getValue();

            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    logger.warn("User not found for expiring budget alerts: {}", userId);
                    continue;
                }

                for (Budget budget : userBudgets) {
                    sendExpiringBudgetAlert(user, budget);
                }

            } catch (Exception e) {
                logger.error("Error processing expiring budget alerts for user: {}", userId, e);
            }
        }
    }

    private void sendBudgetThresholdAlert(User user, Budget budget) {
        try {
            String subject = "Budget Alert: " + budget.getName() + " Nearing Threshold";
            String message = String.format(
                    "Your budget '%s' has reached %.2f%% of its allocated amount. " +
                    "Current spending: %s %s out of %s %s. " +
                    "Alert threshold: %.2f%%.",
                    budget.getName(),
                    budget.getUsedPercentage(),
                    budget.getSpentAmount(),
                    budget.getCurrency(),
                    budget.getTotalAmount(),
                    budget.getCurrency(),
                    budget.getAlertThreshold()
            );

            // Send email notification using the correct method signature
            emailService.sendBudgetAlertEmail(user.getEmail(), budget.getName(), 
                    budget.getUsedPercentage().doubleValue());

            // Send WebSocket notification
            webSocketService.sendNotificationToUser(user.getUsername(),
                    new NotificationMessage(
                            "Budget Threshold Alert",
                            message,
                            "WARNING",
                            user.getUsername(),
                            LocalDateTime.now()
                    ));

            // Create in-app notification using the correct method signature
            notificationService.notifyBudgetWarning(budget.getId(), user.getId(), 
                    budget.getName(), budget.getUsedPercentage().doubleValue());

            logger.debug("Sent threshold alert for budget: {} to user: {}", 
                    budget.getId(), user.getUsername());

        } catch (Exception e) {
            logger.error("Error sending threshold alert for budget: {} to user: {}", 
                    budget.getId(), user.getUsername(), e);
        }
    }

    private void sendOverBudgetAlert(User user, Budget budget) {
        try {
            BigDecimal overAmount = budget.getSpentAmount().subtract(budget.getTotalAmount());
            String subject = "Budget Alert: " + budget.getName() + " Exceeded";
            String message = String.format(
                    "Your budget '%s' has been exceeded by %s %s. " +
                    "Current spending: %s %s out of %s %s allocated.",
                    budget.getName(),
                    overAmount,
                    budget.getCurrency(),
                    budget.getSpentAmount(),
                    budget.getCurrency(),
                    budget.getTotalAmount(),
                    budget.getCurrency()
            );

            // Send email notification using the correct method signature
            emailService.sendBudgetExceededEmail(user.getEmail(), budget.getName(), 
                    overAmount.doubleValue());

            // Send WebSocket notification
            webSocketService.sendNotificationToUser(user.getUsername(),
                    new NotificationMessage(
                            "Budget Exceeded Alert",
                            message,
                            "ERROR",
                            user.getUsername(),
                            LocalDateTime.now()
                    ));

            // Create in-app notification using the correct method signature
            notificationService.notifyBudgetExceeded(budget.getId(), user.getId(), 
                    budget.getName(), budget.getUsedPercentage().doubleValue());

            logger.debug("Sent over-budget alert for budget: {} to user: {}", 
                    budget.getId(), user.getUsername());

        } catch (Exception e) {
            logger.error("Error sending over-budget alert for budget: {} to user: {}", 
                    budget.getId(), user.getUsername(), e);
        }
    }

    private void sendExpiringBudgetAlert(User user, Budget budget) {
        try {
            long daysUntilExpiry = LocalDate.now().until(budget.getEndDate()).getDays();
            String subject = "Budget Expiring: " + budget.getName();
            String message = String.format(
                    "Your budget '%s' will expire in %d day(s) on %s. " +
                    "Current usage: %.2f%% (%s %s out of %s %s).",
                    budget.getName(),
                    daysUntilExpiry,
                    budget.getEndDate(),
                    budget.getUsedPercentage(),
                    budget.getSpentAmount(),
                    budget.getCurrency(),
                    budget.getTotalAmount(),
                    budget.getCurrency()
            );

            // Send WebSocket notification
            webSocketService.sendNotificationToUser(user.getUsername(),
                    new NotificationMessage(
                            "Budget Expiring Soon",
                            message,
                            "INFO",
                            user.getUsername(),
                            LocalDateTime.now()
                    ));

            // Create in-app notification for expiring budget
            notificationService.createNotification(user.getId(), NotificationType.BUDGET_WARNING, 
                    subject, message, "BUDGET", budget.getId(), "MEDIUM");

            logger.debug("Sent expiring alert for budget: {} to user: {}", 
                    budget.getId(), user.getUsername());

        } catch (Exception e) {
            logger.error("Error sending expiring alert for budget: {} to user: {}", 
                    budget.getId(), user.getUsername(), e);
        }
    }

    private void sendDailyBudgetSummary(User user) {
        try {
            List<BudgetResponse> activeBudgets = budgetService.getActiveBudgets(user.getUsername());
            
            if (activeBudgets.isEmpty()) {
                return; // No active budgets, skip summary
            }

            List<BudgetResponse> alertBudgets = activeBudgets.stream()
                    .filter(budget -> budget.getUsedPercentage().compareTo(budget.getAlertThreshold()) >= 0)
                    .collect(Collectors.toList());

            if (!alertBudgets.isEmpty()) {
                StringBuilder summaryMessage = new StringBuilder();
                summaryMessage.append("Daily Budget Summary:\n");
                summaryMessage.append(String.format("You have %d active budget(s), %d requiring attention:\n\n", 
                        activeBudgets.size(), alertBudgets.size()));

                for (BudgetResponse budget : alertBudgets) {
                    summaryMessage.append(String.format("• %s: %.2f%% used (%s %s / %s %s)\n",
                            budget.getName(),
                            budget.getUsedPercentage(),
                            budget.getSpentAmount(),
                            budget.getCurrency(),
                            budget.getTotalAmount(),
                            budget.getCurrency()));
                }

                // Send email summary
                String emailContent = createDailyBudgetSummaryEmailContent(user, activeBudgets, alertBudgets);
                emailService.sendEmail(user.getEmail(), "Daily Budget Summary - Trackify", emailContent);

                // Send WebSocket notification
                webSocketService.sendNotificationToUser(user.getUsername(),
                        new NotificationMessage(
                                "Daily Budget Summary",
                                summaryMessage.toString(),
                                "INFO",
                                user.getUsername(),
                                LocalDateTime.now()
                        ));

                logger.debug("Sent daily budget summary to user: {}", user.getUsername());
            }

        } catch (Exception e) {
            logger.error("Error sending daily budget summary to user: {}", user.getUsername(), e);
        }
    }

    private void sendMonthlyBudgetReport(User user) {
        try {
            LocalDate startOfMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

            BudgetResponse.BudgetAnalytics analytics = budgetService.getBudgetAnalyticsForDashboard(
                    user.getUsername(), startOfMonth, endOfMonth);

            String reportMessage = String.format(
                    "Monthly Budget Report for %s:\n" +
                    "Total Budgeted: %s\n" +
                    "Total Spent: %s\n" +
                    "Total Remaining: %s\n" +
                    "Active Budgets: %d\n" +
                    "Over-Budget Count: %d\n" +
                    "Near Threshold Count: %d",
                    startOfMonth.getMonth() + " " + startOfMonth.getYear(),
                    analytics.getTotalBudgeted(),
                    analytics.getTotalSpent(),
                    analytics.getTotalRemaining(),
                    analytics.getActiveBudgetsCount(),
                    analytics.getOverBudgetCount(),
                    analytics.getNearThresholdCount()
            );

            // Create detailed email content for monthly report
            String emailContent = createMonthlyBudgetReportEmailContent(user, analytics, startOfMonth, endOfMonth);
            
            // Send email report using existing method
            emailService.sendMonthlyReportEmail(user.getEmail(), emailContent);

            // Send WebSocket notification
            webSocketService.sendNotificationToUser(user.getUsername(),
                    new NotificationMessage(
                            "Monthly Budget Report",
                            reportMessage,
                            "INFO",
                            user.getUsername(),
                            LocalDateTime.now()
                    ));

            logger.debug("Sent monthly budget report to user: {}", user.getUsername());

        } catch (Exception e) {
            logger.error("Error sending monthly budget report to user: {}", user.getUsername(), e);
        }
    }

    private String createDailyBudgetSummaryEmailContent(User user, List<BudgetResponse> activeBudgets, 
                                                       List<BudgetResponse> alertBudgets) {
        StringBuilder content = new StringBuilder();
        content.append(String.format("Hello %s,\n\n", user.getFirstName()));
        content.append("Here's your daily budget summary:\n\n");
        content.append(String.format("Total Active Budgets: %d\n", activeBudgets.size()));
        content.append(String.format("Budgets Requiring Attention: %d\n\n", alertBudgets.size()));
        
        if (!alertBudgets.isEmpty()) {
            content.append("Budgets needing your attention:\n");
            for (BudgetResponse budget : alertBudgets) {
                content.append(String.format("• %s: %.2f%% used (%s %s out of %s %s)\n",
                        budget.getName(),
                        budget.getUsedPercentage(),
                        budget.getSpentAmount(),
                        budget.getCurrency(),
                        budget.getTotalAmount(),
                        budget.getCurrency()));
            }
            content.append("\n");
        }
        
        content.append("Best regards,\n");
        content.append("Trackify Team");
        
        return content.toString();
    }

    private String createMonthlyBudgetReportEmailContent(User user, BudgetResponse.BudgetAnalytics analytics, 
                                                        LocalDate startOfMonth, LocalDate endOfMonth) {
        StringBuilder content = new StringBuilder();
        content.append(String.format("Hello %s,\n\n", user.getFirstName()));
        content.append(String.format("Here's your monthly budget report for %s %d:\n\n", 
                startOfMonth.getMonth(), startOfMonth.getYear()));
        
        content.append("BUDGET SUMMARY:\n");
        content.append(String.format("Total Budgeted: %s\n", analytics.getTotalBudgeted()));
        content.append(String.format("Total Spent: %s\n", analytics.getTotalSpent()));
        content.append(String.format("Total Remaining: %s\n", analytics.getTotalRemaining()));
        content.append(String.format("Active Budgets: %d\n", analytics.getActiveBudgetsCount()));
        content.append(String.format("Over-Budget Count: %d\n", analytics.getOverBudgetCount()));
        content.append(String.format("Near Threshold Count: %d\n\n", analytics.getNearThresholdCount()));
        
        if (analytics.getOverBudgetCount() > 0) {
            content.append("⚠️ You have budgets that exceeded their limits this month. ");
            content.append("Please review your spending patterns.\n\n");
        }
        
        if (analytics.getNearThresholdCount() > 0) {
            content.append("📊 Some budgets are approaching their alert thresholds. ");
            content.append("Consider monitoring these budgets closely.\n\n");
        }
        
        content.append("View detailed reports: ").append(emailService.getClass().getAnnotation(Value.class) != null ? 
                "${app.email.base-url:http://localhost:4200}" : "http://localhost:4200").append("/reports\n\n");
        content.append("Best regards,\n");
        content.append("Trackify Team");
        
        return content.toString();
    }
}