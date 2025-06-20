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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    
    private String reportId;
    private String reportType;
    private String reportName;
    private String description;
    private String format;
    private String status; // GENERATING, COMPLETED, FAILED
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;
    
    private String generatedBy;
    private Long fileSizeBytes;
    private String downloadUrl;
    private ReportSummary summary;
    private ReportData data;
    private List<ReportChart> charts;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    // Constructor for basic report response
    public ReportResponse(String reportId, String reportType, String reportName, 
                         LocalDate startDate, LocalDate endDate, String status) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.reportName = reportName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.generatedAt = LocalDateTime.now();
    }

    public ReportResponse() {
		
	}

	@Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportSummary {
        private BigDecimal totalAmount;
        private Long totalCount;
        private BigDecimal averageAmount;
        private BigDecimal maxAmount;
        private BigDecimal minAmount;
        private String topCategory;
        private String topUser;
        private Map<String, Object> additionalMetrics;

        // Constructor
        public ReportSummary(BigDecimal totalAmount, Long totalCount, BigDecimal averageAmount) {
            this.totalAmount = totalAmount;
            this.totalCount = totalCount;
            this.averageAmount = averageAmount;
        }

		public BigDecimal getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
		}

		public Long getTotalCount() {
			return totalCount;
		}

		public void setTotalCount(Long totalCount) {
			this.totalCount = totalCount;
		}

		public BigDecimal getAverageAmount() {
			return averageAmount;
		}

		public void setAverageAmount(BigDecimal averageAmount) {
			this.averageAmount = averageAmount;
		}

		public BigDecimal getMaxAmount() {
			return maxAmount;
		}

		public void setMaxAmount(BigDecimal maxAmount) {
			this.maxAmount = maxAmount;
		}

		public BigDecimal getMinAmount() {
			return minAmount;
		}

		public void setMinAmount(BigDecimal minAmount) {
			this.minAmount = minAmount;
		}

		public String getTopCategory() {
			return topCategory;
		}

		public void setTopCategory(String topCategory) {
			this.topCategory = topCategory;
		}

		public String getTopUser() {
			return topUser;
		}

		public void setTopUser(String topUser) {
			this.topUser = topUser;
		}

		public Map<String, Object> getAdditionalMetrics() {
			return additionalMetrics;
		}

		public void setAdditionalMetrics(Map<String, Object> additionalMetrics) {
			this.additionalMetrics = additionalMetrics;
		}
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportData {
        private List<ExpenseItem> expenses;
        private List<BudgetItem> budgets;
        private List<ApprovalItem> approvals;
        private List<CategorySummary> categorySummaries;
        private List<UserSummary> userSummaries;
        private List<TeamSummary> teamSummaries;
        private List<MonthlySummary> monthlySummaries;
        
        // NEW: Custom analysis fields for CUSTOM report type
        private List<TrendAnalysis> trendAnalysis;
        private List<ComparisonAnalysis> comparisonAnalysis;
        private List<VarianceAnalysis> varianceAnalysis;
        private List<ForecastAnalysis> forecastAnalysis;
        private List<FinancialMetrics> financialMetrics;

		public List<ExpenseItem> getExpenses() {
			return expenses;
		}
		public void setExpenses(List<ExpenseItem> expenses) {
			this.expenses = expenses;
		}
		public List<BudgetItem> getBudgets() {
			return budgets;
		}
		public void setBudgets(List<BudgetItem> budgets) {
			this.budgets = budgets;
		}
		public List<ApprovalItem> getApprovals() {
			return approvals;
		}
		public void setApprovals(List<ApprovalItem> approvals) {
			this.approvals = approvals;
		}
		public List<CategorySummary> getCategorySummaries() {
			return categorySummaries;
		}
		public void setCategorySummaries(List<CategorySummary> categorySummaries) {
			this.categorySummaries = categorySummaries;
		}
		public List<UserSummary> getUserSummaries() {
			return userSummaries;
		}
		public void setUserSummaries(List<UserSummary> userSummaries) {
			this.userSummaries = userSummaries;
		}
		public List<TeamSummary> getTeamSummaries() {
			return teamSummaries;
		}
		public void setTeamSummaries(List<TeamSummary> teamSummaries) {
			this.teamSummaries = teamSummaries;
		}
		public List<MonthlySummary> getMonthlySummaries() {
			return monthlySummaries;
		}
		public void setMonthlySummaries(List<MonthlySummary> monthlySummaries) {
			this.monthlySummaries = monthlySummaries;
		}
		
		// NEW: Getters and setters for custom analysis fields
		public List<TrendAnalysis> getTrendAnalysis() {
			return trendAnalysis;
		}
		public void setTrendAnalysis(List<TrendAnalysis> trendAnalysis) {
			this.trendAnalysis = trendAnalysis;
		}
		public List<ComparisonAnalysis> getComparisonAnalysis() {
			return comparisonAnalysis;
		}
		public void setComparisonAnalysis(List<ComparisonAnalysis> comparisonAnalysis) {
			this.comparisonAnalysis = comparisonAnalysis;
		}
		public List<VarianceAnalysis> getVarianceAnalysis() {
			return varianceAnalysis;
		}
		public void setVarianceAnalysis(List<VarianceAnalysis> varianceAnalysis) {
			this.varianceAnalysis = varianceAnalysis;
		}
		public List<ForecastAnalysis> getForecastAnalysis() {
			return forecastAnalysis;
		}
		public void setForecastAnalysis(List<ForecastAnalysis> forecastAnalysis) {
			this.forecastAnalysis = forecastAnalysis;
		}
		public List<FinancialMetrics> getFinancialMetrics() {
			return financialMetrics;
		}
		public void setFinancialMetrics(List<FinancialMetrics> financialMetrics) {
			this.financialMetrics = financialMetrics;
		}
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseItem {
        private Long expenseId;
        private String title;
        private String description;
        private BigDecimal amount;
        private String currency;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expenseDate;
        
        private String category;
        private String user;
        private String team;
        private String status;
        private String paymentMethod;
        private String merchantName;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        // Constructor
        public ExpenseItem(Long expenseId, String title, BigDecimal amount, 
                          LocalDate expenseDate, String category, String user, String status) {
            this.expenseId = expenseId;
            this.title = title;
            this.amount = amount;
            this.expenseDate = expenseDate;
            this.category = category;
            this.user = user;
            this.status = status;
        }

		public Long getExpenseId() {
			return expenseId;
		}

		public void setExpenseId(Long expenseId) {
			this.expenseId = expenseId;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}

		public LocalDate getExpenseDate() {
			return expenseDate;
		}

		public void setExpenseDate(LocalDate expenseDate) {
			this.expenseDate = expenseDate;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getTeam() {
			return team;
		}

		public void setTeam(String team) {
			this.team = team;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getPaymentMethod() {
			return paymentMethod;
		}

		public void setPaymentMethod(String paymentMethod) {
			this.paymentMethod = paymentMethod;
		}

		public String getMerchantName() {
			return merchantName;
		}

		public void setMerchantName(String merchantName) {
			this.merchantName = merchantName;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(LocalDateTime createdAt) {
			this.createdAt = createdAt;
		}
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetItem {
        private Long budgetId;
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
        
        private String category;
        private String team;
        private String status;
        private Boolean isOverBudget;

        // Constructor
        public BudgetItem(Long budgetId, String name, BigDecimal totalAmount, 
                         BigDecimal spentAmount, String category, String status) {
            this.budgetId = budgetId;
            this.name = name;
            this.totalAmount = totalAmount;
            this.spentAmount = spentAmount;
            this.remainingAmount = totalAmount.subtract(spentAmount);
            this.category = category;
            this.status = status;
            this.isOverBudget = spentAmount.compareTo(totalAmount) > 0;
            
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
    public static class ApprovalItem {
        private Long workflowId;
        private Long expenseId;
        private String expenseTitle;
        private BigDecimal expenseAmount;
        private String submittedBy;
        private String currentApprover;
        private String status;
        private String priority;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime submittedAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime approvedAt;
        
        private Long approvalTimeHours;
        private Boolean isOverdue;
        private Boolean isEscalated;

        // Constructor
        public ApprovalItem(Long workflowId, Long expenseId, String expenseTitle, 
                           BigDecimal expenseAmount, String submittedBy, String status) {
            this.workflowId = workflowId;
            this.expenseId = expenseId;
            this.expenseTitle = expenseTitle;
            this.expenseAmount = expenseAmount;
            this.submittedBy = submittedBy;
            this.status = status;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String categoryName;
        private BigDecimal totalAmount;
        private Long expenseCount;
        private BigDecimal averageAmount;
        private BigDecimal budgetAmount;
        private BigDecimal budgetUsedPercentage;
        private Boolean isOverBudget;

        // Constructor
        public CategorySummary(String categoryName, BigDecimal totalAmount, Long expenseCount) {
            this.categoryName = categoryName;
            this.totalAmount = totalAmount;
            this.expenseCount = expenseCount;
            this.averageAmount = expenseCount > 0 ? 
                totalAmount.divide(BigDecimal.valueOf(expenseCount), 2, java.math.RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        }

		public String getCategoryName() {
			return categoryName;
		}

		public void setCategoryName(String categoryName) {
			this.categoryName = categoryName;
		}

		public BigDecimal getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
		}

		public Long getExpenseCount() {
			return expenseCount;
		}

		public void setExpenseCount(Long expenseCount) {
			this.expenseCount = expenseCount;
		}

		public BigDecimal getAverageAmount() {
			return averageAmount;
		}

		public void setAverageAmount(BigDecimal averageAmount) {
			this.averageAmount = averageAmount;
		}

		public BigDecimal getBudgetAmount() {
			return budgetAmount;
		}

		public void setBudgetAmount(BigDecimal budgetAmount) {
			this.budgetAmount = budgetAmount;
		}

		public BigDecimal getBudgetUsedPercentage() {
			return budgetUsedPercentage;
		}

		public void setBudgetUsedPercentage(BigDecimal budgetUsedPercentage) {
			this.budgetUsedPercentage = budgetUsedPercentage;
		}

		public Boolean getIsOverBudget() {
			return isOverBudget;
		}

		public void setIsOverBudget(Boolean isOverBudget) {
			this.isOverBudget = isOverBudget;
		}
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private String username;
        private String fullName;
        private BigDecimal totalAmount;
        private Long expenseCount;
        private BigDecimal averageAmount;
        private Long pendingApprovals;
        private Long approvedExpenses;
        private Long rejectedExpenses;

        // Constructor
        public UserSummary(String username, String fullName, BigDecimal totalAmount, Long expenseCount) {
            this.username = username;
            this.fullName = fullName;
            this.totalAmount = totalAmount;
            this.expenseCount = expenseCount;
            this.averageAmount = expenseCount > 0 ? 
                totalAmount.divide(BigDecimal.valueOf(expenseCount), 2, java.math.RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        }

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public BigDecimal getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
		}

		public Long getExpenseCount() {
			return expenseCount;
		}

		public void setExpenseCount(Long expenseCount) {
			this.expenseCount = expenseCount;
		}

		public BigDecimal getAverageAmount() {
			return averageAmount;
		}

		public void setAverageAmount(BigDecimal averageAmount) {
			this.averageAmount = averageAmount;
		}

		public Long getPendingApprovals() {
			return pendingApprovals;
		}

		public void setPendingApprovals(Long pendingApprovals) {
			this.pendingApprovals = pendingApprovals;
		}

		public Long getApprovedExpenses() {
			return approvedExpenses;
		}

		public void setApprovedExpenses(Long approvedExpenses) {
			this.approvedExpenses = approvedExpenses;
		}

		public Long getRejectedExpenses() {
			return rejectedExpenses;
		}

		public void setRejectedExpenses(Long rejectedExpenses) {
			this.rejectedExpenses = rejectedExpenses;
		}
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamSummary {
        private String teamName;
        private BigDecimal totalAmount;
        private Long expenseCount;
        private Integer memberCount;
        private BigDecimal averagePerMember;
        private BigDecimal budgetAmount;
        private BigDecimal budgetUsedPercentage;

        // Constructor
        public TeamSummary(String teamName, BigDecimal totalAmount, Long expenseCount, Integer memberCount) {
            this.teamName = teamName;
            this.totalAmount = totalAmount;
            this.expenseCount = expenseCount;
            this.memberCount = memberCount;
            this.averagePerMember = memberCount > 0 ? 
                totalAmount.divide(BigDecimal.valueOf(memberCount), 2, java.math.RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        }

		public String getTeamName() {
			return teamName;
		}

		public void setTeamName(String teamName) {
			this.teamName = teamName;
		}

		public BigDecimal getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
		}

		public Long getExpenseCount() {
			return expenseCount;
		}

		public void setExpenseCount(Long expenseCount) {
			this.expenseCount = expenseCount;
		}

		public Integer getMemberCount() {
			return memberCount;
		}

		public void setMemberCount(Integer memberCount) {
			this.memberCount = memberCount;
		}

		public BigDecimal getAveragePerMember() {
			return averagePerMember;
		}

		public void setAveragePerMember(BigDecimal averagePerMember) {
			this.averagePerMember = averagePerMember;
		}

		public BigDecimal getBudgetAmount() {
			return budgetAmount;
		}

		public void setBudgetAmount(BigDecimal budgetAmount) {
			this.budgetAmount = budgetAmount;
		}

		public BigDecimal getBudgetUsedPercentage() {
			return budgetUsedPercentage;
		}

		public void setBudgetUsedPercentage(BigDecimal budgetUsedPercentage) {
			this.budgetUsedPercentage = budgetUsedPercentage;
		}

		
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySummary {
        private String month;
        private Integer year;
        private BigDecimal totalAmount;
        private Long expenseCount;
        private BigDecimal budgetAmount;
        private BigDecimal budgetUsedPercentage;
        private Integer approvalCount;
        private Double averageApprovalTime;

        // Constructor
        public MonthlySummary(String month, Integer year, BigDecimal totalAmount, Long expenseCount) {
            this.month = month;
            this.year = year;
            this.totalAmount = totalAmount;
            this.expenseCount = expenseCount;
        }

		public String getMonth() {
			return month;
		}

		public void setMonth(String month) {
			this.month = month;
		}

		public Integer getYear() {
			return year;
		}

		public void setYear(Integer year) {
			this.year = year;
		}

		public BigDecimal getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
		}

		public Long getExpenseCount() {
			return expenseCount;
		}

		public void setExpenseCount(Long expenseCount) {
			this.expenseCount = expenseCount;
		}

		public BigDecimal getBudgetAmount() {
			return budgetAmount;
		}

		public void setBudgetAmount(BigDecimal budgetAmount) {
			this.budgetAmount = budgetAmount;
		}

		public BigDecimal getBudgetUsedPercentage() {
			return budgetUsedPercentage;
		}

		public void setBudgetUsedPercentage(BigDecimal budgetUsedPercentage) {
			this.budgetUsedPercentage = budgetUsedPercentage;
		}

		public Integer getApprovalCount() {
			return approvalCount;
		}

		public void setApprovalCount(Integer approvalCount) {
			this.approvalCount = approvalCount;
		}

		public Double getAverageApprovalTime() {
			return averageApprovalTime;
		}

		public void setAverageApprovalTime(Double averageApprovalTime) {
			this.averageApprovalTime = averageApprovalTime;
		}

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportChart {
        private String chartType; // PIE, BAR, LINE, AREA
        private String title;
        private String xAxisLabel;
        private String yAxisLabel;
        private List<ChartDataPoint> dataPoints;
        private Map<String, Object> config;

        // Constructor
        public ReportChart(String chartType, String title, List<ChartDataPoint> dataPoints) {
            this.chartType = chartType;
            this.title = title;
            this.dataPoints = dataPoints;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataPoint {
        private String label;
        private BigDecimal value;
        private String color;
        private Map<String, Object> additionalData;

        // Constructor
        public ChartDataPoint(String label, BigDecimal value) {
            this.label = label;
            this.value = value;
        }

        // Constructor with color
        public ChartDataPoint(String label, BigDecimal value, String color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public BigDecimal getValue() {
			return value;
		}

		public void setValue(BigDecimal value) {
			this.value = value;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}

		public Map<String, Object> getAdditionalData() {
			return additionalData;
		}

		public void setAdditionalData(Map<String, Object> additionalData) {
			this.additionalData = additionalData;
		}
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledReportInfo {
        private Long scheduleId;
        private String reportName;
        private String frequency;
        private Boolean isActive;
        private List<String> emailRecipients;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastGenerated;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime nextScheduled;
        
        private String status;
        private Integer executionCount;
        private String description;

        // Constructor
        public ScheduledReportInfo(Long scheduleId, String reportName, String frequency, 
                                 Boolean isActive, LocalDateTime nextScheduled) {
            this.scheduleId = scheduleId;
            this.reportName = reportName;
            this.frequency = frequency;
            this.isActive = isActive;
            this.nextScheduled = nextScheduled;
            this.executionCount = 0;
        }

        
		public ScheduledReportInfo() {
			
		}


		public Long getScheduleId() {
			return scheduleId;
		}

		public void setScheduleId(Long scheduleId) {
			this.scheduleId = scheduleId;
		}

		public String getReportName() {
			return reportName;
		}

		public void setReportName(String reportName) {
			this.reportName = reportName;
		}

		public String getFrequency() {
			return frequency;
		}

		public void setFrequency(String frequency) {
			this.frequency = frequency;
		}

		public Boolean getIsActive() {
			return isActive;
		}

		public void setIsActive(Boolean isActive) {
			this.isActive = isActive;
		}

		public List<String> getEmailRecipients() {
			return emailRecipients;
		}

		public void setEmailRecipients(List<String> emailRecipients) {
			this.emailRecipients = emailRecipients;
		}

		public LocalDateTime getLastGenerated() {
			return lastGenerated;
		}

		public void setLastGenerated(LocalDateTime lastGenerated) {
			this.lastGenerated = lastGenerated;
		}

		public LocalDateTime getNextScheduled() {
			return nextScheduled;
		}

		public void setNextScheduled(LocalDateTime nextScheduled) {
			this.nextScheduled = nextScheduled;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public Integer getExecutionCount() {
			return executionCount;
		}

		public void setExecutionCount(Integer executionCount) {
			this.executionCount = executionCount;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
    }

    // NEW: Custom Analysis Classes for CUSTOM report type

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendAnalysis {
        private String category;
        private String timePeriod; // MONTHLY, QUARTERLY, YEARLY
        private BigDecimal amount;
        private Long count;
        private BigDecimal previousAmount;
        private BigDecimal growthRate;
        private String trend; // INCREASING, DECREASING, STABLE
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate periodStartDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate periodEndDate;

        // Constructor
        public TrendAnalysis(String category, BigDecimal amount, Long count) {
            this.category = category;
            this.amount = amount;
            this.count = count;
        }

        // Constructor with trend data
        public TrendAnalysis(String category, BigDecimal amount, Long count, 
                           BigDecimal previousAmount, String trend) {
            this.category = category;
            this.amount = amount;
            this.count = count;
            this.previousAmount = previousAmount;
            this.trend = trend;
            
            // Calculate growth rate
            if (previousAmount != null && previousAmount.compareTo(BigDecimal.ZERO) != 0) {
                this.growthRate = amount.subtract(previousAmount)
                    .divide(previousAmount, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            } else {
                this.growthRate = BigDecimal.ZERO;
            }
        }

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getTimePeriod() {
			return timePeriod;
		}

		public void setTimePeriod(String timePeriod) {
			this.timePeriod = timePeriod;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public Long getCount() {
			return count;
		}

		public void setCount(Long count) {
			this.count = count;
		}

		public BigDecimal getPreviousAmount() {
			return previousAmount;
		}

		public void setPreviousAmount(BigDecimal previousAmount) {
			this.previousAmount = previousAmount;
		}

		public BigDecimal getGrowthRate() {
			return growthRate;
		}

		public void setGrowthRate(BigDecimal growthRate) {
			this.growthRate = growthRate;
		}

		public String getTrend() {
			return trend;
		}

		public void setTrend(String trend) {
			this.trend = trend;
		}

		public LocalDate getPeriodStartDate() {
			return periodStartDate;
		}

		public void setPeriodStartDate(LocalDate periodStartDate) {
			this.periodStartDate = periodStartDate;
		}

		public LocalDate getPeriodEndDate() {
			return periodEndDate;
		}

		public void setPeriodEndDate(LocalDate periodEndDate) {
			this.periodEndDate = periodEndDate;
		}

		
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonAnalysis {
        private String period; // CURRENT_MONTH, PREVIOUS_MONTH, YEAR_OVER_YEAR, etc.
        private String category;
        private BigDecimal currentAmount;
        private BigDecimal comparisonAmount;
        private BigDecimal variance;
        private BigDecimal variancePercentage;
        private String comparisonType; // BETTER, WORSE, SAME
        private String description;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate currentPeriodStart;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate currentPeriodEnd;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate comparisonPeriodStart;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate comparisonPeriodEnd;

        // Constructor
        public ComparisonAnalysis(String period, String category, 
                                BigDecimal currentAmount, BigDecimal comparisonAmount) {
            this.period = period;
            this.category = category;
            this.currentAmount = currentAmount;
            this.comparisonAmount = comparisonAmount;
            
            // Calculate variance
            this.variance = currentAmount.subtract(comparisonAmount);
            
            // Calculate variance percentage
            if (comparisonAmount.compareTo(BigDecimal.ZERO) != 0) {
                this.variancePercentage = variance.divide(comparisonAmount, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            } else {
                this.variancePercentage = BigDecimal.ZERO;
            }
            
            // Determine comparison type
            if (variance.compareTo(BigDecimal.ZERO) > 0) {
                this.comparisonType = "INCREASED";
            } else if (variance.compareTo(BigDecimal.ZERO) < 0) {
                this.comparisonType = "DECREASED";
            } else {
                this.comparisonType = "SAME";
            }
        }

		public String getPeriod() {
			return period;
		}

		public void setPeriod(String period) {
			this.period = period;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public BigDecimal getCurrentAmount() {
			return currentAmount;
		}

		public void setCurrentAmount(BigDecimal currentAmount) {
			this.currentAmount = currentAmount;
		}

		public BigDecimal getComparisonAmount() {
			return comparisonAmount;
		}

		public void setComparisonAmount(BigDecimal comparisonAmount) {
			this.comparisonAmount = comparisonAmount;
		}

		public BigDecimal getVariance() {
			return variance;
		}

		public void setVariance(BigDecimal variance) {
			this.variance = variance;
		}

		public BigDecimal getVariancePercentage() {
			return variancePercentage;
		}

		public void setVariancePercentage(BigDecimal variancePercentage) {
			this.variancePercentage = variancePercentage;
		}

		public String getComparisonType() {
			return comparisonType;
		}

		public void setComparisonType(String comparisonType) {
			this.comparisonType = comparisonType;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public LocalDate getCurrentPeriodStart() {
			return currentPeriodStart;
		}

		public void setCurrentPeriodStart(LocalDate currentPeriodStart) {
			this.currentPeriodStart = currentPeriodStart;
		}

		public LocalDate getCurrentPeriodEnd() {
			return currentPeriodEnd;
		}

		public void setCurrentPeriodEnd(LocalDate currentPeriodEnd) {
			this.currentPeriodEnd = currentPeriodEnd;
		}

		public LocalDate getComparisonPeriodStart() {
			return comparisonPeriodStart;
		}

		public void setComparisonPeriodStart(LocalDate comparisonPeriodStart) {
			this.comparisonPeriodStart = comparisonPeriodStart;
		}

		public LocalDate getComparisonPeriodEnd() {
			return comparisonPeriodEnd;
		}

		public void setComparisonPeriodEnd(LocalDate comparisonPeriodEnd) {
			this.comparisonPeriodEnd = comparisonPeriodEnd;
		}

		
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianceAnalysis {
        private String category;
        private BigDecimal budgetedAmount;
        private BigDecimal actualAmount;
        private BigDecimal variance;
        private BigDecimal variancePercentage;
        private String status; // UNDER_BUDGET, OVER_BUDGET, ON_BUDGET
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String recommendation;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate analysisDate;

        // Constructor
        public VarianceAnalysis(String category, BigDecimal budgetedAmount, BigDecimal actualAmount) {
            this.category = category;
            this.budgetedAmount = budgetedAmount;
            this.actualAmount = actualAmount;
            this.analysisDate = LocalDate.now();
            
            // Calculate variance
            this.variance = actualAmount.subtract(budgetedAmount);
            
            // Calculate variance percentage
            if (budgetedAmount.compareTo(BigDecimal.ZERO) != 0) {
                this.variancePercentage = variance.divide(budgetedAmount, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            } else {
                this.variancePercentage = BigDecimal.ZERO;
            }
            
            // Determine status
            if (variance.compareTo(BigDecimal.ZERO) > 0) {
                this.status = "OVER_BUDGET";
            } else if (variance.compareTo(BigDecimal.ZERO) < 0) {
                this.status = "UNDER_BUDGET";
            } else {
                this.status = "ON_BUDGET";
            }
            
            // Determine severity based on variance percentage
            BigDecimal absVariancePercentage = variancePercentage.abs();
            if (absVariancePercentage.compareTo(BigDecimal.valueOf(25)) >= 0) {
                this.severity = "CRITICAL";
            } else if (absVariancePercentage.compareTo(BigDecimal.valueOf(15)) >= 0) {
                this.severity = "HIGH";
            } else if (absVariancePercentage.compareTo(BigDecimal.valueOf(5)) >= 0) {
                this.severity = "MEDIUM";
            } else {
                this.severity = "LOW";
            }
        }

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public BigDecimal getBudgetedAmount() {
			return budgetedAmount;
		}

		public void setBudgetedAmount(BigDecimal budgetedAmount) {
			this.budgetedAmount = budgetedAmount;
		}

		public BigDecimal getActualAmount() {
			return actualAmount;
		}

		public void setActualAmount(BigDecimal actualAmount) {
			this.actualAmount = actualAmount;
		}

		public BigDecimal getVariance() {
			return variance;
		}

		public void setVariance(BigDecimal variance) {
			this.variance = variance;
		}

		public BigDecimal getVariancePercentage() {
			return variancePercentage;
		}

		public void setVariancePercentage(BigDecimal variancePercentage) {
			this.variancePercentage = variancePercentage;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getSeverity() {
			return severity;
		}

		public void setSeverity(String severity) {
			this.severity = severity;
		}

		public String getRecommendation() {
			return recommendation;
		}

		public void setRecommendation(String recommendation) {
			this.recommendation = recommendation;
		}

		public LocalDate getAnalysisDate() {
			return analysisDate;
		}

		public void setAnalysisDate(LocalDate analysisDate) {
			this.analysisDate = analysisDate;
		}

		
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastAnalysis {
        private String category;
        private String forecastPeriod; // NEXT_MONTH, NEXT_QUARTER, NEXT_YEAR
        private BigDecimal projectedAmount;
        private BigDecimal confidenceLevel; // Percentage confidence in the forecast
        private String trend; // INCREASING, DECREASING, STABLE
        private String methodology; // LINEAR, EXPONENTIAL, SEASONAL, ML_BASED
        private List<BigDecimal> historicalData;
        private Map<String, Object> forecastParameters;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate forecastStartDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate forecastEndDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime generatedAt;

        // Constructor
        public ForecastAnalysis(String category, String forecastPeriod, 
                              BigDecimal projectedAmount, BigDecimal confidenceLevel) {
            this.category = category;
            this.forecastPeriod = forecastPeriod;
            this.projectedAmount = projectedAmount;
            this.confidenceLevel = confidenceLevel;
            this.generatedAt = LocalDateTime.now();
            this.methodology = "LINEAR"; // Default methodology
        }

        // Constructor with trend
        public ForecastAnalysis(String category, String forecastPeriod, 
                              BigDecimal projectedAmount, BigDecimal confidenceLevel, String trend) {
            this(category, forecastPeriod, projectedAmount, confidenceLevel);
            this.trend = trend;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialMetrics {
        private String metricName;
        private String metricType; // RATIO, PERCENTAGE, AMOUNT, COUNT
        private BigDecimal value;
        private String unit; // USD, PERCENTAGE, DAYS, etc.
        private String category;
        private String description;
        private String benchmark; // Industry benchmark or target value
        private String status; // GOOD, WARNING, CRITICAL
        private Map<String, Object> additionalData;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate calculationDate;

        // Constructor
        public FinancialMetrics(String metricName, String metricType, 
                              BigDecimal value, String unit) {
            this.metricName = metricName;
            this.metricType = metricType;
            this.value = value;
            this.unit = unit;
            this.calculationDate = LocalDate.now();
        }

        // Constructor with status
        public FinancialMetrics(String metricName, String metricType, 
                              BigDecimal value, String unit, String status) {
            this(metricName, metricType, value, unit);
            this.status = status;
        }

        // Constructor with full details
        public FinancialMetrics(String metricName, String metricType, 
                              BigDecimal value, String unit, String category, 
                              String description, String status) {
            this(metricName, metricType, value, unit, status);
            this.category = category;
            this.description = description;
        }

		public String getMetricName() {
			return metricName;
		}

		public void setMetricName(String metricName) {
			this.metricName = metricName;
		}

		public String getMetricType() {
			return metricType;
		}

		public void setMetricType(String metricType) {
			this.metricType = metricType;
		}

		public BigDecimal getValue() {
			return value;
		}

		public void setValue(BigDecimal value) {
			this.value = value;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getBenchmark() {
			return benchmark;
		}

		public void setBenchmark(String benchmark) {
			this.benchmark = benchmark;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public Map<String, Object> getAdditionalData() {
			return additionalData;
		}

		public void setAdditionalData(Map<String, Object> additionalData) {
			this.additionalData = additionalData;
		}

		public LocalDate getCalculationDate() {
			return calculationDate;
		}

		public void setCalculationDate(LocalDate calculationDate) {
			this.calculationDate = calculationDate;
		}

	}

    // NEW: Performance Metrics for advanced reporting
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private String kpiName;
        private BigDecimal currentValue;
        private BigDecimal targetValue;
        private BigDecimal previousValue;
        private String trend; // IMPROVING, DECLINING, STABLE
        private BigDecimal achievementPercentage;
        private String status; // EXCEEDS, MEETS, BELOW, CRITICAL
        private String frequency; // DAILY, WEEKLY, MONTHLY, QUARTERLY
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate measurementDate;

        // Constructor
        public PerformanceMetrics(String kpiName, BigDecimal currentValue, BigDecimal targetValue) {
            this.kpiName = kpiName;
            this.currentValue = currentValue;
            this.targetValue = targetValue;
            this.measurementDate = LocalDate.now();
            
            // Calculate achievement percentage
            if (targetValue.compareTo(BigDecimal.ZERO) != 0) {
                this.achievementPercentage = currentValue.divide(targetValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            } else {
                this.achievementPercentage = BigDecimal.ZERO;
            }
            
            // Determine status based on achievement
            if (achievementPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                this.status = "MEETS";
                if (achievementPercentage.compareTo(BigDecimal.valueOf(110)) >= 0) {
                    this.status = "EXCEEDS";
                }
            } else if (achievementPercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
                this.status = "BELOW";
            } else {
                this.status = "CRITICAL";
            }
        }
    }

    // NEW: Risk Analysis for financial reporting
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAnalysis {
        private String riskCategory;
        private String riskType; // BUDGET_OVERRUN, CASH_FLOW, COMPLIANCE, FRAUD
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private BigDecimal probabilityScore; // 0-100
        private BigDecimal impactScore; // 0-100
        private BigDecimal riskScore; // Calculated from probability and impact
        private String description;
        private List<String> mitigationStrategies;
        private String owner; // Risk owner
        private String status; // IDENTIFIED, ASSESSED, MITIGATED, CLOSED
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate identifiedDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate targetMitigationDate;

        // Constructor
        public RiskAnalysis(String riskCategory, String riskType, 
                          BigDecimal probabilityScore, BigDecimal impactScore) {
            this.riskCategory = riskCategory;
            this.riskType = riskType;
            this.probabilityScore = probabilityScore;
            this.impactScore = impactScore;
            this.identifiedDate = LocalDate.now();
            this.status = "IDENTIFIED";
            
            // Calculate risk score (probability × impact / 100)
            this.riskScore = probabilityScore.multiply(impactScore)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            
            // Determine risk level based on risk score
            if (riskScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
                this.riskLevel = "CRITICAL";
            } else if (riskScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
                this.riskLevel = "HIGH";
            } else if (riskScore.compareTo(BigDecimal.valueOf(30)) >= 0) {
                this.riskLevel = "MEDIUM";
            } else {
                this.riskLevel = "LOW";
            }
        }
    }

    // NEW: Audit Trail for compliance reporting
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditTrail {
        private String auditId;
        private String entityType; // EXPENSE, BUDGET, APPROVAL, USER
        private Long entityId;
        private String action; // CREATE, UPDATE, DELETE, APPROVE, REJECT
        private String performedBy;
        private String oldValue;
        private String newValue;
        private String reason;
        private String ipAddress;
        private String userAgent;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;

        // Constructor
        public AuditTrail(String entityType, Long entityId, String action, String performedBy) {
            this.auditId = "AUDIT-" + System.currentTimeMillis();
            this.entityType = entityType;
            this.entityId = entityId;
            this.action = action;
            this.performedBy = performedBy;
            this.timestamp = LocalDateTime.now();
        }
    }

    public String getReportId() {
    	return reportId;
    }

    public void setReportId(String reportId) {
    	this.reportId = reportId;
    }

    public String getReportType() {
    	return reportType;
    }

    public void setReportType(String reportType) {
    	this.reportType = reportType;
    }

    public String getReportName() {
    	return reportName;
    }

    public void setReportName(String reportName) {
    	this.reportName = reportName;
    }

    public String getDescription() {
    	return description;
    }

    public void setDescription(String description) {
    	this.description = description;
    }

    public String getFormat() {
    	return format;
    }

    public void setFormat(String format) {
    	this.format = format;
    }

    public String getStatus() {
    	return status;
    }

    public void setStatus(String status) {
    	this.status = status;
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

    public LocalDateTime getGeneratedAt() {
    	return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
    	this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
    	return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
    	this.generatedBy = generatedBy;
    }

    public Long getFileSizeBytes() {
    	return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
    	this.fileSizeBytes = fileSizeBytes;
    }

    public String getDownloadUrl() {
    	return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
    	this.downloadUrl = downloadUrl;
    }

    public ReportSummary getSummary() {
    	return summary;
    }

    public void setSummary(ReportSummary summary) {
    	this.summary = summary;
    }

    public ReportData getData() {
    	return data;
    }

    public void setData(ReportData data) {
    	this.data = data;
    }

    public List<ReportChart> getCharts() {
    	return charts;
    }

    public void setCharts(List<ReportChart> charts) {
    	this.charts = charts;
    }

    public LocalDateTime getExpiresAt() {
    	return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
    	this.expiresAt = expiresAt;
    }
 }