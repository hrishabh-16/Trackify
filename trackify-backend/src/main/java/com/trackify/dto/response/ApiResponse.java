package com.trackify.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success = true;
    private String message;
    private T data;
    private int status;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    // Manual builder method
    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static class ApiResponseBuilder<T> {
        private boolean success = true;
        private String message;
        private T data;
        private int status;
        private String path;
        private LocalDateTime timestamp = LocalDateTime.now();

        public ApiResponseBuilder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public ApiResponseBuilder<T> message(String message) {
        	this.message = message;
            return this;
        }

        public ApiResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public ApiResponseBuilder<T> status(int status) {
            this.status = status;
            return this;
        }

        public ApiResponseBuilder<T> path(String path) {
            this.path = path;
            return this;
        }

        public ApiResponseBuilder<T> timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ApiResponse<T> build() {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(this.success);
            response.setMessage(this.message);
            response.setData(this.data);
            response.setStatus(this.status);
            response.setPath(this.path);
            response.setTimestamp(this.timestamp);
            return response;
        }
    }

    // Factory methods
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Request processed successfully", data);
    }

    public static ApiResponse<Void> success(String message) {
        return success(message, null);
    }

    public static ApiResponse<Void> success() {
        return success("Request processed successfully", null);
    }

    // FIXED: Added overloaded error methods to handle different parameter combinations
    public static <T> ApiResponse<T> error(String message, int status, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .status(status)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return error(message, status, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, 500, null);
    }

    // FIXED: Added this method to handle error(String, T) calls
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .status(500)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> validationError(String message, T validationErrors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(validationErrors)
                .status(400)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}