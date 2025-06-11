package com.trackify.enums;

public enum UserRole {
    ADMIN("ADMIN", "System Administrator"),
    USER("USER", "Regular User"),
    VIEWER("VIEWER", "View Only User");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : UserRole.values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid UserRole code: " + code);
    }
}