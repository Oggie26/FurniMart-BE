package com.example.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class VerificationService {

    private static final int OTP_LENGTH = 6;
    private static final int VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate a unique verification token for email verification
     */
    public String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate expiry time for verification token (24 hours)
     */
    public LocalDateTime generateVerificationTokenExpiry() {
        return LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS);
    }

    /**
     * Generate a unique reset token for password reset
     */
    public String generateResetToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate expiry time for reset token (1 hour)
     */
    public LocalDateTime generateResetTokenExpiry() {
        return LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS);
    }

    /**
     * Generate a 6-digit OTP code
     */
    public String generateOtpCode() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Generate expiry time for OTP (10 minutes)
     */
    public LocalDateTime generateOtpExpiry() {
        return LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
    }

    /**
     * Verify if token is expired
     */
    public boolean isTokenExpired(LocalDateTime expiry) {
        if (expiry == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(expiry);
    }

    /**
     * Verify if OTP is expired
     */
    public boolean isOtpExpired(LocalDateTime expiry) {
        if (expiry == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(expiry);
    }

    /**
     * Verify OTP code matches
     */
    public boolean verifyOtp(String storedOtp, String providedOtp) {
        if (storedOtp == null || providedOtp == null) {
            return false;
        }
        return storedOtp.equals(providedOtp);
    }
}

