package com.trackify.entity;

import com.trackify.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_workflows")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(name = "current_approver")
    private Long currentApproverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "approval_level", nullable = false)
    private Integer approvalLevel = 1;

    @Column(name = "max_approval_level", nullable = false)
    private Integer maxApprovalLevel = 1;

    @Column(name = "expense_amount", precision = 12, scale = 2)
    private BigDecimal expenseAmount;

    @Column(name = "approval_required_amount", precision = 12, scale = 2)
    private BigDecimal approvalRequiredAmount;

    @Column(name = "auto_approve_enabled")
    private Boolean autoApproveEnabled = false;

    @Column(name = "escalation_enabled")
    private Boolean escalationEnabled = false;

    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;

    @Column(name = "escalated_to")
    private Long escalatedTo;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "final_approver")
    private Long finalApproverId;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "approval_notes", length = 1000)
    private String approvalNotes;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", insertable = false, updatable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", insertable = false, updatable = false)
    private User submittedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver", insertable = false, updatable = false)
    private User currentApprover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_approver", insertable = false, updatable = false)
    private User finalApprover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @OneToMany(mappedBy = "workflowId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public ApprovalWorkflow(Long expenseId, Long submittedBy, BigDecimal expenseAmount) {
        this.expenseId = expenseId;
        this.submittedBy = submittedBy;
        this.expenseAmount = expenseAmount;
        this.status = ApprovalStatus.PENDING;
        this.approvalLevel = 1;
        this.maxApprovalLevel = 1;
        this.submittedAt = LocalDateTime.now();
        this.autoApproveEnabled = false;
        this.escalationEnabled = false;
        this.escalationLevel = 0;
        this.priority = "MEDIUM";
    }

    public ApprovalWorkflow(Long expenseId, Long submittedBy, Long currentApproverId, 
                           BigDecimal expenseAmount, Long teamId, Long categoryId) {
        this(expenseId, submittedBy, expenseAmount);
        this.currentApproverId = currentApproverId;
        this.teamId = teamId;
        this.categoryId = categoryId;
    }

    // Utility methods
    public boolean isPending() {
        return ApprovalStatus.PENDING.equals(this.status);
    }

    public boolean isApproved() {
        return ApprovalStatus.APPROVED.equals(this.status) || ApprovalStatus.AUTO_APPROVED.equals(this.status);
    }

    public boolean isRejected() {
        return ApprovalStatus.REJECTED.equals(this.status);
    }

    public boolean isCompleted() {
        return this.status.isCompleted();
    }

    public boolean canBeModified() {
        return this.status.canBeModified();
    }

    public boolean isEscalated() {
        return ApprovalStatus.ESCALATED.equals(this.status);
    }

    public boolean isOverdue() {
        return deadline != null && LocalDateTime.now().isAfter(deadline) && isPending();
    }

    public boolean canEscalate() {
        return escalationEnabled && !isEscalated() && isPending();
    }

    public void approve(Long approverId, String notes) {
        this.status = ApprovalStatus.APPROVED;
        this.finalApproverId = approverId;
        this.approvedAt = LocalDateTime.now();
        this.approvalNotes = notes;
    }

    public void reject(Long rejectorId, String reason) {
        this.status = ApprovalStatus.REJECTED;
        this.finalApproverId = rejectorId;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public void escalate(Long escalatedToUserId) {
        this.status = ApprovalStatus.ESCALATED;
        this.escalatedTo = escalatedToUserId;
        this.escalatedAt = LocalDateTime.now();
        this.escalationLevel++;
        this.currentApproverId = escalatedToUserId;
    }

    public void cancel() {
        this.status = ApprovalStatus.CANCELLED;
    }

    public void autoApprove() {
        this.status = ApprovalStatus.AUTO_APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.approvalNotes = "Auto-approved based on system rules";
    }

    public int getDaysUntilDeadline() {
        if (deadline == null) return -1;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), deadline);
    }

    public long getHoursSinceSubmission() {
        return java.time.temporal.ChronoUnit.HOURS.between(submittedAt, LocalDateTime.now());
    }

    public boolean requiresHigherApproval(BigDecimal thresholdAmount) {
        return expenseAmount != null && thresholdAmount != null && 
               expenseAmount.compareTo(thresholdAmount) > 0;
    }

    public void moveToNextApprovalLevel() {
        if (approvalLevel < maxApprovalLevel) {
            this.approvalLevel++;
        }
    }

    public boolean isAtMaxApprovalLevel() {
        return approvalLevel.equals(maxApprovalLevel);
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getExpenseId() {
		return expenseId;
	}

	public void setExpenseId(Long expenseId) {
		this.expenseId = expenseId;
	}

	public Long getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(Long submittedBy) {
		this.submittedBy = submittedBy;
	}

	public Long getCurrentApproverId() {
		return currentApproverId;
	}

	public void setCurrentApproverId(Long currentApproverId) {
		this.currentApproverId = currentApproverId;
	}

	public ApprovalStatus getStatus() {
		return status;
	}

	public void setStatus(ApprovalStatus status) {
		this.status = status;
	}

	public Integer getApprovalLevel() {
		return approvalLevel;
	}

	public void setApprovalLevel(Integer approvalLevel) {
		this.approvalLevel = approvalLevel;
	}

	public Integer getMaxApprovalLevel() {
		return maxApprovalLevel;
	}

	public void setMaxApprovalLevel(Integer maxApprovalLevel) {
		this.maxApprovalLevel = maxApprovalLevel;
	}

	public BigDecimal getExpenseAmount() {
		return expenseAmount;
	}

	public void setExpenseAmount(BigDecimal expenseAmount) {
		this.expenseAmount = expenseAmount;
	}

	public BigDecimal getApprovalRequiredAmount() {
		return approvalRequiredAmount;
	}

	public void setApprovalRequiredAmount(BigDecimal approvalRequiredAmount) {
		this.approvalRequiredAmount = approvalRequiredAmount;
	}

	public Boolean getAutoApproveEnabled() {
		return autoApproveEnabled;
	}

	public void setAutoApproveEnabled(Boolean autoApproveEnabled) {
		this.autoApproveEnabled = autoApproveEnabled;
	}

	public Boolean getEscalationEnabled() {
		return escalationEnabled;
	}

	public void setEscalationEnabled(Boolean escalationEnabled) {
		this.escalationEnabled = escalationEnabled;
	}

	public Integer getEscalationLevel() {
		return escalationLevel;
	}

	public void setEscalationLevel(Integer escalationLevel) {
		this.escalationLevel = escalationLevel;
	}

	public Long getEscalatedTo() {
		return escalatedTo;
	}

	public void setEscalatedTo(Long escalatedTo) {
		this.escalatedTo = escalatedTo;
	}

	public LocalDateTime getEscalatedAt() {
		return escalatedAt;
	}

	public void setEscalatedAt(LocalDateTime escalatedAt) {
		this.escalatedAt = escalatedAt;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(LocalDateTime submittedAt) {
		this.submittedAt = submittedAt;
	}

	public LocalDateTime getApprovedAt() {
		return approvedAt;
	}

	public void setApprovedAt(LocalDateTime approvedAt) {
		this.approvedAt = approvedAt;
	}

	public LocalDateTime getRejectedAt() {
		return rejectedAt;
	}

	public void setRejectedAt(LocalDateTime rejectedAt) {
		this.rejectedAt = rejectedAt;
	}

	public Long getFinalApproverId() {
		return finalApproverId;
	}

	public void setFinalApproverId(Long finalApproverId) {
		this.finalApproverId = finalApproverId;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}

	public String getApprovalNotes() {
		return approvalNotes;
	}

	public void setApprovalNotes(String approvalNotes) {
		this.approvalNotes = approvalNotes;
	}

	public LocalDateTime getDeadline() {
		return deadline;
	}

	public void setDeadline(LocalDateTime deadline) {
		this.deadline = deadline;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public Expense getExpense() {
		return expense;
	}

	public void setExpense(Expense expense) {
		this.expense = expense;
	}

	public User getSubmittedByUser() {
		return submittedByUser;
	}

	public void setSubmittedByUser(User submittedByUser) {
		this.submittedByUser = submittedByUser;
	}

	public User getCurrentApprover() {
		return currentApprover;
	}

	public void setCurrentApprover(User currentApprover) {
		this.currentApprover = currentApprover;
	}

	public User getFinalApprover() {
		return finalApprover;
	}

	public void setFinalApprover(User finalApprover) {
		this.finalApprover = finalApprover;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
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