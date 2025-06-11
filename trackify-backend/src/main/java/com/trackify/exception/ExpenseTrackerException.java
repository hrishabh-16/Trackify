package com.trackify.exception;

public class ExpenseTrackerException extends RuntimeException {
    
    private String errorCode;
    private Object data;
    
    public ExpenseTrackerException(String message) {
        super(message);
    }
    
    public ExpenseTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExpenseTrackerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ExpenseTrackerException(String message, String errorCode, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }
    
    public ExpenseTrackerException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
}