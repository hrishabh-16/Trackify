package com.trackify.exception;

import com.trackify.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
    	logger.error("Resource not found: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {
        
    	logger.error("Bad request: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        
    	logger.error("Unauthorized access: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbiddenException(
            ForbiddenException ex, HttpServletRequest request) {
        
    	logger.error("Forbidden access: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getMessage(),
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
    	logger.error("Validation error: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        if (ex.getErrors() != null) {
            errorDetails.put("errors", ex.getErrors());
        }
        if (ex.getFieldErrors() != null) {
            errorDetails.put("fieldErrors", ex.getFieldErrors());
        }
        
        ApiResponse<Object> response = ApiResponse.builder()
            .success(false)
            .message(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .data(errorDetails)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ExpenseTrackerException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpenseTrackerException(
            ExpenseTrackerException ex, HttpServletRequest request) {
        
    	logger.error("Application error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorDetails = new HashMap<>();
        if (ex.getErrorCode() != null) {
            errorDetails.put("errorCode", ex.getErrorCode());
        }
        if (ex.getData() != null) {
            errorDetails.put("data", ex.getData());
        }
        
        ApiResponse<Object> response = ApiResponse.builder()
            .success(false)
            .message(ex.getMessage())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .data(errorDetails.isEmpty() ? null : errorDetails)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
    	logger.error("Validation failed: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        List<String> globalErrors = new ArrayList<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                globalErrors.add(error.getDefaultMessage());
            }
        });
        
        Map<String, Object> errorDetails = new HashMap<>();
        if (!fieldErrors.isEmpty()) {
            errorDetails.put("fieldErrors", fieldErrors);
        }
        if (!globalErrors.isEmpty()) {
            errorDetails.put("globalErrors", globalErrors);
        }
        
        ApiResponse<Object> response = ApiResponse.builder()
            .success(false)
            .message("Validation failed")
            .status(HttpStatus.BAD_REQUEST.value())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .data(errorDetails)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(
            BindException ex, HttpServletRequest request) {
        
    	logger.error("Binding failed: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage()));
        
        ApiResponse<Object> response = ApiResponse.builder()
            .success(false)
            .message("Invalid request parameters")
            .status(HttpStatus.BAD_REQUEST.value())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .data(Map.of("fieldErrors", fieldErrors))
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
    	logger.error("Constraint violation: {}", ex.getMessage());
        
        List<String> errors = ex.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList());
        
        ApiResponse<Object> response = ApiResponse.builder()
            .success(false)
            .message("Validation constraints violated")
            .status(HttpStatus.BAD_REQUEST.value())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .data(Map.of("errors", errors))
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
    	logger.error("Authentication failed: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            "Authentication failed: " + ex.getMessage(),
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
    	logger.error("Bad credentials: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            "Invalid email or password",
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
    	logger.error("Access denied: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            "Access denied: You don't have permission to access this resource",
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
    	logger.error("Method not supported: {}", ex.getMessage());
        
        String message = String.format("Request method '%s' not supported. Supported methods: %s",
            ex.getMethod(), String.join(", ", ex.getSupportedMethods()));
        
        ApiResponse<Object> response = ApiResponse.error(
            message,
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        
    	logger.error("Media type not supported: {}", ex.getMessage());
        
        String message = String.format("Media type '%s' not supported. Supported media types: %s",
            ex.getContentType(), ex.getSupportedMediaTypes());
        
        ApiResponse<Object> response = ApiResponse.error(
            message,
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
    	logger.error("Message not readable: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            "Malformed JSON request",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
    	logger.error("Missing request parameter: {}", ex.getMessage());
        
        String message = String.format("Required request parameter '%s' is missing", ex.getParameterName());
        
        ApiResponse<Object> response = ApiResponse.error(
            message,
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
    	logger.error("Method argument type mismatch: {}", ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        
        ApiResponse<Object> response = ApiResponse.error(
            message,
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
    	logger.error("File size exceeded: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
            "File size exceeds maximum allowed limit",
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
    	logger.error("No handler found: {}", ex.getMessage());
        
        String message = String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL());
        
        ApiResponse<Object> response = ApiResponse.error(
            message,
            HttpStatus.NOT_FOUND.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        
    	logger.error("Unexpected error occurred", ex);
        
        ApiResponse<Object> response = ApiResponse.error(
            "An unexpected error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}