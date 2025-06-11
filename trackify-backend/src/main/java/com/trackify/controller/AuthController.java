package com.trackify.controller;

import com.trackify.dto.request.LoginRequest;
import com.trackify.dto.request.RegisterRequest;
import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.AuthResponse;
import com.trackify.dto.response.UserResponse;
import com.trackify.security.UserPrincipal;
import com.trackify.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization APIs")
public class AuthController {

	private static final Logger  logger = LoggerFactory.getLogger(AuthController.class);

	@Autowired
    private  AuthService authService;
    
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
    	logger.info("Registration attempt for email: {}", registerRequest.getEmail());
        
        try {
            AuthResponse authResponse = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
        } catch (Exception e) {
        	logger.error("Registration failed for email: {}", registerRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Registration failed: " + e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
    	logger.info("Login attempt for email: {}", loginRequest.getEmail());
        
        try {
            AuthResponse authResponse = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
        } catch (Exception e) {
        	logger.error("Login failed for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Login failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
        }
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Refresh JWT access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody String refreshToken) {
    	logger.info("Token refresh attempt");
        
        try {
            AuthResponse authResponse = authService.refreshToken(refreshToken.trim());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
        } catch (Exception e) {
        	logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Token refresh failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
        }
    }
    
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate tokens")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (StringUtils.hasText(token)) {
            authService.logout(token);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user details")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        UserResponse user = authService.getCurrentUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", user));
    }
    
    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RegisterRequest updateRequest) {
        
        UserResponse updatedUser = authService.updateCurrentUser(currentUser.getId(), updateRequest);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        
        authService.changeCurrentUserPassword(currentUser.getId(), currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
    
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify user email using verification token")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(@RequestParam String token) {
    	logger.info("Email verification attempt with token: {}", token);
        
        UserResponse user = authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", user));
    }
    
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification", description = "Resend email verification link")
    public ResponseEntity<ApiResponse<Void>> resendEmailVerification(@RequestParam String email) {
    	logger.info("Resend email verification for: {}", email);
        
        authService.resendEmailVerification(email);
        return ResponseEntity.ok(ApiResponse.success("Email verification sent successfully", null));
    }
    
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send password reset link to email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
    	logger.info("Password reset request for email: {}", email);
        
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent successfully", null));
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using reset token")
    public ResponseEntity<ApiResponse<UserResponse>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
    	logger.info("Password reset attempt with token: {}", token);
        
        UserResponse user = authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", user));
    }
    
    @PostMapping("/validate-token")
    @Operation(summary = "Validate token", description = "Validate JWT token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestParam String token) {
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token validation completed", isValid));
    }
    
    // Test endpoint to verify controller is working
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth controller is working!");
    }
    
    // Utility method to extract token from request
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}