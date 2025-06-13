package com.trackify.enums;

public enum ExpenseStatus {
    DRAFT("Draft"),
    PENDING("Pending"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PAID("Paid"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    ExpenseStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}