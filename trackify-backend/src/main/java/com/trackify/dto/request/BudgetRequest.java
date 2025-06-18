package com.trackify.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // Default constructor
    public BudgetRequest() {
    }

    // Constructor for all fields
    public BudgetRequest(String name, String description, BigDecimal totalAmount, LocalDate startDate, 
                        LocalDate endDate, String currency, BigDecimal alertThreshold, Boolean isActive, 
                        Boolean isRecurring, String recurrencePeriod, Long categoryId, Long teamId, String notes) {
        this.name = name;
        this.description = description;
        this.totalAmount = totalAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currency = currency;
        this.alertThreshold = alertThreshold;
        this.isActive = isActive;
        this.isRecurring = isRecurring;
        this.recurrencePeriod = recurrencePeriod;
        this.categoryId = categoryId;
        this.teamId = teamId;
        this.notes = notes;
    }

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

    // Getters and Setters
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

    // Override equals, hashCode, and toString methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BudgetRequest that = (BudgetRequest) obj;
        
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (totalAmount != null ? !totalAmount.equals(that.totalAmount) : that.totalAmount != null) return false;
        if (startDate != null ? !startDate.equals(that.startDate) : that.startDate != null) return false;
        if (endDate != null ? !endDate.equals(that.endDate) : that.endDate != null) return false;
        if (currency != null ? !currency.equals(that.currency) : that.currency != null) return false;
        if (alertThreshold != null ? !alertThreshold.equals(that.alertThreshold) : that.alertThreshold != null) return false;
        if (isActive != null ? !isActive.equals(that.isActive) : that.isActive != null) return false;
        if (isRecurring != null ? !isRecurring.equals(that.isRecurring) : that.isRecurring != null) return false;
        if (recurrencePeriod != null ? !recurrencePeriod.equals(that.recurrencePeriod) : that.recurrencePeriod != null) return false;
        if (categoryId != null ? !categoryId.equals(that.categoryId) : that.categoryId != null) return false;
        if (teamId != null ? !teamId.equals(that.teamId) : that.teamId != null) return false;
        return notes != null ? notes.equals(that.notes) : that.notes == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (totalAmount != null ? totalAmount.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        result = 31 * result + (alertThreshold != null ? alertThreshold.hashCode() : 0);
        result = 31 * result + (isActive != null ? isActive.hashCode() : 0);
        result = 31 * result + (isRecurring != null ? isRecurring.hashCode() : 0);
        result = 31 * result + (recurrencePeriod != null ? recurrencePeriod.hashCode() : 0);
        result = 31 * result + (categoryId != null ? categoryId.hashCode() : 0);
        result = 31 * result + (teamId != null ? teamId.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BudgetRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", totalAmount=" + totalAmount +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", currency='" + currency + '\'' +
                ", alertThreshold=" + alertThreshold +
                ", isActive=" + isActive +
                ", isRecurring=" + isRecurring +
                ", recurrencePeriod='" + recurrencePeriod + '\'' +
                ", categoryId=" + categoryId +
                ", teamId=" + teamId +
                ", notes='" + notes + '\'' +
                '}';
    }

    // Inner Classes
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

        // Default constructor
        public UpdateBudgetRequest() {
        }

        // All args constructor
        public UpdateBudgetRequest(String name, String description, BigDecimal totalAmount, LocalDate startDate, 
                                 LocalDate endDate, String currency, BigDecimal alertThreshold, Boolean isActive, 
                                 Boolean isRecurring, String recurrencePeriod, Long categoryId, Long teamId, String notes) {
            this.name = name;
            this.description = description;
            this.totalAmount = totalAmount;
            this.startDate = startDate;
            this.endDate = endDate;
            this.currency = currency;
            this.alertThreshold = alertThreshold;
            this.isActive = isActive;
            this.isRecurring = isRecurring;
            this.recurrencePeriod = recurrencePeriod;
            this.categoryId = categoryId;
            this.teamId = teamId;
            this.notes = notes;
        }

        // Getters and Setters
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            UpdateBudgetRequest that = (UpdateBudgetRequest) obj;
            
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (description != null ? !description.equals(that.description) : that.description != null) return false;
            if (totalAmount != null ? !totalAmount.equals(that.totalAmount) : that.totalAmount != null) return false;
            if (startDate != null ? !startDate.equals(that.startDate) : that.startDate != null) return false;
            if (endDate != null ? !endDate.equals(that.endDate) : that.endDate != null) return false;
            if (currency != null ? !currency.equals(that.currency) : that.currency != null) return false;
            if (alertThreshold != null ? !alertThreshold.equals(that.alertThreshold) : that.alertThreshold != null) return false;
            if (isActive != null ? !isActive.equals(that.isActive) : that.isActive != null) return false;
            if (isRecurring != null ? !isRecurring.equals(that.isRecurring) : that.isRecurring != null) return false;
            if (recurrencePeriod != null ? !recurrencePeriod.equals(that.recurrencePeriod) : that.recurrencePeriod != null) return false;
            if (categoryId != null ? !categoryId.equals(that.categoryId) : that.categoryId != null) return false;
            if (teamId != null ? !teamId.equals(that.teamId) : that.teamId != null) return false;
            return notes != null ? notes.equals(that.notes) : that.notes == null;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (totalAmount != null ? totalAmount.hashCode() : 0);
            result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
            result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
            result = 31 * result + (currency != null ? currency.hashCode() : 0);
            result = 31 * result + (alertThreshold != null ? alertThreshold.hashCode() : 0);
            result = 31 * result + (isActive != null ? isActive.hashCode() : 0);
            result = 31 * result + (isRecurring != null ? isRecurring.hashCode() : 0);
            result = 31 * result + (recurrencePeriod != null ? recurrencePeriod.hashCode() : 0);
            result = 31 * result + (categoryId != null ? categoryId.hashCode() : 0);
            result = 31 * result + (teamId != null ? teamId.hashCode() : 0);
            result = 31 * result + (notes != null ? notes.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "UpdateBudgetRequest{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", totalAmount=" + totalAmount +
                    ", startDate=" + startDate +
                    ", endDate=" + endDate +
                    ", currency='" + currency + '\'' +
                    ", alertThreshold=" + alertThreshold +
                    ", isActive=" + isActive +
                    ", isRecurring=" + isRecurring +
                    ", recurrencePeriod='" + recurrencePeriod + '\'' +
                    ", categoryId=" + categoryId +
                    ", teamId=" + teamId +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }

    public static class BudgetAlertRequest {
        
        @NotNull(message = "Budget ID is required")
        private Long budgetId;

        @DecimalMin(value = "0.0", message = "Alert threshold must be non-negative")
        @DecimalMax(value = "100.0", message = "Alert threshold cannot exceed 100%")
        private BigDecimal alertThreshold;

        private Boolean emailAlert = true;

        private Boolean pushAlert = true;

        private String alertMessage;

        // Default constructor
        public BudgetAlertRequest() {
        }

        // All args constructor
        public BudgetAlertRequest(Long budgetId, BigDecimal alertThreshold, Boolean emailAlert, 
                                Boolean pushAlert, String alertMessage) {
            this.budgetId = budgetId;
            this.alertThreshold = alertThreshold;
            this.emailAlert = emailAlert;
            this.pushAlert = pushAlert;
            this.alertMessage = alertMessage;
        }

        // Getters and Setters
        public Long getBudgetId() {
            return budgetId;
        }

        public void setBudgetId(Long budgetId) {
            this.budgetId = budgetId;
        }

        public BigDecimal getAlertThreshold() {
            return alertThreshold;
        }

        public void setAlertThreshold(BigDecimal alertThreshold) {
            this.alertThreshold = alertThreshold;
        }

        public Boolean getEmailAlert() {
            return emailAlert;
        }

        public void setEmailAlert(Boolean emailAlert) {
            this.emailAlert = emailAlert;
        }

        public Boolean getPushAlert() {
            return pushAlert;
        }

        public void setPushAlert(Boolean pushAlert) {
            this.pushAlert = pushAlert;
        }

        public String getAlertMessage() {
            return alertMessage;
        }

        public void setAlertMessage(String alertMessage) {
            this.alertMessage = alertMessage;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            BudgetAlertRequest that = (BudgetAlertRequest) obj;
            
            if (budgetId != null ? !budgetId.equals(that.budgetId) : that.budgetId != null) return false;
            if (alertThreshold != null ? !alertThreshold.equals(that.alertThreshold) : that.alertThreshold != null) return false;
            if (emailAlert != null ? !emailAlert.equals(that.emailAlert) : that.emailAlert != null) return false;
            if (pushAlert != null ? !pushAlert.equals(that.pushAlert) : that.pushAlert != null) return false;
            return alertMessage != null ? alertMessage.equals(that.alertMessage) : that.alertMessage == null;
        }

        @Override
        public int hashCode() {
            int result = budgetId != null ? budgetId.hashCode() : 0;
            result = 31 * result + (alertThreshold != null ? alertThreshold.hashCode() : 0);
            result = 31 * result + (emailAlert != null ? emailAlert.hashCode() : 0);
            result = 31 * result + (pushAlert != null ? pushAlert.hashCode() : 0);
            result = 31 * result + (alertMessage != null ? alertMessage.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "BudgetAlertRequest{" +
                    "budgetId=" + budgetId +
                    ", alertThreshold=" + alertThreshold +
                    ", emailAlert=" + emailAlert +
                    ", pushAlert=" + pushAlert +
                    ", alertMessage='" + alertMessage + '\'' +
                    '}';
        }
    }

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

        // Default constructor
        public RecurringBudgetRequest() {
        }

        // All args constructor
        public RecurringBudgetRequest(Long budgetId, String recurrencePeriod, Boolean autoCreate, 
                                    Integer occurrences, LocalDate nextStartDate, String notes) {
            this.budgetId = budgetId;
            this.recurrencePeriod = recurrencePeriod;
            this.autoCreate = autoCreate;
            this.occurrences = occurrences;
            this.nextStartDate = nextStartDate;
            this.notes = notes;
        }

        // Getters and Setters
        public Long getBudgetId() {
            return budgetId;
        }

        public void setBudgetId(Long budgetId) {
            this.budgetId = budgetId;
        }

        public String getRecurrencePeriod() {
            return recurrencePeriod;
        }

        public void setRecurrencePeriod(String recurrencePeriod) {
            this.recurrencePeriod = recurrencePeriod;
        }

        public Boolean getAutoCreate() {
            return autoCreate;
        }

        public void setAutoCreate(Boolean autoCreate) {
            this.autoCreate = autoCreate;
        }

        public Integer getOccurrences() {
            return occurrences;
        }

        public void setOccurrences(Integer occurrences) {
            this.occurrences = occurrences;
        }

        public LocalDate getNextStartDate() {
            return nextStartDate;
        }

        public void setNextStartDate(LocalDate nextStartDate) {
            this.nextStartDate = nextStartDate;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            RecurringBudgetRequest that = (RecurringBudgetRequest) obj;
            
            if (budgetId != null ? !budgetId.equals(that.budgetId) : that.budgetId != null) return false;
            if (recurrencePeriod != null ? !recurrencePeriod.equals(that.recurrencePeriod) : that.recurrencePeriod != null) return false;
            if (autoCreate != null ? !autoCreate.equals(that.autoCreate) : that.autoCreate != null) return false;
            if (occurrences != null ? !occurrences.equals(that.occurrences) : that.occurrences != null) return false;
            if (nextStartDate != null ? !nextStartDate.equals(that.nextStartDate) : that.nextStartDate != null) return false;
            return notes != null ? notes.equals(that.notes) : that.notes == null;
        }

        @Override
        public int hashCode() {
            int result = budgetId != null ? budgetId.hashCode() : 0;
            result = 31 * result + (recurrencePeriod != null ? recurrencePeriod.hashCode() : 0);
            result = 31 * result + (autoCreate != null ? autoCreate.hashCode() : 0);
            result = 31 * result + (occurrences != null ? occurrences.hashCode() : 0);
            result = 31 * result + (nextStartDate != null ? nextStartDate.hashCode() : 0);
            result = 31 * result + (notes != null ? notes.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "RecurringBudgetRequest{" +
                    "budgetId=" + budgetId +
                    ", recurrencePeriod='" + recurrencePeriod + '\'' +
                    ", autoCreate=" + autoCreate +
                    ", occurrences=" + occurrences +
                    ", nextStartDate=" + nextStartDate +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }

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

        // Default constructor
        public BudgetTransferRequest() {
        }

        // All args constructor
        public BudgetTransferRequest(Long sourceBudgetId, Long targetBudgetId, BigDecimal amount, String reason) {
            this.sourceBudgetId = sourceBudgetId;
            this.targetBudgetId = targetBudgetId;
            this.amount = amount;
            this.reason = reason;
        }

        // Getters and Setters
        public Long getSourceBudgetId() {
            return sourceBudgetId;
        }

        public void setSourceBudgetId(Long sourceBudgetId) {
            this.sourceBudgetId = sourceBudgetId;
        }

        public Long getTargetBudgetId() {
            return targetBudgetId;
        }

        public void setTargetBudgetId(Long targetBudgetId) {
            this.targetBudgetId = targetBudgetId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            BudgetTransferRequest that = (BudgetTransferRequest) obj;
            
            if (sourceBudgetId != null ? !sourceBudgetId.equals(that.sourceBudgetId) : that.sourceBudgetId != null) return false;
            if (targetBudgetId != null ? !targetBudgetId.equals(that.targetBudgetId) : that.targetBudgetId != null) return false;
            if (amount != null ? !amount.equals(that.amount) : that.amount != null) return false;
            return reason != null ? reason.equals(that.reason) : that.reason == null;
        }

        @Override
        public int hashCode() {
            int result = sourceBudgetId != null ? sourceBudgetId.hashCode() : 0;
            result = 31 * result + (targetBudgetId != null ? targetBudgetId.hashCode() : 0);
            result = 31 * result + (amount != null ? amount.hashCode() : 0);
            result = 31 * result + (reason != null ? reason.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "BudgetTransferRequest{" +
                    "sourceBudgetId=" + sourceBudgetId +
                    ", targetBudgetId=" + targetBudgetId +
                    ", amount=" + amount +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}