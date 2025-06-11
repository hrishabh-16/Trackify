package com.trackify.service.impl;

import com.trackify.dto.request.LoginRequest;
import com.trackify.dto.request.RegisterRequest;
import com.trackify.dto.response.AuthResponse;
import com.trackify.dto.response.UserResponse;
import com.trackify.entity.User;
import com.trackify.enums.UserRole;
import com.trackify.exception.UnauthorizedException;
import com.trackify.security.JwtTokenProvider;
import com.trackify.security.UserPrincipal;
import com.trackify.service.AuthService;
import com.trackify.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);


	@Autowired
	private  AuthenticationManager authenticationManager;

	@Autowired
	private  JwtTokenProvider jwtTokenProvider;

	@Autowired
	private  UserService userService;

	@Override
	public AuthResponse register(RegisterRequest registerRequest) {
		logger.info("Attempting registration for email: {}", registerRequest.getEmail());

		try {
			// Create user
			UserResponse userResponse = userService.createUser(registerRequest);

			// Auto-login after registration
			LoginRequest loginRequest = new LoginRequest();
			loginRequest.setEmail(registerRequest.getEmail());
			loginRequest.setPassword(registerRequest.getPassword());

			AuthResponse authResponse = login(loginRequest);

			logger.info("Registration and login successful for email: {}", registerRequest.getEmail());
			return authResponse;
		} catch (Exception e) {
			logger.error("Registration failed for email: {}", registerRequest.getEmail(), e);
			throw new RuntimeException("Registration failed: " + e.getMessage(), e);
		}
	}

	@Override
	public AuthResponse login(LoginRequest loginRequest) {
		logger.info("Attempting login for email: {}", loginRequest.getEmail());

		try {
			// Authenticate user
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							loginRequest.getEmail(),
							loginRequest.getPassword()
							)
					);

			UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

			// Check if user is enabled
			if (!userPrincipal.isEnabled()) {
				throw new UnauthorizedException("Account is disabled");
			}

			// Generate tokens
			String accessToken = jwtTokenProvider.generateToken(authentication);
			String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

			// Update last login time
			userService.updateLastLoginTime(userPrincipal.getId());

			// Get user details
			UserResponse userResponse = userService.getUserById(userPrincipal.getId());

			logger.info("Login successful for email: {}", loginRequest.getEmail());

			return new AuthResponse(
					accessToken,
					refreshToken,
					jwtTokenProvider.getExpirationTime(),
					userResponse
					);

		} catch (AuthenticationException e) {
			logger.error("Login failed for email: {}", loginRequest.getEmail(), e);
			throw new UnauthorizedException("Invalid email or password");
		}
	}

	@Override
	public AuthResponse refreshToken(String refreshToken) {
		logger.info("Attempting to refresh token");

		if (!jwtTokenProvider.validateToken(refreshToken)) {
			throw new UnauthorizedException("Invalid refresh token");
		}

		Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
		UserResponse userResponse = userService.getUserById(userId);

		// Generate new access token
		String newAccessToken = jwtTokenProvider.generateTokenFromUserId(userId);

		logger.info("Token refreshed successfully for user: {}", userId);

		return AuthResponse.builder()
				.accessToken(newAccessToken)
				.refreshToken(refreshToken) // Keep the same refresh token
				.tokenType("Bearer")
				.expiresIn(jwtTokenProvider.getExpirationTime())
				.user(userResponse)
				.build();
	}

	@Override
	public void logout(String token) {
		try {
			// 1. Validate token first
			if (!jwtTokenProvider.validateToken(token)) {
				logger.warn("Invalid token provided for logout");
				return;
			}

			// 2. Extract user information from token
			Long userId = jwtTokenProvider.getUserIdFromToken(token);
			String email = jwtTokenProvider.getEmailFromToken(token);

			// 3. Log the logout event
			logger.info("User {} (ID: {}) logged out successfully at {}", 
					email, userId, LocalDateTime.now());

		} catch (Exception e) {
			logger.error("Error during logout process: {}", e.getMessage());
			// Don't throw exception to prevent logout failures
		}
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(Long userId) {
		return userService.getUserById(userId);
	}

	@Override
	public UserResponse updateCurrentUser(Long userId, RegisterRequest updateRequest) {
		return userService.updateUser(userId, updateRequest);
	}

	@Override
	public void changeCurrentUserPassword(Long userId, String currentPassword, String newPassword) {
		userService.changePassword(userId, currentPassword, newPassword);
	}

	@Override
	public UserResponse verifyEmail(String token) {
		return userService.verifyEmail(token);
	}

	@Override
	public void resendEmailVerification(String email) {
		userService.resendEmailVerification(email);
	}

	@Override
	public void forgotPassword(String email) {
		userService.initiatePasswordReset(email);
	}

	@Override
	public UserResponse resetPassword(String token, String newPassword) {
		return userService.resetPassword(token, newPassword);
	}

	@Override
	public boolean validateToken(String token) {
		return jwtTokenProvider.validateToken(token);
	}

	@Override
	public Long getUserIdFromToken(String token) {
		return jwtTokenProvider.getUserIdFromToken(token);
	}

	@Override
	public AuthResponse processOAuth2Login(String email, String firstName, String lastName, String avatarUrl) {
		logger.info("Processing OAuth2 login for email: {}", email);

		Optional<User> existingUser = userService.findByEmail(email);

		if (existingUser.isPresent()) {
			// User exists, perform login
			User user = existingUser.get();

			// Update user info from OAuth2 provider if needed
			if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
				userService.updateAvatar(user.getId(), avatarUrl);
			}

			// Create authentication for existing user
			UserPrincipal userPrincipal = UserPrincipal.create(user);
			Authentication authentication = new UsernamePasswordAuthenticationToken(
					userPrincipal, null, userPrincipal.getAuthorities());

			String accessToken = jwtTokenProvider.generateToken(authentication);
			String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

			userService.updateLastLoginTime(user.getId());
			UserResponse userResponse = userService.getUserById(user.getId());

			return new AuthResponse(accessToken, refreshToken, jwtTokenProvider.getExpirationTime(), userResponse);

		} else {
			// User doesn't exist, create new user
			RegisterRequest registerRequest = new RegisterRequest();
			registerRequest.setEmail(email);
			registerRequest.setPassword("oauth2_" + System.currentTimeMillis()); // Temporary password
			registerRequest.setConfirmPassword("oauth2_" + System.currentTimeMillis());
			registerRequest.setFirstName(firstName);
			registerRequest.setLastName(lastName);
			registerRequest.setRole(UserRole.USER);
			registerRequest.setAcceptTerms(true);

			UserResponse newUser = userService.createUser(registerRequest);

			// Update avatar if provided
			if (avatarUrl != null) {
				userService.updateAvatar(newUser.getId(), avatarUrl);
			}

			// Verify email automatically for OAuth2 users
			User user = userService.findByEmail(email).get();
			user.setEmailVerified(true);
			user.setEmailVerificationToken(null);

			// Create authentication for new user
			UserPrincipal userPrincipal = UserPrincipal.create(user);
			Authentication authentication = new UsernamePasswordAuthenticationToken(
					userPrincipal, null, userPrincipal.getAuthorities());

			String accessToken = jwtTokenProvider.generateToken(authentication);
			String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

			userService.updateLastLoginTime(user.getId());
			UserResponse userResponse = userService.getUserById(user.getId());

			logger.info("OAuth2 registration and login successful for email: {}", email);
			return new AuthResponse(accessToken, refreshToken, jwtTokenProvider.getExpirationTime(), userResponse);
		}
	}
}