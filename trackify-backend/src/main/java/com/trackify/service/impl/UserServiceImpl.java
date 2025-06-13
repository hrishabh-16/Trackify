package com.trackify.service.impl;

import com.trackify.dto.request.RegisterRequest;
import com.trackify.dto.response.UserResponse;
import com.trackify.entity.User;
import com.trackify.enums.UserRole;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.UserRepository;
import com.trackify.service.EmailService;
import com.trackify.service.UserService;
import com.trackify.validator.UserValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private UserValidator userValidator;
    
    @Autowired
    private EmailService emailService;
    
    @Override
    public UserResponse createUser(RegisterRequest registerRequest) {
    	logger.info("Creating new user with email: {}", registerRequest.getEmail());
        
        // Validate registration request
        List<String> validationErrors = userValidator.validateRegistration(registerRequest);
        if (!validationErrors.isEmpty()) {
            throw new BadRequestException("Validation failed: " + String.join(", ", validationErrors));
        }
        
        // Create user entity
        User user = convertToEntity(registerRequest);
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Send email verification
        try {
            emailService.sendEmailVerification(savedUser.getEmail(), savedUser.getEmailVerificationToken());
        } catch (Exception e) {
        	logger.error("Failed to send email verification to: {}", savedUser.getEmail(), e);
        }
        
        logger.info("User created successfully with id: {}", savedUser.getId());
        return convertToResponse(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return convertToResponse(user);
    }
    
    @Override
    public UserResponse updateUser(Long id, RegisterRequest updateRequest) {
    	logger.info("Updating user with id: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
     // Validate email availability if changed
        if (!existingUser.getEmail().equals(updateRequest.getEmail())) {
            validateEmailAvailable(updateRequest.getEmail(), id);
        }
        
        // Update user fields
        existingUser.setEmail(updateRequest.getEmail());
        existingUser.setFirstName(updateRequest.getFirstName());
        existingUser.setLastName(updateRequest.getLastName());
        existingUser.setPhoneNumber(updateRequest.getPhoneNumber());
        
        if (StringUtils.hasText(updateRequest.getPassword())) {
            List<String> passwordErrors = userValidator.validatePassword(updateRequest.getPassword());
            if (!passwordErrors.isEmpty()) {
                throw new BadRequestException("Password validation failed: " + String.join(", ", passwordErrors));
            }
            existingUser.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        
        User updatedUser = userRepository.save(existingUser);
        logger.info("User updated successfully with id: {}", updatedUser.getId());
        
        return convertToResponse(updatedUser);
    }
    
    @Override
    public void deleteUser(Long id) {
    	logger.info("Deleting user with id: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        
        userRepository.deleteById(id);
        logger.info("User deleted successfully with id: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        Page<User> users = userRepository.findByRole(role, pageable);
        return users.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByStatus(Boolean isEnabled, Pageable pageable) {
        Page<User> users = userRepository.findByIsEnabled(isEnabled, pageable);
        return users.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        Page<User> users = userRepository.findByKeyword(keyword, pageable);
        return users.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByEmailVerificationStatus(Boolean emailVerified) {
        List<User> users = userRepository.findByEmailVerified(emailVerified);
        return convertToResponseList(users);
    }
    
    @Override
    public UserResponse enableUser(Long id) {
    	logger.info("Enabling user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setIsEnabled(true);
        User updatedUser = userRepository.save(user);
        
        logger.info("User enabled successfully with id: {}", id);
        return convertToResponse(updatedUser);
    }
    
    @Override
    public UserResponse disableUser(Long id) {
    	logger.info("Disabling user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setIsEnabled(false);
        User updatedUser = userRepository.save(user);
        
        logger.info("User disabled successfully with id: {}", id);
        return convertToResponse(updatedUser);
    }
    
    @Override
    public UserResponse changeUserRole(Long id, UserRole newRole) {
    	logger.info("Changing role for user with id: {} to {}", id, newRole);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        
        logger.info("User role changed successfully for id: {}", id);
        return convertToResponse(updatedUser);
    }
    
    @Override
    public UserResponse verifyEmail(String token) {
    	logger.info("Verifying email with token: {}", token);
        
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid email verification token"));
        
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        User updatedUser = userRepository.save(user);
        
        logger.info("Email verified successfully for user: {}", user.getEmail());
        return convertToResponse(updatedUser);
    }
    
    @Override
    public void sendEmailVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (user.getEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        
        String token = UUID.randomUUID().toString();
        user.setEmailVerificationToken(token);
        userRepository.save(user);
        
        emailService.sendEmailVerification(user.getEmail(), token);
        logger.info("Email verification sent to: {}", user.getEmail());
    }
    
    @Override
    public void resendEmailVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        sendEmailVerification(user.getId());
    }
    
    @Override
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hours expiry
        userRepository.save(user);
        
        emailService.sendPasswordResetEmail(email, token);
        logger.info("Password reset email sent to: {}", email);
    }
    
    @Override
    public UserResponse resetPassword(String token, String newPassword) {
        User user = userRepository.findByValidPasswordResetToken(token, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));
        
        List<String> passwordErrors = userValidator.validatePassword(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException("Password validation failed: " + String.join(", ", passwordErrors));
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        User updatedUser = userRepository.save(user);
        
        logger.info("Password reset successfully for user: {}", user.getEmail());
        return convertToResponse(updatedUser);
    }
    
    @Override
    public UserResponse changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        
        List<String> passwordErrors = userValidator.validatePassword(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException("Password validation failed: " + String.join(", ", passwordErrors));
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", user.getEmail());
        return convertToResponse(updatedUser);
    }
    
    @Override
    public UserResponse updateProfile(Long userId, String firstName, String lastName, String phoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (StringUtils.hasText(firstName)) {
            user.setFirstName(firstName.trim());
        }
        if (StringUtils.hasText(lastName)) {
            user.setLastName(lastName.trim());
        }
        if (StringUtils.hasText(phoneNumber)) {
            if (!userValidator.isValidPhoneFormat(phoneNumber)) {
                throw new BadRequestException("Invalid phone number format");
            }
            user.setPhoneNumber(phoneNumber.trim());
        }
        
        User updatedUser = userRepository.save(user);
        logger.info("Profile updated successfully for user: {}", user.getEmail());
        
        return convertToResponse(updatedUser);
    }
    
    @Override
    public UserResponse updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        user.setAvatarUrl(avatarUrl);
        User updatedUser = userRepository.save(user);
        
        logger.info("Avatar updated successfully for user: {}", user.getEmail());
        return convertToResponse(updatedUser);
    }
    
    @Override
    public void updateLastLoginTime(Long userId) {
        userRepository.updateLastLoginTime(userId, LocalDateTime.now());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTotalUsersCount() {
        return userRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getUsersCountByRole(UserRole role) {
        return userRepository.countByRole(role);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getNewUsersCount(LocalDateTime since) {
        return userRepository.countNewUsersAfter(since);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getInactiveUsers(LocalDateTime since) {
        List<User> inactiveUsers = userRepository.findInactiveUsers(since);
        return convertToResponseList(inactiveUsers);
    }
    
    @Override
    public User convertToEntity(RegisterRequest registerRequest) {
        // Create user with regular constructor instead of builder
        User user = new User();
        user.setUsername(registerRequest.getEmail().split("@")[0]); // Generate username from email
        user.setEmail(registerRequest.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName().trim());
        user.setLastName(registerRequest.getLastName().trim());
        user.setPhoneNumber(StringUtils.hasText(registerRequest.getPhoneNumber()) ? 
                           registerRequest.getPhoneNumber().trim() : null);
        user.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : UserRole.USER);
        user.setIsEnabled(true);
        user.setIsAccountNonExpired(true);
        user.setIsAccountNonLocked(true);
        user.setIsCredentialsNonExpired(true);
        user.setEmailVerified(false);
        
        return user;
    }
    
    @Override
    public UserResponse convertToResponse(User user) {
        UserResponse response = modelMapper.map(user, UserResponse.class);
        response.setFullName(user.getFullName());
        return response;
    }
    
    @Override
    public List<UserResponse> convertToResponseList(List<User> users) {
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }
    
    @Override
    public void validateEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered: " + email);
        }
    }
    
    @Override
    public void validateEmailAvailable(String email, Long excludeUserId) {
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(excludeUserId)) {
            throw new BadRequestException("Email is already registered: " + email);
        }
    }
}