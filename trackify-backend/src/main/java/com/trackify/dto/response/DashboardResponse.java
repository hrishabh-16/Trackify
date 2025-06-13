package com.trackify.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    
    private ExpenseSummary expenseSummary;
    private BudgetSummary budgetSummary;
    private CategorySummary categorySummary;
    private List<MonthlyExpense> monthlyExpenses;
    private List<CategoryExpense> categoryExpenses;
    private List<DailyExpense> dailyExpenses;
    private ExpenseTrend expenseTrend;
    private List<TopCategory> topCategories;
    private List<RecentExpense> recentExpenses;
    private List<BudgetStatus> budgetStatusList;
    private BudgetAlert budgetAlert;
    private TeamSummary teamSummary;
    private List<TeamMemberExpense> teamMemberExpenses;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseSummary {
        private BigDecimal totalAmount;
        private long totalCount;
        private long approvedCount;
        private long pendingCount;
        private long rejectedCount;
        private BigDecimal averageAmount;
        private BigDecimal changePercentage;
        
        
		public ExpenseSummary(BigDecimal totalAmount, long totalCount, long approvedCount, long pendingCount,
				long rejectedCount, BigDecimal averageAmount, BigDecimal changePercentage) {
			super();
			this.totalAmount = totalAmount;
			this.totalCount = totalCount;
			this.approvedCount = approvedCount;
			this.pendingCount = pendingCount;
			this.rejectedCount = rejectedCount;
			this.averageAmount = averageAmount;
			this.changePercentage = changePercentage;
		}
		public BigDecimal getTotalAmount() {
			return totalAmount;
		}
		public void setTotalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
		}
		public long getTotalCount() {
			return totalCount;
		}
		public void setTotalCount(long totalCount) {
			this.totalCount = totalCount;
		}
		public long getApprovedCount() {
			return approvedCount;
		}
		public void setApprovedCount(long approvedCount) {
			this.approvedCount = approvedCount;
		}
		public long getPendingCount() {
			return pendingCount;
		}
		public void setPendingCount(long pendingCount) {
			this.pendingCount = pendingCount;
		}
		public long getRejectedCount() {
			return rejectedCount;
		}
		public void setRejectedCount(long rejectedCount) {
			this.rejectedCount = rejectedCount;
		}
		public BigDecimal getAverageAmount() {
			return averageAmount;
		}
		public void setAverageAmount(BigDecimal averageAmount) {
			this.averageAmount = averageAmount;
		}
		public BigDecimal getChangePercentage() {
			return changePercentage;
		}
		public void setChangePercentage(BigDecimal changePercentage) {
			this.changePercentage = changePercentage;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetSummary {
        private BigDecimal totalBudget;
        private BigDecimal totalSpent;
        private BigDecimal remainingBudget;
        private BigDecimal usedPercentage;
        private long activeBudgets;
        private long exceededBudgets;
		public BudgetSummary(BigDecimal totalBudget, BigDecimal totalSpent, BigDecimal remainingBudget,
				BigDecimal usedPercentage, long activeBudgets, long exceededBudgets) {
			super();
			this.totalBudget = totalBudget;
			this.totalSpent = totalSpent;
			this.remainingBudget = remainingBudget;
			this.usedPercentage = usedPercentage;
			this.activeBudgets = activeBudgets;
			this.exceededBudgets = exceededBudgets;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private long totalCategories;
        private String topCategory;
        private BigDecimal topCategoryAmount;
        private List<Map.Entry<String, BigDecimal>> categoryBreakdown;
		public CategorySummary(long totalCategories, String topCategory, BigDecimal topCategoryAmount,
				List<Entry<String, BigDecimal>> categoryBreakdown) {
			super();
			this.totalCategories = totalCategories;
			this.topCategory = topCategory;
			this.topCategoryAmount = topCategoryAmount;
			this.categoryBreakdown = categoryBreakdown;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyExpense {
        private String month;
        private String monthName;
        private int year;
        private BigDecimal amount;
        private long expenseCount;
		public String getMonth() {
			return month;
		}
		public void setMonth(String month) {
			this.month = month;
		}
		public String getMonthName() {
			return monthName;
		}
		public void setMonthName(String monthName) {
			this.monthName = monthName;
		}
		public int getYear() {
			return year;
		}
		public void setYear(int year) {
			this.year = year;
		}
		public BigDecimal getAmount() {
			return amount;
		}
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
		public long getExpenseCount() {
			return expenseCount;
		}
		public void setExpenseCount(long expenseCount) {
			this.expenseCount = expenseCount;
		}
		public MonthlyExpense(String month, String monthName, int year, BigDecimal amount, long expenseCount) {
			super();
			this.month = month;
			this.monthName = monthName;
			this.year = year;
			this.amount = amount;
			this.expenseCount = expenseCount;
		}
        
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryExpense {
        private String categoryName;
        private BigDecimal amount;
        private long expenseCount;
        private String color;
		public String getCategoryName() {
			return categoryName;
		}
		public void setCategoryName(String categoryName) {
			this.categoryName = categoryName;
		}
		public BigDecimal getAmount() {
			return amount;
		}
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
		public long getExpenseCount() {
			return expenseCount;
		}
		public void setExpenseCount(long expenseCount) {
			this.expenseCount = expenseCount;
		}
		public String getColor() {
			return color;
		}
		public void setColor(String color) {
			this.color = color;
		}
		public CategoryExpense(String categoryName, BigDecimal amount, long expenseCount, String color) {
			super();
			this.categoryName = categoryName;
			this.amount = amount;
			this.expenseCount = expenseCount;
			this.color = color;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyExpense {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private String dateString;
        private BigDecimal amount;
        private long expenseCount;
		public LocalDate getDate() {
			return date;
		}
		public void setDate(LocalDate date) {
			this.date = date;
		}
		public String getDateString() {
			return dateString;
		}
		public void setDateString(String dateString) {
			this.dateString = dateString;
		}
		public BigDecimal getAmount() {
			return amount;
		}
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
		public long getExpenseCount() {
			return expenseCount;
		}
		public void setExpenseCount(long expenseCount) {
			this.expenseCount = expenseCount;
		}
		public DailyExpense(LocalDate date, String dateString, BigDecimal amount, long expenseCount) {
			super();
			this.date = date;
			this.dateString = dateString;
			this.amount = amount;
			this.expenseCount = expenseCount;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseTrend {
        private String trend; // INCREASING, DECREASING, STABLE
        private BigDecimal changePercentage;
        private String description;
		public ExpenseTrend(String trend, BigDecimal changePercentage, String description) {
			super();
			this.trend = trend;
			this.changePercentage = changePercentage;
			this.description = description;
		}
		public String getTrend() {
			return trend;
		}
		public void setTrend(String trend) {
			this.trend = trend;
		}
		public BigDecimal getChangePercentage() {
			return changePercentage;
		}
		public void setChangePercentage(BigDecimal changePercentage) {
			this.changePercentage = changePercentage;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCategory {
        private String categoryName;
        private BigDecimal amount;
        private long expenseCount;
		public TopCategory(String categoryName, BigDecimal amount, long expenseCount) {
			super();
			this.categoryName = categoryName;
			this.amount = amount;
			this.expenseCount = expenseCount;
		}
		public String getCategoryName() {
			return categoryName;
		}
		public void setCategoryName(String categoryName) {
			this.categoryName = categoryName;
		}
		public BigDecimal getAmount() {
			return amount;
		}
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
		public long getExpenseCount() {
			return expenseCount;
		}
		public void setExpenseCount(long expenseCount) {
			this.expenseCount = expenseCount;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentExpense {
        private Long expenseId;
        private String title;
        private String description;
        private BigDecimal amount;
        private String categoryName;
        private String status;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime expenseDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

		public RecentExpense(Long expenseId, String title, String description, BigDecimal amount, String categoryName,
				String status, LocalDateTime expenseDate, LocalDateTime createdAt) {
			super();
			this.expenseId = expenseId;
			this.title = title;
			this.description = description;
			this.amount = amount;
			this.categoryName = categoryName;
			this.status = status;
			this.expenseDate = expenseDate;
			this.createdAt = createdAt;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetStatus {
        private Long budgetId;
        private String categoryName;
        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private BigDecimal usedPercentage;
        private String status; // ON_TRACK, WARNING, EXCEEDED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetAlert {
        private String alertType; // SUCCESS, WARNING, ERROR
        private String message;
        private long exceededBudgets;
        private long warningBudgets;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;

		public BudgetAlert(String alertType, String message, long exceededBudgets, long warningBudgets,
				LocalDateTime timestamp) {
			super();
			this.alertType = alertType;
			this.message = message;
			this.exceededBudgets = exceededBudgets;
			this.warningBudgets = warningBudgets;
			this.timestamp = timestamp;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamSummary {
        private Long teamId;
        private BigDecimal totalTeamExpenses;
        private long totalExpenseCount;
        private long activeMembersCount;
        private String topSpender;
		public TeamSummary(Long teamId, BigDecimal totalTeamExpenses, long totalExpenseCount, long activeMembersCount,
				String topSpender) {
			super();
			this.teamId = teamId;
			this.totalTeamExpenses = totalTeamExpenses;
			this.totalExpenseCount = totalExpenseCount;
			this.activeMembersCount = activeMembersCount;
			this.topSpender = topSpender;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberExpense {
        private Long userId;
        private String username;
        private String fullName;
        private BigDecimal totalAmount;
        private long expenseCount;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonData {
        private BigDecimal currentAmount;
        private BigDecimal previousAmount;
        private BigDecimal amountDifference;
        private BigDecimal percentageChange;
        private long countDifference;
        
        
		public ComparisonData(BigDecimal currentAmount, BigDecimal previousAmount, BigDecimal amountDifference,
				BigDecimal percentageChange, long countDifference) {
			super();
			this.currentAmount = currentAmount;
			this.previousAmount = previousAmount;
			this.amountDifference = amountDifference;
			this.percentageChange = percentageChange;
			this.countDifference = countDifference;
		}
		public BigDecimal getCurrentAmount() {
			return currentAmount;
		}
		public void setCurrentAmount(BigDecimal currentAmount) {
			this.currentAmount = currentAmount;
		}
		public BigDecimal getPreviousAmount() {
			return previousAmount;
		}
		public void setPreviousAmount(BigDecimal previousAmount) {
			this.previousAmount = previousAmount;
		}
		public BigDecimal getAmountDifference() {
			return amountDifference;
		}
		public void setAmountDifference(BigDecimal amountDifference) {
			this.amountDifference = amountDifference;
		}
		public BigDecimal getPercentageChange() {
			return percentageChange;
		}
		public void setPercentageChange(BigDecimal percentageChange) {
			this.percentageChange = percentageChange;
		}
		public long getCountDifference() {
			return countDifference;
		}
		public void setCountDifference(long countDifference) {
			this.countDifference = countDifference;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearlyComparison {
        private int currentYear;
        private int previousYear;
        private BigDecimal currentYearAmount;
        private BigDecimal previousYearAmount;
        private BigDecimal amountDifference;
        private BigDecimal percentageChange;
        private long countDifference;
		public YearlyComparison(int currentYear, int previousYear, BigDecimal currentYearAmount,
				BigDecimal previousYearAmount, BigDecimal amountDifference, BigDecimal percentageChange,
				long countDifference) {
			super();
			this.currentYear = currentYear;
			this.previousYear = previousYear;
			this.currentYearAmount = currentYearAmount;
			this.previousYearAmount = previousYearAmount;
			this.amountDifference = amountDifference;
			this.percentageChange = percentageChange;
			this.countDifference = countDifference;
		}
        
        
    }

	public ExpenseSummary getExpenseSummary() {
		return expenseSummary;
	}

	public void setExpenseSummary(ExpenseSummary expenseSummary) {
		this.expenseSummary = expenseSummary;
	}

	public BudgetSummary getBudgetSummary() {
		return budgetSummary;
	}

	public void setBudgetSummary(BudgetSummary budgetSummary) {
		this.budgetSummary = budgetSummary;
	}

	public CategorySummary getCategorySummary() {
		return categorySummary;
	}

	public void setCategorySummary(CategorySummary categorySummary) {
		this.categorySummary = categorySummary;
	}

	public List<MonthlyExpense> getMonthlyExpenses() {
		return monthlyExpenses;
	}

	public void setMonthlyExpenses(List<MonthlyExpense> monthlyExpenses) {
		this.monthlyExpenses = monthlyExpenses;
	}

	public List<CategoryExpense> getCategoryExpenses() {
		return categoryExpenses;
	}

	public void setCategoryExpenses(List<CategoryExpense> categoryExpenses) {
		this.categoryExpenses = categoryExpenses;
	}

	public List<DailyExpense> getDailyExpenses() {
		return dailyExpenses;
	}

	public void setDailyExpenses(List<DailyExpense> dailyExpenses) {
		this.dailyExpenses = dailyExpenses;
	}

	public ExpenseTrend getExpenseTrend() {
		return expenseTrend;
	}

	public void setExpenseTrend(ExpenseTrend expenseTrend) {
		this.expenseTrend = expenseTrend;
	}

	public List<TopCategory> getTopCategories() {
		return topCategories;
	}

	public void setTopCategories(List<TopCategory> topCategories) {
		this.topCategories = topCategories;
	}

	public List<RecentExpense> getRecentExpenses() {
		return recentExpenses;
	}

	public void setRecentExpenses(List<RecentExpense> recentExpenses) {
		this.recentExpenses = recentExpenses;
	}

	public List<BudgetStatus> getBudgetStatusList() {
		return budgetStatusList;
	}

	public void setBudgetStatusList(List<BudgetStatus> budgetStatusList) {
		this.budgetStatusList = budgetStatusList;
	}

	public BudgetAlert getBudgetAlert() {
		return budgetAlert;
	}

	public void setBudgetAlert(BudgetAlert budgetAlert) {
		this.budgetAlert = budgetAlert;
	}

	public TeamSummary getTeamSummary() {
		return teamSummary;
	}

	public void setTeamSummary(TeamSummary teamSummary) {
		this.teamSummary = teamSummary;
	}

	public List<TeamMemberExpense> getTeamMemberExpenses() {
		return teamMemberExpenses;
	}

	public void setTeamMemberExpenses(List<TeamMemberExpense> teamMemberExpenses) {
		this.teamMemberExpenses = teamMemberExpenses;
	}

	public LocalDateTime getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(LocalDateTime generatedAt) {
		this.generatedAt = generatedAt;
	}
    
    
}