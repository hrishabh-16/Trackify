package com.trackify.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {

    @NotBlank(message = "Budget name is required")
    @Size(min = 2, max = 200, message = "Budget name must be between 2 and 200 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal totalAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency = "USD";

    @DecimalMin(value = "0.0", message = "Alert threshold must be non-negative")
    @DecimalMax(value = "100.0", message = "Alert threshold cannot exceed 100%")
    private BigDecimal alertThreshold = BigDecimal.valueOf(80.0);

    private Boolean isActive = true;

    private Boolean isRecurring = false;

    @Pattern(regexp = "^(MONTHLY|QUARTERLY|YEARLY)$", message = "Invalid recurrence period")
    private String recurrencePeriod;

    private Long categoryId;

    private Long teamId;

    private String notes;

    // Constructor for basic budget
    public BudgetRequest(String name, BigDecimal totalAmount, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.totalAmount = totalAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currency = "USD";
        this.alertThreshold = BigDecimal.valueOf(80.0);
        this.isActive = true;
        this.isRecurring = false;
    }

    // Constructor with category
    public BudgetRequest(String name, BigDecimal totalAmount, LocalDate startDate, 
                        LocalDate endDate, Long categoryId) {
        this(name, totalAmount, startDate, endDate);
        this.categoryId = categoryId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBudgetRequest {
        
        @Size(min = 2, max = 200, message = "Budget name must be between 2 and 200 characters")
        private String name;

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;

        @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
        private BigDecimal totalAmount;

        private LocalDate startDate;

        private LocalDate endDate;

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        private String currency;

        @DecimalMin(value = "0.0", message = "Alert threshold must be non-negative")
        @DecimalMax(value = "100.0", message = "Alert threshold cannot exceed 100%")
        private BigDecimal alertThreshold;

        private Boolean isActive;

        private Boolean isRecurring;

        @Pattern(regexp = "^(MONTHLY|QUARTERLY|YEARLY)$", message = "Invalid recurrence period")
        private String recurrencePeriod;

        private Long categoryId;

        private Long teamId;

        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetAlertRequest {
        
        @NotNull(message = "Budget ID is required")
        private Long budgetId;

        @DecimalMin(value = "0.0", message = "Alert threshold must be non-negative")
        @DecimalMax(value = "100.0", message = "Alert threshold cannot exceed 100%")
        private BigDecimal alertThreshold;

        private Boolean emailAlert = true;

        private Boolean pushAlert = true;

        private String alertMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecurringBudgetRequest {
        
        @NotNull(message = "Budget ID is required")
        private Long budgetId;

        @NotNull(message = "Recurrence period is required")
        @Pattern(regexp = "^(MONTHLY|QUARTERLY|YEARLY)$", message = "Invalid recurrence period")
        private String recurrencePeriod;

        private Boolean autoCreate = true;

        private Integer occurrences; // Number of times to repeat, null for infinite

        private LocalDate nextStartDate;

        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetTransferRequest {
        
        @NotNull(message = "Source budget ID is required")
        private Long sourceBudgetId;

        @NotNull(message = "Target budget ID is required")
        private Long targetBudgetId;

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(value = "0.01", message = "Transfer amount must be greater than 0")
        private BigDecimal amount;

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        private String reason;
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

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
    
}