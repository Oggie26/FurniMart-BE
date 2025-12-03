package com.example.notificationservice.service;

import com.example.notificationservice.event.OrderCreatedEvent;
import com.example.notificationservice.exception.AppException;
import com.example.notificationservice.feign.AuthClient;
import com.example.notificationservice.feign.OrderClient;
import com.example.notificationservice.feign.UserClient;
import com.example.notificationservice.response.ApiResponse;
import com.example.notificationservice.response.AuthResponse;
import com.example.notificationservice.response.OrderResponse;
import com.example.notificationservice.response.UserResponse;
import com.example.notificationservice.enums.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOrderService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserClient userClient;
    private final OrderClient orderClient;
    private final AuthClient authClient;

    public void sendMailToCreateOrderSuccess(OrderCreatedEvent event) {
        try {
            String link = "http://localhost:5173/orders/" + event.getOrderId();
            String button = "Xem chi tiết đơn hàng";


            Context context = new Context();
            context.setVariable("name", event.getFullName());
            context.setVariable("button", button);
            context.setVariable("link", link);
            context.setVariable("orderDate", event.getOrderDate());
            context.setVariable("paymentMethod", event.getPaymentMethod());
            context.setVariable("totalAmount", event.getTotalPrice());
            {log.error(event.getEmail());}
            context.setVariable("items", event.getItems());
            String htmlContent = templateEngine.process("ordercreatesuccess", context);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("namphse173452@fpt.edu.vn", "FurniMart");
            helper.setTo(event.getEmail());
            helper.setSubject("Đơn hàng #" + event.getOrderId() + " của bạn đã được thanh toán thành công!");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Email đơn hàng gửi thành công tới {}", event.getEmail());

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi gửi email: " + e.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi xử lý dữ liệu email: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

}
