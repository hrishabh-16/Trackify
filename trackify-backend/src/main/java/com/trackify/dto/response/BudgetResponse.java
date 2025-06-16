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

    public BudgetResponse() {
		
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

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getStatusMessage() {
			return statusMessage;
		}

		public void setStatusMessage(String statusMessage) {
			this.statusMessage = statusMessage;
		}

		public Boolean getIsOverBudget() {
			return isOverBudget;
		}

		public void setIsOverBudget(Boolean isOverBudget) {
			this.isOverBudget = isOverBudget;
		}

		public Boolean getIsNearThreshold() {
			return isNearThreshold;
		}

		public void setIsNearThreshold(Boolean isNearThreshold) {
			this.isNearThreshold = isNearThreshold;
		}

		public Boolean getIsExpired() {
			return isExpired;
		}

		public void setIsExpired(Boolean isExpired) {
			this.isExpired = isExpired;
		}

		public Boolean getIsCurrentlyActive() {
			return isCurrentlyActive;
		}

		public void setIsCurrentlyActive(Boolean isCurrentlyActive) {
			this.isCurrentlyActive = isCurrentlyActive;
		}

		public Integer getDaysRemaining() {
			return daysRemaining;
		}

		public void setDaysRemaining(Integer daysRemaining) {
			this.daysRemaining = daysRemaining;
		}

		public BigDecimal getAmountOverBudget() {
			return amountOverBudget;
		}

		public void setAmountOverBudget(BigDecimal amountOverBudget) {
			this.amountOverBudget = amountOverBudget;
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public BigDecimal getSpentAmount() {
		return spentAmount;
	}

	public void setSpentAmount(BigDecimal spentAmount) {
		this.spentAmount = spentAmount;
	}

	public BigDecimal getRemainingAmount() {
		return remainingAmount;
	}

	public void setRemainingAmount(BigDecimal remainingAmount) {
		this.remainingAmount = remainingAmount;
	}

	public BigDecimal getUsedPercentage() {
		return usedPercentage;
	}

	public void setUsedPercentage(BigDecimal usedPercentage) {
		this.usedPercentage = usedPercentage;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getAlertThreshold() {
		return alertThreshold;
	}

	public void setAlertThreshold(BigDecimal alertThreshold) {
		this.alertThreshold = alertThreshold;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Boolean getIsRecurring() {
		return isRecurring;
	}

	public void setIsRecurring(Boolean isRecurring) {
		this.isRecurring = isRecurring;
	}

	public String getRecurrencePeriod() {
		return recurrencePeriod;
	}

	public void setRecurrencePeriod(String recurrencePeriod) {
		this.recurrencePeriod = recurrencePeriod;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public BudgetStatus getStatus() {
		return status;
	}

	public void setStatus(BudgetStatus status) {
		this.status = status;
	}

	public BudgetAlert getAlert() {
		return alert;
	}

	public void setAlert(BudgetAlert alert) {
		this.alert = alert;
	}

	public List<ExpenseInfo> getRecentExpenses() {
		return recentExpenses;
	}

	public void setRecentExpenses(List<ExpenseInfo> recentExpenses) {
		this.recentExpenses = recentExpenses;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
    
    
}