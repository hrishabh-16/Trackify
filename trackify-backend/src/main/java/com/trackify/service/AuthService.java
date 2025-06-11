package com.trackify.service;

import com.trackify.dto.request.LoginRequest;
import com.trackify.dto.request.RegisterRequest;
import com.trackify.dto.response.AuthResponse;
import com.trackify.dto.response.UserResponse;

public interface AuthService {
    
    // Authentication operations
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse refreshToken(String refreshToken);
    void logout(String token);
    
    // Current user operations
    UserResponse getCurrentUser(Long userId);
    UserResponse updateCurrentUser(Long userId, RegisterRequest updateRequest);
    void changeCurrentUserPassword(Long userId, String currentPassword, String newPassword);
    
    // Email verification
    UserResponse verifyEmail(String token);
    void resendEmailVerification(String email);
    
    // Password reset
    void forgotPassword(String email);
    UserResponse resetPassword(String token, String newPassword);
    
    // Token validation
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
    
    // OAuth2 support
    AuthResponse processOAuth2Login(String email, String firstName, String lastName, String avatarUrl);
}