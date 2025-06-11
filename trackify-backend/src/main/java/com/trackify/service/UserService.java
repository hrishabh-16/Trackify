package com.trackify.service;

import com.trackify.dto.request.RegisterRequest;
import com.trackify.dto.response.UserResponse;
import com.trackify.entity.User;
import com.trackify.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserService {
    
    // User CRUD operations
    UserResponse createUser(RegisterRequest registerRequest);
    UserResponse getUserById(Long id);
    UserResponse getUserByEmail(String email);
    UserResponse updateUser(Long id, RegisterRequest updateRequest);
    void deleteUser(Long id);
    
    // User search and filtering
    Page<UserResponse> getAllUsers(Pageable pageable);
    Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable);
    Page<UserResponse> getUsersByStatus(Boolean isEnabled, Pageable pageable);
    Page<UserResponse> searchUsers(String keyword, Pageable pageable);
    List<UserResponse> getUsersByEmailVerificationStatus(Boolean emailVerified);
    
    // User status management
    UserResponse enableUser(Long id);
    UserResponse disableUser(Long id);
    UserResponse changeUserRole(Long id, UserRole newRole);
    
    // Email verification
    UserResponse verifyEmail(String token);
    void sendEmailVerification(Long userId);
    void resendEmailVerification(String email);
    
    // Password management
    void initiatePasswordReset(String email);
    UserResponse resetPassword(String token, String newPassword);
    UserResponse changePassword(Long userId, String currentPassword, String newPassword);
    
    // User profile management
    UserResponse updateProfile(Long userId, String firstName, String lastName, String phoneNumber);
    UserResponse updateAvatar(Long userId, String avatarUrl);
    
    // Authentication support
    void updateLastLoginTime(Long userId);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Statistics and analytics
    long getTotalUsersCount();
    long getUsersCountByRole(UserRole role);
    long getNewUsersCount(LocalDateTime since);
    List<UserResponse> getInactiveUsers(LocalDateTime since);
    
    // Utility methods
    User convertToEntity(RegisterRequest registerRequest);
    UserResponse convertToResponse(User user);
    List<UserResponse> convertToResponseList(List<User> users);
    
    // Validation
    void validateUserExists(Long userId);
    void validateEmailAvailable(String email);
    void validateEmailAvailable(String email, Long excludeUserId);
}