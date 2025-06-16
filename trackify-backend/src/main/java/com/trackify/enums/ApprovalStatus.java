package com.trackify.enums;

public enum ApprovalStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    ESCALATED("Escalated"),
    AUTO_APPROVED("Auto Approved");

    private final String displayName;

    ApprovalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCompleted() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }

    public boolean isPending() {
        return this == PENDING || this == ESCALATED;
    }

    public boolean canBeModified() {
        return this == PENDING;
    }

    public boolean isSuccessful() {
        return this == APPROVED || this == AUTO_APPROVED;
    }
}