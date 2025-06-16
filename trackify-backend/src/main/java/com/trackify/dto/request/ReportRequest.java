package com.trackify.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    @NotBlank(message = "Report type is required")
    @Pattern(regexp = "^(EXPENSE|BUDGET|APPROVAL|CATEGORY|TEAM|USER|FINANCIAL)$", 
            message = "Invalid report type")
    private String reportType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
    private String format = "PDF";

    private String reportName;

    private String description;

    private List<Long> userIds;

    private List<Long> teamIds;

    private List<Long> categoryIds;

    private List<String> expenseStatuses;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private String currency;

    private Boolean includeCharts = true;

    private Boolean includeDetails = true;

    private Boolean includeSummary = true;

    private String groupBy; // CATEGORY, TEAM, USER, DATE, MONTH

    private String sortBy = "date"; // date, amount, category, user

    private String sortDirection = "DESC"; // ASC, DESC

    private Boolean emailReport = false;

    private List<String> emailRecipients;

    private Boolean scheduleReport = false;

    private String scheduleFrequency; // DAILY, WEEKLY, MONTHLY, QUARTERLY

    // Constructor for basic expense report
    public ReportRequest(String reportType, LocalDate startDate, LocalDate endDate, String format) {
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.format = format;
        this.includeCharts = true;
        this.includeDetails = true;
        this.includeSummary = true;
        this.sortBy = "date";
        this.sortDirection = "DESC";
        this.emailReport = false;
        this.scheduleReport = false;
    }
    
    

    public String getReportType() {
		return reportType;
	}

	public void setReportType(String reportType) {
		this.reportType = reportType;
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

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
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

	public List<Long> getUserIds() {
		return userIds;
	}

	public void setUserIds(List<Long> userIds) {
		this.userIds = userIds;
	}

	public List<Long> getTeamIds() {
		return teamIds;
	}

	public void setTeamIds(List<Long> teamIds) {
		this.teamIds = teamIds;
	}

	public List<Long> getCategoryIds() {
		return categoryIds;
	}

	public void setCategoryIds(List<Long> categoryIds) {
		this.categoryIds = categoryIds;
	}

	public List<String> getExpenseStatuses() {
		return expenseStatuses;
	}

	public void setExpenseStatuses(List<String> expenseStatuses) {
		this.expenseStatuses = expenseStatuses;
	}

	public BigDecimal getMinAmount() {
		return minAmount;
	}

	public void setMinAmount(BigDecimal minAmount) {
		this.minAmount = minAmount;
	}

	public BigDecimal getMaxAmount() {
		return maxAmount;
	}

	public void setMaxAmount(BigDecimal maxAmount) {
		this.maxAmount = maxAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Boolean getIncludeCharts() {
		return includeCharts;
	}

	public void setIncludeCharts(Boolean includeCharts) {
		this.includeCharts = includeCharts;
	}

	public Boolean getIncludeDetails() {
		return includeDetails;
	}

	public void setIncludeDetails(Boolean includeDetails) {
		this.includeDetails = includeDetails;
	}

	public Boolean getIncludeSummary() {
		return includeSummary;
	}

	public void setIncludeSummary(Boolean includeSummary) {
		this.includeSummary = includeSummary;
	}

	public String getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}

	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public String getSortDirection() {
		return sortDirection;
	}

	public void setSortDirection(String sortDirection) {
		this.sortDirection = sortDirection;
	}

	public Boolean getEmailReport() {
		return emailReport;
	}

	public void setEmailReport(Boolean emailReport) {
		this.emailReport = emailReport;
	}

	public List<String> getEmailRecipients() {
		return emailRecipients;
	}

	public void setEmailRecipients(List<String> emailRecipients) {
		this.emailRecipients = emailRecipients;
	}

	public Boolean getScheduleReport() {
		return scheduleReport;
	}

	public void setScheduleReport(Boolean scheduleReport) {
		this.scheduleReport = scheduleReport;
	}

	public String getScheduleFrequency() {
		return scheduleFrequency;
	}

	public void setScheduleFrequency(String scheduleFrequency) {
		this.scheduleFrequency = scheduleFrequency;
	}



	@Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseReportRequest {
        
        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @Pattern(regexp = "^(PDF|CSV|XLSX)$", message = "Invalid format")
        private String format = "PDF";

        private List<Long> categoryIds;

        private List<String> expenseStatuses;

        private BigDecimal minAmount;

        private BigDecimal maxAmount;

        private String groupBy = "CATEGORY"; // CATEGORY, DATE, MONTH, USER

        private Boolean includeReceipts = false;

        private Boolean includeComments = false;

        private Boolean includeApprovalHistory = false;

        // Constructor
        public ExpenseReportRequest(LocalDate startDate, LocalDate endDate, String format) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.format = format;
            this.groupBy = "CATEGORY";
            this.includeReceipts = false;
            this.includeComments = false;
            this.includeApprovalHistory = false;
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

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public List<Long> getCategoryIds() {
			return categoryIds;
		}

		public void setCategoryIds(List<Long> categoryIds) {
			this.categoryIds = categoryIds;
		}

		public List<String> getExpenseStatuses() {
			return expenseStatuses;
		}

		public void setExpenseStatuses(List<String> expenseStatuses) {
			this.expenseStatuses = expenseStatuses;
		}

		public BigDecimal getMinAmount() {
			return minAmount;
		}

		public void setMinAmount(BigDecimal minAmount) {
			this.minAmount = minAmount;
		}

		public BigDecimal getMaxAmount() {
			return maxAmount;
		}

		public void setMaxAmount(BigDecimal maxAmount) {
			this.maxAmount = maxAmount;
		}

		public String getGroupBy() {
			return groupBy;
		}

		public void setGroupBy(String groupBy) {
			this.groupBy = groupBy;
		}

		public Boolean getIncludeReceipts() {
			return includeReceipts;
		}

		public void setIncludeReceipts(Boolean includeReceipts) {
			this.includeReceipts = includeReceipts;
		}

		public Boolean getIncludeComments() {
			return includeComments;
		}

		public void setIncludeComments(Boolean includeComments) {
			this.includeComments = includeComments;
		}

		public Boolean getIncludeApprovalHistory() {
			return includeApprovalHistory;
		}

		public void setIncludeApprovalHistory(Boolean includeApprovalHistory) {
			this.includeApprovalHistory = includeApprovalHistory;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetReportRequest {
        
        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @Pattern(regexp = "^(PDF|CSV|XLSX)$", message = "Invalid format")
        private String format = "PDF";

        private List<Long> categoryIds;

        private List<Long> teamIds;

        private Boolean includeExpiredBudgets = false;

        private Boolean includeOverBudgets = true;

        private Boolean includeProjections = true;

        private String groupBy = "CATEGORY"; // CATEGORY, TEAM, MONTH

        // Constructor
        public BudgetReportRequest(LocalDate startDate, LocalDate endDate, String format) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.format = format;
            this.includeExpiredBudgets = false;
            this.includeOverBudgets = true;
            this.includeProjections = true;
            this.groupBy = "CATEGORY";
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

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public List<Long> getCategoryIds() {
			return categoryIds;
		}

		public void setCategoryIds(List<Long> categoryIds) {
			this.categoryIds = categoryIds;
		}

		public List<Long> getTeamIds() {
			return teamIds;
		}

		public void setTeamIds(List<Long> teamIds) {
			this.teamIds = teamIds;
		}

		public Boolean getIncludeExpiredBudgets() {
			return includeExpiredBudgets;
		}

		public void setIncludeExpiredBudgets(Boolean includeExpiredBudgets) {
			this.includeExpiredBudgets = includeExpiredBudgets;
		}

		public Boolean getIncludeOverBudgets() {
			return includeOverBudgets;
		}

		public void setIncludeOverBudgets(Boolean includeOverBudgets) {
			this.includeOverBudgets = includeOverBudgets;
		}

		public Boolean getIncludeProjections() {
			return includeProjections;
		}

		public void setIncludeProjections(Boolean includeProjections) {
			this.includeProjections = includeProjections;
		}

		public String getGroupBy() {
			return groupBy;
		}

		public void setGroupBy(String groupBy) {
			this.groupBy = groupBy;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalReportRequest {
        
        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @Pattern(regexp = "^(PDF|CSV|XLSX)$", message = "Invalid format")
        private String format = "PDF";

        private List<Long> approverIds;

        private List<String> approvalStatuses;

        private Boolean includeTimings = true;

        private Boolean includeEscalations = true;

        private Boolean includeComments = false;

        private String groupBy = "APPROVER"; // APPROVER, STATUS, MONTH

        // Constructor
        public ApprovalReportRequest(LocalDate startDate, LocalDate endDate, String format) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.format = format;
            this.includeTimings = true;
            this.includeEscalations = true;
            this.includeComments = false;
            this.groupBy = "APPROVER";
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

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public List<Long> getApproverIds() {
			return approverIds;
		}

		public void setApproverIds(List<Long> approverIds) {
			this.approverIds = approverIds;
		}

		public List<String> getApprovalStatuses() {
			return approvalStatuses;
		}

		public void setApprovalStatuses(List<String> approvalStatuses) {
			this.approvalStatuses = approvalStatuses;
		}

		public Boolean getIncludeTimings() {
			return includeTimings;
		}

		public void setIncludeTimings(Boolean includeTimings) {
			this.includeTimings = includeTimings;
		}

		public Boolean getIncludeEscalations() {
			return includeEscalations;
		}

		public void setIncludeEscalations(Boolean includeEscalations) {
			this.includeEscalations = includeEscalations;
		}

		public Boolean getIncludeComments() {
			return includeComments;
		}

		public void setIncludeComments(Boolean includeComments) {
			this.includeComments = includeComments;
		}

		public String getGroupBy() {
			return groupBy;
		}

		public void setGroupBy(String groupBy) {
			this.groupBy = groupBy;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamReportRequest {
        
        @NotNull(message = "Team ID is required")
        private Long teamId;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @Pattern(regexp = "^(PDF|CSV|XLSX)$", message = "Invalid format")
        private String format = "PDF";

        private Boolean includeMemberBreakdown = true;

        private Boolean includeBudgetAnalysis = true;

        private Boolean includeApprovalMetrics = true;

        private Boolean includeExpenseDetails = false;

        private String groupBy = "MEMBER"; // MEMBER, CATEGORY, MONTH

        // Constructor
        public TeamReportRequest(Long teamId, LocalDate startDate, LocalDate endDate, String format) {
            this.teamId = teamId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.format = format;
            this.includeMemberBreakdown = true;
            this.includeBudgetAnalysis = true;
            this.includeApprovalMetrics = true;
            this.includeExpenseDetails = false;
            this.groupBy = "MEMBER";
        }

		public Long getTeamId() {
			return teamId;
		}

		public void setTeamId(Long teamId) {
			this.teamId = teamId;
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

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public Boolean getIncludeMemberBreakdown() {
			return includeMemberBreakdown;
		}

		public void setIncludeMemberBreakdown(Boolean includeMemberBreakdown) {
			this.includeMemberBreakdown = includeMemberBreakdown;
		}

		public Boolean getIncludeBudgetAnalysis() {
			return includeBudgetAnalysis;
		}

		public void setIncludeBudgetAnalysis(Boolean includeBudgetAnalysis) {
			this.includeBudgetAnalysis = includeBudgetAnalysis;
		}

		public Boolean getIncludeApprovalMetrics() {
			return includeApprovalMetrics;
		}

		public void setIncludeApprovalMetrics(Boolean includeApprovalMetrics) {
			this.includeApprovalMetrics = includeApprovalMetrics;
		}

		public Boolean getIncludeExpenseDetails() {
			return includeExpenseDetails;
		}

		public void setIncludeExpenseDetails(Boolean includeExpenseDetails) {
			this.includeExpenseDetails = includeExpenseDetails;
		}

		public String getGroupBy() {
			return groupBy;
		}

		public void setGroupBy(String groupBy) {
			this.groupBy = groupBy;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledReportRequest {
        
        @NotBlank(message = "Report name is required")
        private String reportName;

        @NotNull(message = "Report configuration is required")
        private ReportRequest reportConfig;

        @NotBlank(message = "Schedule frequency is required")
        @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|QUARTERLY)$", message = "Invalid frequency")
        private String frequency;

        @NotEmpty(message = "Email recipients are required")
        private List<String> emailRecipients;

        private Boolean isActive = true;

        private String description;

        // Constructor
        public ScheduledReportRequest(String reportName, ReportRequest reportConfig, 
                                     String frequency, List<String> emailRecipients) {
            this.reportName = reportName;
            this.reportConfig = reportConfig;
            this.frequency = frequency;
            this.emailRecipients = emailRecipients;
            this.isActive = true;
        }

		public String getReportName() {
			return reportName;
		}

		public void setReportName(String reportName) {
			this.reportName = reportName;
		}

		public ReportRequest getReportConfig() {
			return reportConfig;
		}

		public void setReportConfig(ReportRequest reportConfig) {
			this.reportConfig = reportConfig;
		}

		public String getFrequency() {
			return frequency;
		}

		public void setFrequency(String frequency) {
			this.frequency = frequency;
		}

		public List<String> getEmailRecipients() {
			return emailRecipients;
		}

		public void setEmailRecipients(List<String> emailRecipients) {
			this.emailRecipients = emailRecipients;
		}

		public Boolean getIsActive() {
			return isActive;
		}

		public void setIsActive(Boolean isActive) {
			this.isActive = isActive;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
        
        
    }
}