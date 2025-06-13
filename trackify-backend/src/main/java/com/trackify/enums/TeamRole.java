package com.trackify.enums;

public enum TeamRole {
    OWNER("Owner"),
    ADMIN("Admin"),
    MANAGER("Manager"),
    MEMBER("Member"),
    VIEWER("Viewer");

    private final String displayName;

    TeamRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasPermission(TeamRole requiredRole) {
        return this.ordinal() <= requiredRole.ordinal();
    }

    public boolean canManageTeam() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN || this == MANAGER;
    }

    public boolean canViewFinancials() {
        return this != VIEWER;
    }

    public boolean canApproveExpenses() {
        return this == OWNER || this == ADMIN || this == MANAGER;
    }

    public boolean canCreateExpenses() {
        return this != VIEWER;
    }
}