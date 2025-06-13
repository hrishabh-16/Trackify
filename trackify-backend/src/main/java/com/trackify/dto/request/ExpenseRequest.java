package com.trackify.dto.request;

import com.trackify.enums.ExpenseStatus;
import com.trackify.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExpenseRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "Amount must have at most 6 integer digits and 2 decimal places")
    private BigDecimal amount;
    
    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;
    
    @NotNull(message = "Category is required")
    private Long categoryId;
    
    private PaymentMethod paymentMethod;
    
    @Size(max = 200, message = "Merchant name must not exceed 200 characters")
    private String merchantName;
    
    @Size(max = 500, message = "Location must not exceed 500 characters")
    private String location;
    
    @Size(max = 500, message = "Tags must not exceed 500 characters")
    private String tags;
    
    private Boolean isRecurring = false;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a valid 3-letter code (e.g., USD)")
    private String currencyCode = "USD";
    
    @DecimalMin(value = "0.0001", message = "Exchange rate must be positive")
    @DecimalMax(value = "9999.9999", message = "Exchange rate is too high")
    private BigDecimal exchangeRate;
    
    @DecimalMin(value = "0.01", message = "Original amount must be greater than 0")
    private BigDecimal originalAmount;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Original currency must be a valid 3-letter code")
    private String originalCurrency;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;
    
    private Boolean isBusinessExpense = false;
    
    private Boolean isReimbursable = false;
    
    private Long teamId;
    
    private Long projectId;
    
    private List<Long> receiptIds;
    
    // Status should typically be set by the system, but can be included for drafts
    private ExpenseStatus status = ExpenseStatus.PENDING;

    // Getters and Setters
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

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Boolean getIsBusinessExpense() {
        return isBusinessExpense;
    }

    public void setIsBusinessExpense(Boolean isBusinessExpense) {
        this.isBusinessExpense = isBusinessExpense;
    }

    public Boolean getIsReimbursable() {
        return isReimbursable;
    }

    public void setIsReimbursable(Boolean isReimbursable) {
        this.isReimbursable = isReimbursable;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public List<Long> getReceiptIds() {
        return receiptIds;
    }

    public void setReceiptIds(List<Long> receiptIds) {
        this.receiptIds = receiptIds;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }
}