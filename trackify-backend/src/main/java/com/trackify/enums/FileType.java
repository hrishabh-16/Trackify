package com.trackify.enums;

public enum FileType {
    IMAGE("Image"),
    PDF("PDF"),
    DOCUMENT("Document"),
    OTHER("Other");
    
    private final String displayName;
    
    FileType(String displayName) {
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