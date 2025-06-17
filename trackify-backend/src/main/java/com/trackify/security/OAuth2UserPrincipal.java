package com.trackify.security;

import com.trackify.entity.User;
import com.trackify.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class OAuth2UserPrincipal implements OAuth2User {
    
    private Long id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private UserRole role;
    private Boolean isEnabled;
    private Boolean isAccountNonExpired;
    private Boolean isAccountNonLocked;
    private Boolean isCredentialsNonExpired;
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private Map<String, Object> attributes;

    public OAuth2UserPrincipal(
            Long id, String email, String password, String firstName, String lastName,
            UserRole role, Boolean isEnabled, Boolean isAccountNonExpired,
            Boolean isAccountNonLocked, Boolean isCredentialsNonExpired,
            Boolean emailVerified, LocalDateTime lastLoginAt,
            Map<String, Object> attributes
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isEnabled = isEnabled;
        this.isAccountNonExpired = isAccountNonExpired;
        this.isAccountNonLocked = isAccountNonLocked;
        this.isCredentialsNonExpired = isCredentialsNonExpired;
        this.emailVerified = emailVerified;
        this.lastLoginAt = lastLoginAt;
        this.attributes = attributes;
    }

    public static OAuth2UserPrincipal create(User user, Map<String, Object> attributes) {
        return new OAuth2UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getIsEnabled(),
                user.getIsAccountNonExpired(),
                user.getIsAccountNonLocked(),
                user.getIsCredentialsNonExpired(),
                user.getEmailVerified(),
                user.getLastLoginAt(),
                attributes
        );
    }

    // OAuth2User interface methods
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return email;
    }

    // Additional methods to match UserPrincipal functionality
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public Boolean getIsAccountNonExpired() {
        return isAccountNonExpired;
    }

    public Boolean getIsAccountNonLocked() {
        return isAccountNonLocked;
    }

    public Boolean getIsCredentialsNonExpired() {
        return isCredentialsNonExpired;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(UserRole role) {
        return this.role == role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isUser() {
        return role == UserRole.USER;
    }

    public boolean isViewer() {
        return role == UserRole.VIEWER;
    }

    // UserDetails-like methods for compatibility
    public String getUsername() {
        return email;
    }

    public boolean isAccountNonExpired() {
        return isAccountNonExpired != null ? isAccountNonExpired : true;
    }

    public boolean isAccountNonLocked() {
        return isAccountNonLocked != null ? isAccountNonLocked : true;
    }

    public boolean isCredentialsNonExpired() {
        return isCredentialsNonExpired != null ? isCredentialsNonExpired : true;
    }

    public boolean isEnabled() {
        return isEnabled != null ? isEnabled : true;
    }
}