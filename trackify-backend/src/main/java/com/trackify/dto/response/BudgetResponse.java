package com.trackify.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal totalAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usedPercentage;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private String currency;
    private BigDecimal alertThreshold;
    private Boolean isActive;
    private Boolean isRecurring;
    private String recurrencePeriod;
    private Long userId;
    private String username;
    private Long categoryId;
    private String categoryName;
    private Long teamId;
    private String teamName;
    
    private BudgetStatus status;
    private BudgetAlert alert;
    private List<ExpenseInfo> recentExpenses;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Constructor for basic budget response
    public BudgetResponse(Long id, String name, BigDecimal totalAmount, BigDecimal spentAmount,
                         LocalDate startDate, LocalDate endDate, String currency, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.totalAmount = totalAmount;
        this.spentAmount = spentAmount;
        this.remainingAmount = totalAmount.subtract(spentAmount);
        this.startDate = startDate;
        this.endDate = endDate;
        this.currency = currency;
        this.isActive = isActive;
        
        // Calculate used percentage
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.usedPercentage = spentAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            this.usedPercentage = BigDecimal.ZERO;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetStatus {
        private String status; // ACTIVE, EXPIRED, OVER_BUDGET, NEAR_THRESHOLD, INACTIVE
        private String statusMessage;
        private Boolean isOverBudget;
        private Boolean isNearThreshold;
        private Boolean isExpired;
        private Boolean isCurrentlyActive;
        private Integer daysRemaining;
        private BigDecimal amountOverBudget;

        // Constructor
        public BudgetStatus(String status, String statusMessage, Boolean isOverBudget, 
                           Boolean isNearThreshold, Boolean isExpired) {
            this.status = status;
            this.statusMessage = statusMessage;
            this.isOverBudget = isOverBudget;
            this.isNearThreshold = isNearThreshold;
            this.isExpired = isExpired;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetAlert {
        private String alertType; // INFO, WARNING, DANGER
        private String alertMessage;
        private BigDecimal currentThreshold;
        private BigDecimal alertThreshold;
        private Boolean shouldAlert;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime alertTime;

        // Constructor
        public BudgetAlert(String alertType, String alertMessage, BigDecimal currentThreshold,
                          BigDecimal alertThreshold, Boolean shouldAlert) {
            this.alertType = alertType;
            this.alertMessage = alertMessage;
            this.currentThreshold = currentThreshold;
            this.alertThreshold = alertThreshold;
            this.shouldAlert = shouldAlert;
            this.alertTime = LocalDateTime.now();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseInfo {
        private Long expenseId;
        private String title;
        private BigDecimal amount;
        private String categoryName;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expenseDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        // Constructor
        public ExpenseInfo(Long expenseId, String title, BigDecimal amount, 
                          String categoryName, LocalDate expenseDate) {
            this.expenseId = expenseId;
            this.title = title;
            this.amount = amount;
            this.categoryName = categoryName;
            this.expenseDate = expenseDate;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetSummary {
        private Long id;
        private String name;
        private BigDecimal totalAmount;
        private BigDecimal spentAmount;
        private BigDecimal usedPercentage;
        private String status;
        private String categoryName;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        // Constructor
        public BudgetSummary(Long id, String name, BigDecimal totalAmount, 
                           BigDecimal spentAmount, String status, LocalDate endDate) {
            this.id = id;
            this.name = name;
            this.totalAmount = totalAmount;
            this.spentAmount = spentAmount;
            this.status = status;
            this.endDate = endDate;
            
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.usedPercentage = spentAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                this.usedPercentage = BigDecimal.ZERO;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetAnalytics {
        private BigDecimal totalBudgeted;
        private BigDecimal totalSpent;
        private BigDecimal totalRemaining;
        private BigDecimal averageUsagePercentage;
        private Integer activeBudgetsCount;
        private Integer overBudgetCount;
        private Integer nearThresholdCount;
        private List<CategoryBudgetSummary> categoryBreakdown;
        private List<MonthlyBudgetTrend> monthlyTrends;

        // Constructor
        public BudgetAnalytics(BigDecimal totalBudgeted, BigDecimal totalSpent,
                              Integer activeBudgetsCount, Integer overBudgetCount) {
            this.totalBudgeted = totalBudgeted;
            this.totalSpent = totalSpent;
            this.totalRemaining = totalBudgeted.subtract(totalSpent);
            this.activeBudgetsCount = activeBudgetsCount;
            this.overBudgetCount = overBudgetCount;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBudgetSummary {
        private Long categoryId;
        private String categoryName;
        private BigDecimal budgetedAmount;
        private BigDecimal spentAmount;
        private BigDecimal usedPercentage;
        private Integer budgetCount;

        // Constructor
        public CategoryBudgetSummary(Long categoryId, String categoryName, 
                                   BigDecimal budgetedAmount, BigDecimal spentAmount) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.budgetedAmount = budgetedAmount;
            this.spentAmount = spentAmount;
            
            if (budgetedAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.usedPercentage = spentAmount.divide(budgetedAmount, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                this.usedPercentage = BigDecimal.ZERO;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyBudgetTrend {
        private String month;
        private Integer year;
        private BigDecimal budgetedAmount;
        private BigDecimal spentAmount;
        private BigDecimal usedPercentage;
        private Integer budgetCount;

        // Constructor
        public MonthlyBudgetTrend(String month, Integer year, BigDecimal budgetedAmount, 
                                BigDecimal spentAmount, Integer budgetCount) {
            this.month = month;
            this.year = year;
            this.budgetedAmount = budgetedAmount;
            this.spentAmount = spentAmount;
            this.budgetCount = budgetCount;
            
            if (budgetedAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.usedPercentage = spentAmount.divide(budgetedAmount, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                this.usedPercentage = BigDecimal.ZERO;
            }
        }
    }
}