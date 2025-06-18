package com.trackify.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trackify.entity.User;
import com.trackify.enums.UserRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
public class UserPrincipal implements UserDetails {
	
    
    private Long id;
    private String email;
    
    @JsonIgnore
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
    
    
    private static final Logger logger = LoggerFactory.getLogger(UserPrincipal.class);

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    // Manually define the all-args constructor (instead of using @AllArgsConstructor)
    public UserPrincipal(
            Long id, String email, String password, String firstName, String lastName,
            UserRole role, Boolean isEnabled, Boolean isAccountNonExpired,
            Boolean isAccountNonLocked, Boolean isCredentialsNonExpired,
            Boolean emailVerified, LocalDateTime lastLoginAt
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
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Make sure this returns the correct authority
        String authority = "ROLE_" + role.name();
        logger.debug("UserPrincipal authorities: {}", authority);
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getUsername() {
        return email;
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
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
                user.getLastLoginAt()
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return isAccountNonExpired != null ? isAccountNonExpired : true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isAccountNonLocked != null ? isAccountNonLocked : true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isCredentialsNonExpired != null ? isCredentialsNonExpired : true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled != null ? isEnabled : true;
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

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

	public Boolean getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public Boolean getIsAccountNonExpired() {
		return isAccountNonExpired;
	}

	public void setIsAccountNonExpired(Boolean isAccountNonExpired) {
		this.isAccountNonExpired = isAccountNonExpired;
	}

	public Boolean getIsAccountNonLocked() {
		return isAccountNonLocked;
	}

	public void setIsAccountNonLocked(Boolean isAccountNonLocked) {
		this.isAccountNonLocked = isAccountNonLocked;
	}

	public Boolean getIsCredentialsNonExpired() {
		return isCredentialsNonExpired;
	}

	public void setIsCredentialsNonExpired(Boolean isCredentialsNonExpired) {
		this.isCredentialsNonExpired = isCredentialsNonExpired;
	}

	public Boolean getEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(Boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public LocalDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(LocalDateTime lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getId() {
		return id;
	}

	public String getPassword() {
		return password;
	}
	
	public static UserPrincipal create(User user, Map<String, Object> attributes) {
	    UserPrincipal userPrincipal = UserPrincipal.create(user);
	    userPrincipal.setAttributes(attributes);
	    return userPrincipal;
	}

}