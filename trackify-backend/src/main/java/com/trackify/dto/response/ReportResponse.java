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