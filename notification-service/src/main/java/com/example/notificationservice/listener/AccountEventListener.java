package com.example.notificationservice.listener;

import com.example.notificationservice.event.AccountPlaceEvent;
import com.example.notificationservice.event.EmailVerificationEvent;
import com.example.notificationservice.event.OtpEvent;
import com.example.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountEventListener {
    private final EmailService emailService;

    @KafkaListener(
            topics = "account-created-topic",
            groupId = "notification-group",
            containerFactory = "accountKafkaListenerContainerFactory")
    public void handleUserCreated(AccountPlaceEvent event) {
        try {
            emailService.sendMailRegisterSuccess(event);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail: " + e.getMessage());

        }
    }

    @KafkaListener(
            topics = "email-verification-topic",
            groupId = "notification-group",
            containerFactory = "emailVerificationKafkaListenerContainerFactory")
    public void handleEmailVerification(EmailVerificationEvent event) {
        try {
            emailService.sendMailEmailVerification(event);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email xác thực: " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = "forgot-password-otp-topic",
            groupId = "notification-group",
            containerFactory = "otpKafkaListenerContainerFactory")
    public void handleForgotPasswordOtp(OtpEvent event) {
        try {
            emailService.sendMailOtpCode(event);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi OTP quên mật khẩu: " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = "change-password-otp-topic",
            groupId = "notification-group",
            containerFactory = "otpKafkaListenerContainerFactory")
    public void handleChangePasswordOtp(OtpEvent event) {
        try {
            emailService.sendMailChangePasswordOtp(event);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi OTP đổi mật khẩu: " + e.getMessage());
        }
    }
}
