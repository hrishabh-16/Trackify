package com.trackify.security;

import com.trackify.entity.User;
import com.trackify.enums.UserRole;
import com.trackify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {
	
	private static final Logger logger = LoggerFactory.getLogger(OAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            logger.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // Extract user info based on provider
        OAuth2UserInfo oauth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);
        
        if (oauth2UserInfo.getEmail() == null || oauth2UserInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oauth2UserInfo.getEmail());
        User user;
        
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update existing user with OAuth2 info if needed
            user = updateExistingUser(user, oauth2UserInfo);
        } else {
            // Register new user
            user = registerNewUser(oauth2UserInfo, registrationId);
        }

        // Return OAuth2UserPrincipal instead of UserPrincipal
        return OAuth2UserPrincipal.create(user, attributes);
    }

    private User registerNewUser(OAuth2UserInfo oauth2UserInfo, String provider) {
        User user = new User();
        user.setEmail(oauth2UserInfo.getEmail());
        user.setFirstName(oauth2UserInfo.getFirstName());
        user.setLastName(oauth2UserInfo.getLastName());
        user.setAvatarUrl(oauth2UserInfo.getImageUrl());
        user.setEmailVerified(true); // OAuth2 emails are typically pre-verified
        user.setRole(UserRole.USER);
        user.setIsEnabled(true);
        user.setIsAccountNonExpired(true);
        user.setIsAccountNonLocked(true);
        user.setIsCredentialsNonExpired(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        
        // Set a random password (not used for OAuth2 users)
        user.setPassword("oauth2_" + System.currentTimeMillis());
        
        logger.info("Registering new OAuth2 user with email: {} from provider: {}", 
                oauth2UserInfo.getEmail(), provider);
        
        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oauth2UserInfo) {
        // Update user info from OAuth2 if fields are empty
        if (existingUser.getFirstName() == null || existingUser.getFirstName().isEmpty()) {
            existingUser.setFirstName(oauth2UserInfo.getFirstName());
        }
        if (existingUser.getLastName() == null || existingUser.getLastName().isEmpty()) {
            existingUser.setLastName(oauth2UserInfo.getLastName());
        }
        if (existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isEmpty()) {
            existingUser.setAvatarUrl(oauth2UserInfo.getImageUrl());
        }
        
        // Mark email as verified for OAuth2 users
        existingUser.setEmailVerified(true);
        existingUser.setLastLoginAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        logger.info("Updating existing user with email: {}", oauth2UserInfo.getEmail());
        
        return userRepository.save(existingUser);
    }
}

// OAuth2 User Info classes (same as before)
abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();
    public abstract String getEmail();
    public abstract String getFirstName();
    public abstract String getLastName();
    public abstract String getImageUrl();
}

class GoogleOAuth2UserInfo extends OAuth2UserInfo {
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getFirstName() {
        return (String) attributes.get("given_name");
    }

    @Override
    public String getLastName() {
        return (String) attributes.get("family_name");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}

class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException("Login with " + registrationId + " is not supported");
        }
    }
}