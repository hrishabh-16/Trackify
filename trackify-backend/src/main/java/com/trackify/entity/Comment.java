package com.trackify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_text", nullable = false, length = 2000)
    private String commentText;

    @Column(name = "comment_type", length = 50)
    private String commentType = "GENERAL"; // GENERAL, APPROVAL, REJECTION, ESCALATION, SYSTEM

    @Column(name = "is_internal")
    private Boolean isInternal = false;

    @Column(name = "is_system_generated")
    private Boolean isSystemGenerated = false;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "mentioned_users", length = 500)
    private String mentionedUsers; // Comma-separated user IDs

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "visibility", length = 20)
    private String visibility = "ALL"; // ALL, TEAM, APPROVERS, ADMIN

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", insertable = false, updatable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", insertable = false, updatable = false)
    private ApprovalWorkflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
    private Comment parentComment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Comment(Long expenseId, Long userId, String commentText) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.commentText = commentText;
        this.commentType = "GENERAL";
        this.isInternal = false;
        this.isSystemGenerated = false;
        this.isEdited = false;
        this.isDeleted = false;
        this.visibility = "ALL";
    }

    public Comment(Long expenseId, Long workflowId, Long userId, String commentText, String commentType) {
        this(expenseId, userId, commentText);
        this.workflowId = workflowId;
        this.commentType = commentType;
    }

    public Comment(Long expenseId, String commentText, String commentType, Boolean isSystemGenerated) {
        this.expenseId = expenseId;
        this.commentText = commentText;
        this.commentType = commentType;
        this.isSystemGenerated = isSystemGenerated;
        this.isInternal = false;
        this.isEdited = false;
        this.isDeleted = false;
        this.visibility = "ALL";
    }

    // Utility methods
    public boolean isReply() {
        return parentCommentId != null;
    }

    public boolean canBeEdited(Long currentUserId) {
        return !isDeleted && !isSystemGenerated && userId.equals(currentUserId);
    }

    public boolean canBeDeleted(Long currentUserId) {
        return !isDeleted && userId.equals(currentUserId);
    }

    public boolean isVisible(Long currentUserId, String userRole) {
        if (isDeleted) return false;
        
        switch (visibility) {
            case "ALL":
                return true;
            case "TEAM":
                // Would need team membership check
                return true; // Simplified for now
            case "APPROVERS":
                return "ADMIN".equals(userRole) || "MANAGER".equals(userRole);
            case "ADMIN":
                return "ADMIN".equals(userRole);
            default:
                return true;
        }
    }

    public void edit(String newText) {
        this.commentText = newText;
        this.isEdited = true;
        this.editedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void addMentionedUser(Long userId) {
        if (mentionedUsers == null || mentionedUsers.isEmpty()) {
            mentionedUsers = userId.toString();
        } else {
            mentionedUsers += "," + userId;
        }
    }

    public boolean hasMentions() {
        return mentionedUsers != null && !mentionedUsers.isEmpty();
    }

    public String[] getMentionedUserIds() {
        if (mentionedUsers == null || mentionedUsers.isEmpty()) {
            return new String[0];
        }
        return mentionedUsers.split(",");
    }

    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.isEmpty();
    }

    public long getAgeInHours() {
        return java.time.temporal.ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
    }

    public boolean isRecent() {
        return getAgeInHours() <= 24; // Consider recent if within 24 hours
    }

    public static Comment createSystemComment(Long expenseId, String message) {
        return new Comment(expenseId, message, "SYSTEM", true);
    }

    public static Comment createApprovalComment(Long expenseId, Long workflowId, Long userId, String message) {
        return new Comment(expenseId, workflowId, userId, message, "APPROVAL");
    }

    public static Comment createRejectionComment(Long expenseId, Long workflowId, Long userId, String message) {
        return new Comment(expenseId, workflowId, userId, message, "REJECTION");
    }
}