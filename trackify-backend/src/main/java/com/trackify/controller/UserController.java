package com.trackify.controller;

import com.trackify.dto.request.RegisterRequest;
import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.UserResponse;
import com.trackify.enums.UserRole;
import com.trackify.security.UserPrincipal;
import com.trackify.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user management operations")
public class UserController {
    @Autowired
    private  UserService userService;
    
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieve the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        UserResponse user = userService.getUserById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", user));
    }
    
    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RegisterRequest updateRequest) {
        
        UserResponse updatedUser = userService.updateUser(currentUser.getId(), updateRequest);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }
    
    @PutMapping("/me/profile")
    @Operation(summary = "Update user profile fields", description = "Update specific profile fields")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber) {
        
        UserResponse updatedUser = userService.updateProfile(
            currentUser.getId(), firstName, lastName, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }
    
    @PutMapping("/me/avatar")
    @Operation(summary = "Update user avatar", description = "Update the avatar URL for the current user")
    public ResponseEntity<ApiResponse<UserResponse>> updateAvatar(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String avatarUrl) {
        
        UserResponse updatedUser = userService.updateAvatar(currentUser.getId(), avatarUrl);
        return ResponseEntity.ok(ApiResponse.success("Avatar updated successfully", updatedUser));
    }
    
    @PostMapping("/me/change-password")
    @Operation(summary = "Change password", description = "Change the password for the current user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        
        userService.changePassword(currentUser.getId(), currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
    
    @PostMapping("/me/resend-verification")
    @Operation(summary = "Resend email verification", description = "Resend email verification for the current user")
    public ResponseEntity<ApiResponse<Void>> resendEmailVerification(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        userService.sendEmailVerification(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Email verification sent successfully", null));
    }
    
    // Admin-only endpoints
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieve paginated list of all users (Admin only)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Operation(summary = "Get user by ID", description = "Retrieve user details by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new user", description = "Create a new user (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody RegisterRequest registerRequest) {
        
        UserResponse newUser = userService.createUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User created successfully", newUser));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update user details (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody RegisterRequest updateRequest) {
        
        UserResponse updatedUser = userService.updateUser(id, updateRequest);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete a user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users", description = "Search users by keyword (Admin only)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> users = userService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users found successfully", users));
    }
    
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get users by role", description = "Retrieve users by role (Admin only)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsersByRole(
            @Parameter(description = "User role") @PathVariable UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> users = userService.getUsersByRole(role, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get users by status", description = "Retrieve users by enabled status (Admin only)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsersByStatus(
            @Parameter(description = "User status (enabled/disabled)") @PathVariable Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> users = userService.getUsersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable user", description = "Enable a user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> enableUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        
        UserResponse user = userService.enableUser(id);
        return ResponseEntity.ok(ApiResponse.success("User enabled successfully", user));
    }
    
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable user", description = "Disable a user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> disableUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        
        UserResponse user = userService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.success("User disabled successfully", user));
    }
    
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user role", description = "Change user role (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestParam UserRole role) {
        
        UserResponse user = userService.changeUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("User role changed successfully", user));
    }
    
    @GetMapping("/unverified")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get unverified users", description = "Retrieve users with unverified emails (Admin only)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUnverifiedUsers() {
        
        List<UserResponse> users = userService.getUsersByEmailVerificationStatus(false);
        return ResponseEntity.ok(ApiResponse.success("Unverified users retrieved successfully", users));
    }
    
    @GetMapping("/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get inactive users", description = "Retrieve inactive users (Admin only)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getInactiveUsers(
            @RequestParam(defaultValue = "30") int days) {
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<UserResponse> users = userService.getInactiveUsers(since);
        return ResponseEntity.ok(ApiResponse.success("Inactive users retrieved successfully", users));
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user statistics", description = "Retrieve user statistics (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStatistics() {
        
        Map<String, Object> stats = Map.of(
            "totalUsers", userService.getTotalUsersCount(),
            "adminCount", userService.getUsersCountByRole(UserRole.ADMIN),
            "userCount", userService.getUsersCountByRole(UserRole.USER),
            "viewerCount", userService.getUsersCountByRole(UserRole.VIEWER),
            "newUsersThisMonth", userService.getNewUsersCount(LocalDateTime.now().minusMonths(1)),
            "newUsersThisWeek", userService.getNewUsersCount(LocalDateTime.now().minusWeeks(1))
        );
        
        return ResponseEntity.ok(ApiResponse.success("User statistics retrieved successfully", stats));
    }
}