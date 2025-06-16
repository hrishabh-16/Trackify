package com.trackify.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    @NotNull(message = "Expense ID is required")
    private Long expenseId;

    @NotNull(message = "Action is required")
    @Pattern(regexp = "^(APPROVE|REJECT|ESCALATE|CANCEL)$", message = "Invalid action")
    private String action;

    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;

    @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
    private String rejectionReason;

    private Long escalateToUserId;

    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|URGENT)$", message = "Invalid priority")
    private String priority = "MEDIUM";

    private LocalDateTime deadline;

    private Boolean notifySubmitter = true;

    private Boolean notifyTeam = false;

    // Constructor for approval
    public ApprovalRequest(Long expenseId, String action, String comments) {
        this.expenseId = expenseId;
        this.action = action;
        this.comments = comments;
        this.notifySubmitter = true;
        this.notifyTeam = false;
        this.priority = "MEDIUM";
    }

    // Constructor for rejection
    public ApprovalRequest(Long expenseId, String action, String comments, String rejectionReason) {
        this(expenseId, action, comments);
        this.rejectionReason = rejectionReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkApprovalRequest {
        
        @NotEmpty(message = "Expense IDs are required")
        private List<Long> expenseIds;

        @NotNull(message = "Action is required")
        @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Invalid bulk action")
        private String action;

        @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
        private String comments;

        @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
        private String rejectionReason;

        private Boolean notifySubmitters = true;

        private Boolean notifyTeams = false;

        // Constructor
        public BulkApprovalRequest(List<Long> expenseIds, String action, String comments) {
            this.expenseIds = expenseIds;
            this.action = action;
            this.comments = comments;
            this.notifySubmitters = true;
            this.notifyTeams = false;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentRequest {
        
        @NotNull(message = "Expense ID is required")
        private Long expenseId;

        @NotBlank(message = "Comment text is required")
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        private String commentText;

        @Pattern(regexp = "^(GENERAL|APPROVAL|REJECTION|ESCALATION|QUESTION|CLARIFICATION)$", 
                message = "Invalid comment type")
        private String commentType = "GENERAL";

        private Boolean isInternal = false;

        private Long parentCommentId;

        private List<Long> mentionedUserIds;

        @Pattern(regexp = "^(ALL|TEAM|APPROVERS|ADMIN)$", message = "Invalid visibility")
        private String visibility = "ALL";

        private String attachmentUrl;

        // Constructor for simple comment
        public CommentRequest(Long expenseId, String commentText) {
            this.expenseId = expenseId;
            this.commentText = commentText;
            this.commentType = "GENERAL";
            this.isInternal = false;
            this.visibility = "ALL";
        }

        // Constructor for reply
        public CommentRequest(Long expenseId, String commentText, Long parentCommentId) {
            this(expenseId, commentText);
            this.parentCommentId = parentCommentId;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowConfigRequest {
        
        private Long teamId;

        private Long categoryId;

        @DecimalMin(value = "0.0", message = "Auto approval threshold must be non-negative")
        private BigDecimal autoApprovalThreshold;

        private Boolean autoApproveEnabled = false;

        private Boolean escalationEnabled = false;

        @Min(value = 1, message = "Escalation hours must be at least 1")
        @Max(value = 168, message = "Escalation hours cannot exceed 168 (1 week)")
        private Integer escalationHours = 24;

        private List<Long> approverIds;

        @Min(value = 1, message = "Max approval level must be at least 1")
        @Max(value = 5, message = "Max approval level cannot exceed 5")
        private Integer maxApprovalLevel = 1;

        private Boolean requireManagerApproval = true;

        private Boolean requireFinanceApproval = false;

        @DecimalMin(value = "0.0", message = "Finance approval threshold must be non-negative")
        private BigDecimal financeApprovalThreshold;

        // Constructor
        public WorkflowConfigRequest(Long teamId, Boolean autoApproveEnabled, Boolean escalationEnabled) {
            this.teamId = teamId;
            this.autoApproveEnabled = autoApproveEnabled;
            this.escalationEnabled = escalationEnabled;
            this.escalationHours = 24;
            this.maxApprovalLevel = 1;
            this.requireManagerApproval = true;
            this.requireFinanceApproval = false;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalFilterRequest {
        
        private String status; // PENDING, APPROVED, REJECTED, etc.

        private Long approverId;

        private Long submittedBy;

        private Long teamId;

        private Long categoryId;

        private BigDecimal minAmount;

        private BigDecimal maxAmount;

        private LocalDateTime startDate;

        private LocalDateTime endDate;

        private String priority;

        private Boolean overdue;

        private Boolean escalated;

        private String sortBy = "submittedAt"; // submittedAt, amount, priority

        private String sortDirection = "DESC"; // ASC, DESC

        // Constructor for basic filter
        public ApprovalFilterRequest(String status, Long approverId) {
            this.status = status;
            this.approverId = approverId;
            this.sortBy = "submittedAt";
            this.sortDirection = "DESC";
        }
    }
}