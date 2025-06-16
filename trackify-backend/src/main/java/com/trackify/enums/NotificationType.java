package com.trackify.enums;

public enum NotificationType {
    EXPENSE_SUBMITTED("Expense Submitted"),
    EXPENSE_APPROVED("Expense Approved"),
    EXPENSE_REJECTED("Expense Rejected"),
    EXPENSE_ESCALATED("Expense Escalated"),
    EXPENSE_AUTO_APPROVED("Expense Auto Approved"),
    EXPENSE_CANCELLED("Expense Cancelled"),
    EXPENSE_PENDING_APPROVAL("Expense Pending Approval"),
    EXPENSE_OVERDUE("Expense Overdue"),
    BUDGET_EXCEEDED("Budget Exceeded"),
    BUDGET_WARNING("Budget Warning"),
    BUDGET_CREATED("Budget Created"),
    BUDGET_UPDATED("Budget Updated"),
    BUDGET_EXPIRED("Budget Expired"),
    COMMENT_ADDED("Comment Added"),
    COMMENT_REPLY("Comment Reply"),
    COMMENT_MENTION("Comment Mention"),
    APPROVAL_REQUEST("Approval Request"),
    APPROVAL_REMINDER("Approval Reminder"),
    APPROVAL_ESCALATION("Approval Escalation"),
    REPORT_GENERATED("Report Generated"),
    REPORT_READY("Report Ready"),
    SYSTEM_UPDATE("System Update"),
    POLICY_VIOLATION("Policy Violation"),
    DUPLICATE_EXPENSE("Duplicate Expense Detected"),
    RECEIPT_MISSING("Receipt Missing"),
    ACCOUNT_LOCKED("Account Locked"),
    PASSWORD_RESET("Password Reset"),
    LOGIN_ALERT("Login Alert"),
    AI_SUGGESTION("AI Suggestion"),
    ANOMALY_DETECTED("Anomaly Detected");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isExpenseRelated() {
        return this.name().startsWith("EXPENSE_");
    }

    public boolean isBudgetRelated() {
        return this.name().startsWith("BUDGET_");
    }

    public boolean isApprovalRelated() {
        return this.name().startsWith("APPROVAL_");
    }

    public boolean isCommentRelated() {
        return this.name().startsWith("COMMENT_");
    }

    public boolean isSecurityRelated() {
        return this == ACCOUNT_LOCKED || this == PASSWORD_RESET || this == LOGIN_ALERT;
    }

    public boolean isSystemRelated() {
        return this == SYSTEM_UPDATE || this == REPORT_GENERATED || this == REPORT_READY;
    }

    public boolean isAiRelated() {
        return this == AI_SUGGESTION || this == ANOMALY_DETECTED;
    }

    public boolean requiresAction() {
        return this == EXPENSE_PENDING_APPROVAL || 
               this == APPROVAL_REQUEST || 
               this == RECEIPT_MISSING ||
               this == POLICY_VIOLATION ||
               this == BUDGET_EXCEEDED;
    }

    public boolean isUrgent() {
        return this == EXPENSE_OVERDUE || 
               this == BUDGET_EXCEEDED || 
               this == POLICY_VIOLATION ||
               this == ACCOUNT_LOCKED ||
               this == ANOMALY_DETECTED;
    }
}