package com.krushna.moviebooking.auth.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Stateless validation utilities for auth-service.
 * Keeps business-rule validation out of service layer.
 */
@Slf4j
@Component
public class AuthValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,}$");

    /**
     * Validates Indian mobile number format (10-digit, starts with 6-9).
     */
    public boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return false;
        boolean valid = PHONE_PATTERN.matcher(phone.trim()).matches();
        if (!valid) {
            log.warn("[AuthValidator] Invalid phone number format: '{}'", phone);
        }
        return valid;
    }

    /**
     * Validates password strength: min 6 chars, at least 1 letter and 1 digit.
     */
    public boolean isStrongPassword(String password) {
        if (password == null || password.isBlank()) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
