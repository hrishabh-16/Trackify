package com.trackify.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

	@NotBlank(message = "Report type is required")
	@Pattern(regexp = "^(EXPENSE|BUDGET|APPROVAL|CATEGORY|TEAM|USER|FINANCIAL|CUSTOM)$", message = "Invalid report type")
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
	private Map<String, Object> customParameters;
	private String currency;
	private Boolean includeCharts = true;
	private Boolean includeDetails = true;
	private Boolean includeSummary = true;
	private String groupBy;
	private String sortBy = "date";
	private String sortDirection = "DESC";
	private Boolean emailReport = false;
	private List<String> emailRecipients;
	private Boolean scheduleReport = false;
	private String scheduleFrequency;

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

	public ReportRequest() {
		
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ExpenseReportRequest {

		@NotNull(message = "Start date is required")
		private LocalDate startDate;

		@NotNull(message = "End date is required")
		private LocalDate endDate;

		// FIXED: Updated pattern to match main class
		@Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
		private String format = "PDF";

		private List<Long> categoryIds;
		private List<String> expenseStatuses;
		private BigDecimal minAmount;
		private BigDecimal maxAmount;
		private String groupBy = "CATEGORY";
		private Boolean includeReceipts = false;
		private Boolean includeComments = false;
		private Boolean includeApprovalHistory = false;
		private Boolean includeCharts = true; // ADDED

		public ExpenseReportRequest(LocalDate startDate, LocalDate endDate, String format) {
			this.startDate = startDate;
			this.endDate = endDate;
			this.format = format;
			this.groupBy = "CATEGORY";
			this.includeReceipts = false;
			this.includeComments = false;
			this.includeApprovalHistory = false;
			this.includeCharts = true;
		}

		// Getters and setters
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

		public Boolean getIncludeCharts() {
			return includeCharts;
		}

		public void setIncludeCharts(Boolean includeCharts) {
			this.includeCharts = includeCharts;
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
		@Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
		private String format = "PDF";
		private List<Long> categoryIds;
		private List<Long> teamIds;
		private Boolean includeExpiredBudgets = false;
		private Boolean includeOverBudgets = true;
		private Boolean includeProjections = true;
		private String groupBy = "CATEGORY";

		public BudgetReportRequest(LocalDate startDate, LocalDate endDate, String format) {
			this.startDate = startDate;
			this.endDate = endDate;
			this.format = format;
			this.includeExpiredBudgets = false;
			this.includeOverBudgets = true;
			this.includeProjections = true;
			this.groupBy = "CATEGORY";
		}

		// Getters and setters
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
		@Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
		private String format = "PDF";
		private List<Long> approverIds;
		private List<String> approvalStatuses;
		private Boolean includeTimings = true;
		private Boolean includeEscalations = true;
		private Boolean includeComments = false;
		private String groupBy = "APPROVER";

		public ApprovalReportRequest(LocalDate startDate, LocalDate endDate, String format) {
			this.startDate = startDate;
			this.endDate = endDate;
			this.format = format;
			this.includeTimings = true;
			this.includeEscalations = true;
			this.includeComments = false;
			this.groupBy = "APPROVER";
		}

		// Getters and setters
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
		@Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
		private String format = "PDF";
		private Boolean includeMemberBreakdown = true;
		private Boolean includeBudgetAnalysis = true;
		private Boolean includeApprovalMetrics = true;
		private Boolean includeExpenseDetails = false;
		private String groupBy = "MEMBER";

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

		// Getters and setters
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

		public ScheduledReportRequest(String reportName, ReportRequest reportConfig, String frequency,
				List<String> emailRecipients) {
			this.reportName = reportName;
			this.reportConfig = reportConfig;
			this.frequency = frequency;
			this.emailRecipients = emailRecipients;
			this.isActive = true;
		}

		public ScheduledReportRequest() {
			
		}

		// All getters and setters
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

	// NEW: Custom Report Request for advanced analysis
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CustomReportRequest {
		@NotNull(message = "Start date is required")
		private LocalDate startDate;
		
		@NotNull(message = "End date is required")
		private LocalDate endDate;
		
		@Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
		private String format = "PDF";
		
		private String reportName;
		private String description;
		
		// Analysis type: TREND, COMPARISON, VARIANCE, SUMMARY, FORECAST
		@Pattern(regexp = "^(TREND|COMPARISON|VARIANCE|SUMMARY|FORECAST|CUSTOM)$", message = "Invalid analysis type")
		private String analysisType = "SUMMARY";
		
		// Group by options
		@Pattern(regexp = "^(CATEGORY|USER|TEAM|DATE|MONTH|QUARTER|YEAR|STATUS|AMOUNT)$", message = "Invalid group by option")
		private String groupBy = "CATEGORY";
		
		// Time period for comparison (for COMPARISON analysis)
		private String comparisonPeriod; // PREVIOUS_MONTH, PREVIOUS_QUARTER, PREVIOUS_YEAR, CUSTOM
		private LocalDate comparisonStartDate;
		private LocalDate comparisonEndDate;
		
		// Filters
		private List<Long> categoryIds;
		private List<Long> teamIds;
		private List<Long> userIds;
		private List<String> expenseStatuses;
		private BigDecimal minAmount;
		private BigDecimal maxAmount;
		
		// Analysis options
		private Boolean includeProjections = false;
		private Boolean includeVarianceAnalysis = false;
		private Boolean includeTrendAnalysis = false;
		private Boolean includeCharts = true;
		private Boolean includeDetails = true;
		private Boolean includeSummary = true;
		
		// Custom parameters for flexible analysis
		private Map<String, Object> customParameters;
		
		// Sorting and pagination
		private String sortBy = "amount";
		private String sortDirection = "DESC";
		private Integer limitResults;
		
		public CustomReportRequest(LocalDate startDate, LocalDate endDate, String format, String analysisType) {
			this.startDate = startDate;
			this.endDate = endDate;
			this.format = format;
			this.analysisType = analysisType;
			this.groupBy = "CATEGORY";
			this.includeCharts = true;
			this.includeDetails = true;
			this.includeSummary = true;
			this.sortBy = "amount";
			this.sortDirection = "DESC";
		}
	}

	// NEW: Financial Analysis Request
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class FinancialAnalysisRequest {
		@NotNull(message = "Start date is required")
		private LocalDate startDate;
		
		@NotNull(message = "End date is required")
		private LocalDate endDate;
		
		@Pattern(regexp = "^(PDF|CSV|XLSX|JSON)$", message = "Invalid format")
		private String format = "PDF";
		
		// Financial analysis types
		private Boolean includeCashFlow = true;
		private Boolean includeProfitLoss = false;
		private Boolean includeBudgetVariance = true;
		private Boolean includeROIAnalysis = false;
		private Boolean includeCostAnalysis = true;
		
		// Breakdown options
		private String breakdownBy = "CATEGORY"; // CATEGORY, TEAM, USER, PROJECT
		private List<Long> categoryIds;
		private List<Long> teamIds;
		private String currency = "USD";
		
		// Comparison options
		private Boolean includeYearOverYear = false;
		private Boolean includeMonthOverMonth = true;
		private Boolean includeProjections = false;
		
		public FinancialAnalysisRequest(LocalDate startDate, LocalDate endDate, String format) {
			this.startDate = startDate;
			this.endDate = endDate;
			this.format = format;
			this.includeCashFlow = true;
			this.includeBudgetVariance = true;
			this.includeCostAnalysis = true;
			this.breakdownBy = "CATEGORY";
			this.currency = "USD";
			this.includeMonthOverMonth = true;
		}
	}

	// Add method to get custom parameters with type safety
	public Map<String, Object> getCustomParameters() {
		return customParameters;
	}

	public void setCustomParameters(Map<String, Object> customParameters) {
		this.customParameters = customParameters;
	}

	// Helper method to get custom parameter by key
	public Object getCustomParameter(String key) {
		return customParameters != null ? customParameters.get(key) : null;
	}

	// Helper method to get custom parameter with default value
	public Object getCustomParameter(String key, Object defaultValue) {
		if (customParameters != null && customParameters.containsKey(key)) {
			return customParameters.get(key);
		}
		return defaultValue;
	}

	// Helper method to get string custom parameter
	public String getCustomStringParameter(String key, String defaultValue) {
		Object value = getCustomParameter(key);
		return value instanceof String ? (String) value : defaultValue;
	}

	// Helper method to get boolean custom parameter
	public Boolean getCustomBooleanParameter(String key, Boolean defaultValue) {
		Object value = getCustomParameter(key);
		return value instanceof Boolean ? (Boolean) value : defaultValue;
	}

	// All getters and setters for main class
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
}