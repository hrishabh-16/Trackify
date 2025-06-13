package com.trackify.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseWebSocketMessage {
    
    private Long expenseId;
    private String action; // CREATE, UPDATE, DELETE, APPROVE, REJECT
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String category;
    private String status;
    private String paymentMethod;
    private Long userId;
    private String username;
    private Long teamId;
    private String teamName;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expenseDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String receiptUrl;
    private String notes;
    private String approvedBy;
    private String rejectedBy;
    private String rejectionReason;

    // Constructor for expense creation
    public ExpenseWebSocketMessage(String action, Long expenseId, String title, 
                                 BigDecimal amount, String category, String username) {
        this.action = action;
        this.expenseId = expenseId;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for expense update
    public ExpenseWebSocketMessage(String action, Long expenseId, String title, 
                                 BigDecimal amount, String status, String username,
                                 LocalDateTime updatedAt) {
        this.action = action;
        this.expenseId = expenseId;
        this.title = title;
        this.amount = amount;
        this.status = status;
        this.username = username;
        this.updatedAt = updatedAt;
    }

    // Constructor for expense deletion
    public ExpenseWebSocketMessage(String action, Long expenseId, String username) {
        this.action = action;
        this.expenseId = expenseId;
        this.username = username;
        this.updatedAt = LocalDateTime.now();
    }

	public Long getExpenseId() {
		return expenseId;
	}

	public void setExpenseId(Long expenseId) {
		this.expenseId = expenseId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
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

	public LocalDateTime getExpenseDate() {
		return expenseDate;
	}

	public void setExpenseDate(LocalDateTime expenseDate) {
		this.expenseDate = expenseDate;
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

	public String getReceiptUrl() {
		return receiptUrl;
	}

	public void setReceiptUrl(String receiptUrl) {
		this.receiptUrl = receiptUrl;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(String approvedBy) {
		this.approvedBy = approvedBy;
	}

	public String getRejectedBy() {
		return rejectedBy;
	}

	public void setRejectedBy(String rejectedBy) {
		this.rejectedBy = rejectedBy;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}
    
    
}