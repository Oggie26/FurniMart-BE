package com.example.notificationservice.service;

import com.example.notificationservice.event.AccountPlaceEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendMailRegisterSuccess(AccountPlaceEvent event) {
        try {

            String link = "http://localhost:5173/";
            String button = "TRANG CHỦ";
            Context context = new Context();
            context.setVariable("name", event.getFullName());
            context.setVariable("button", button);
            context.setVariable("link", link);

            String htmlContent = templateEngine.process("registersuccess", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom("namphse173452@fpt.edu.vn");
            helper.setTo(event.getEmail());
            helper.setSubject("Chào mừng bạn trở thành viên FurniMart");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Đã gửi email thành công");
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    public void sendMailForgotPassword(AccountPlaceEvent event, String resetToken) {
        try {
            String link = "http://localhost:5173/reset-password?token=" + resetToken;
            String button = "ĐẶT LẠI MẬT KHẨU";

            Context context = new Context();
            context.setVariable("name", event.getFullName());
            context.setVariable("button", button);
            context.setVariable("link", link);

            String htmlContent = templateEngine.process("forgotpassword", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("namphse173452@fpt.edu.vn");
            helper.setTo(event.getEmail());
            helper.setSubject("Khôi phục mật khẩu FurniMart");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("✅ Đã gửi email quên mật khẩu thành công");
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Lỗi khi gửi email: " + e.getMessage());
        }
    }

}
