package com.example.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationService Tests")
class VerificationServiceTest {

    @InjectMocks
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService();
    }

    @Test
    @DisplayName("Should generate unique verification token")
    void testGenerateVerificationToken() {
        // When
        String token1 = verificationService.generateVerificationToken();
        String token2 = verificationService.generateVerificationToken();

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() > 0);
        assertTrue(token2.length() > 0);
    }

    @Test
    @DisplayName("Should generate verification token expiry 24 hours from now")
    void testGenerateVerificationTokenExpiry() {
        // When
        LocalDateTime expiry = verificationService.generateVerificationTokenExpiry();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expectedMin = now.plusHours(23).plusMinutes(59);
        LocalDateTime expectedMax = now.plusHours(24).plusMinutes(1);

        // Then
        assertNotNull(expiry);
        assertTrue(expiry.isAfter(expectedMin));
        assertTrue(expiry.isBefore(expectedMax));
    }

    @Test
    @DisplayName("Should generate unique reset token")
    void testGenerateResetToken() {
        // When
        String token1 = verificationService.generateResetToken();
        String token2 = verificationService.generateResetToken();

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() > 0);
        assertTrue(token2.length() > 0);
    }

    @Test
    @DisplayName("Should generate reset token expiry 1 hour from now")
    void testGenerateResetTokenExpiry() {
        // When
        LocalDateTime expiry = verificationService.generateResetTokenExpiry();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expectedMin = now.plusMinutes(59);
        LocalDateTime expectedMax = now.plusHours(1).plusMinutes(1);

        // Then
        assertNotNull(expiry);
        assertTrue(expiry.isAfter(expectedMin));
        assertTrue(expiry.isBefore(expectedMax));
    }

    @Test
    @DisplayName("Should generate 6-digit OTP code")
    void testGenerateOtpCode() {
        // When
        String otp1 = verificationService.generateOtpCode();
        String otp2 = verificationService.generateOtpCode();

        // Then
        assertNotNull(otp1);
        assertNotNull(otp2);
        assertEquals(6, otp1.length());
        assertEquals(6, otp2.length());
        assertTrue(otp1.matches("\\d{6}"));
        assertTrue(otp2.matches("\\d{6}"));
        // OTPs should be different (very high probability)
        assertNotEquals(otp1, otp2);
    }

    @Test
    @DisplayName("Should generate OTP code in valid range (100000-999999)")
    void testGenerateOtpCodeRange() {
        // When
        String otp = verificationService.generateOtpCode();
        int otpValue = Integer.parseInt(otp);

        // Then
        assertTrue(otpValue >= 100000);
        assertTrue(otpValue <= 999999);
    }

    @Test
    @DisplayName("Should generate OTP expiry 10 minutes from now")
    void testGenerateOtpExpiry() {
        // When
        LocalDateTime expiry = verificationService.generateOtpExpiry();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expectedMin = now.plusMinutes(9);
        LocalDateTime expectedMax = now.plusMinutes(11);

        // Then
        assertNotNull(expiry);
        assertTrue(expiry.isAfter(expectedMin));
        assertTrue(expiry.isBefore(expectedMax));
    }

    @Test
    @DisplayName("Should return true for expired token")
    void testIsTokenExpired_Expired() {
        // Given
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1);

        // When
        boolean result = verificationService.isTokenExpired(expiredTime);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for valid token")
    void testIsTokenExpired_Valid() {
        // Given
        LocalDateTime validTime = LocalDateTime.now().plusHours(1);

        // When
        boolean result = verificationService.isTokenExpired(validTime);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true for null expiry")
    void testIsTokenExpired_Null() {
        // When
        boolean result = verificationService.isTokenExpired(null);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return true for expired OTP")
    void testIsOtpExpired_Expired() {
        // Given
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(1);

        // When
        boolean result = verificationService.isOtpExpired(expiredTime);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for valid OTP")
    void testIsOtpExpired_Valid() {
        // Given
        LocalDateTime validTime = LocalDateTime.now().plusMinutes(5);

        // When
        boolean result = verificationService.isOtpExpired(validTime);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true for null OTP expiry")
    void testIsOtpExpired_Null() {
        // When
        boolean result = verificationService.isOtpExpired(null);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return true for matching OTP")
    void testVerifyOtp_Matching() {
        // Given
        String storedOtp = "123456";
        String providedOtp = "123456";

        // When
        boolean result = verificationService.verifyOtp(storedOtp, providedOtp);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for non-matching OTP")
    void testVerifyOtp_NonMatching() {
        // Given
        String storedOtp = "123456";
        String providedOtp = "654321";

        // When
        boolean result = verificationService.verifyOtp(storedOtp, providedOtp);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for null stored OTP")
    void testVerifyOtp_NullStored() {
        // Given
        String storedOtp = null;
        String providedOtp = "123456";

        // When
        boolean result = verificationService.verifyOtp(storedOtp, providedOtp);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for null provided OTP")
    void testVerifyOtp_NullProvided() {
        // Given
        String storedOtp = "123456";
        String providedOtp = null;

        // When
        boolean result = verificationService.verifyOtp(storedOtp, providedOtp);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for both null OTPs")
    void testVerifyOtp_BothNull() {
        // When
        boolean result = verificationService.verifyOtp(null, null);

        // Then
        assertFalse(result);
    }
}

