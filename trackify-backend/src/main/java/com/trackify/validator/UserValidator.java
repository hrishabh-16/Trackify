package com.trackify.validator;

import com.trackify.dto.request.RegisterRequest;
import com.trackify.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class UserValidator {
    @Autowired
    private  UserRepository userRepository;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]\\d{1,14}$"
    );
    
    public List<String> validateRegistration(RegisterRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Validate email
        if (!StringUtils.hasText(request.getEmail())) {
            errors.add("Email is required");
        } else if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            errors.add("Email format is invalid");
        } else if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            errors.add("Email is already registered");
        }
        
        // Validate password
        if (!StringUtils.hasText(request.getPassword())) {
            errors.add("Password is required");
        } else {
            if (request.getPassword().length() < 6) {
                errors.add("Password must be at least 6 characters long");
            }
            if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
                errors.add("Password must contain at least one uppercase letter, one lowercase letter, one number and one special character");
            }
        }
        
        // Validate confirm password
        if (!StringUtils.hasText(request.getConfirmPassword())) {
            errors.add("Confirm password is required");
        } else if (!request.getPassword().equals(request.getConfirmPassword())) {
            errors.add("Passwords do not match");
        }
        
        // Validate first name
        if (!StringUtils.hasText(request.getFirstName())) {
            errors.add("First name is required");
        } else if (request.getFirstName().trim().length() < 2) {
            errors.add("First name must be at least 2 characters long");
        } else if (request.getFirstName().trim().length() > 100) {
            errors.add("First name must not exceed 100 characters");
        }
        
        // Validate last name
        if (!StringUtils.hasText(request.getLastName())) {
            errors.add("Last name is required");
        } else if (request.getLastName().trim().length() < 2) {
            errors.add("Last name must be at least 2 characters long");
        } else if (request.getLastName().trim().length() > 100) {
            errors.add("Last name must not exceed 100 characters");
        }
        
        // Validate phone number (optional)
        if (StringUtils.hasText(request.getPhoneNumber()) && 
            !PHONE_PATTERN.matcher(request.getPhoneNumber()).matches()) {
            errors.add("Phone number format is invalid");
        }
        
        // Validate terms acceptance
        if (request.getAcceptTerms() == null || !request.getAcceptTerms()) {
            errors.add("You must accept the terms and conditions");
        }
        
        return errors;
    }
    
    public List<String> validateEmail(String email) {
        List<String> errors = new ArrayList<>();
        
        if (!StringUtils.hasText(email)) {
            errors.add("Email is required");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Email format is invalid");
        }
        
        return errors;
    }
    
    public List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        
        if (!StringUtils.hasText(password)) {
            errors.add("Password is required");
        } else {
            if (password.length() < 6) {
                errors.add("Password must be at least 6 characters long");
            }
            if (password.length() > 100) {
                errors.add("Password must not exceed 100 characters");
            }
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one uppercase letter, one lowercase letter, one number and one special character");
            }
        }
        
        return errors;
    }
    
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailIgnoreCase(email);
    }
    
    public boolean isValidEmailFormat(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public boolean isValidPasswordFormat(String password) {
        return StringUtils.hasText(password) && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public boolean isValidPhoneFormat(String phone) {
        return !StringUtils.hasText(phone) || PHONE_PATTERN.matcher(phone).matches();
    }
 }
        