package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.VNPayService;
import com.example.orderservice.util.VNPayUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final OrderRepository orderRepository;
    private final UserClient userClient;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    private final VNPayService vnPayService;

    @GetMapping("/vnpay")
    public String createPayment(@RequestParam Double amount,
                                @RequestParam Long orderId) throws Exception {
        String returnUrl = "http://localhost:8085/api/v1/payment/vnpay-return";
        return vnPayService.createPaymentUrl(orderId, amount, returnUrl);
    }

//    @GetMapping("/vnpay-return")
//    public void vnpayReturn(@RequestParam Map<String, String> vnpParams,
//                            HttpServletResponse response) throws IOException {
//
//        String secureHash = vnpParams.remove("vnp_SecureHash");
//        vnpParams.remove("vnp_SecureHashType");
//
//        String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
//
//        String frontendUrl = "http://localhost:5173/payment-success";
//
//        if (signValue.equalsIgnoreCase(secureHash)) {
//            String responseCode = vnpParams.get("vnp_ResponseCode");
//            String orderId = vnpParams.get("vnp_TxnRef");
//
//            if ("00".equals(responseCode)) {
//                Order order = orderRepository.findById(Long.parseLong(orderId))
//                                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//                response.sendRedirect(frontendUrl + "?status=success&orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8));
//            } else {
//                response.sendRedirect(frontendUrl + "?status=failed&code=" + URLEncoder.encode(responseCode, StandardCharsets.UTF_8));
//            }
//        } else {
//            response.sendRedirect(frontendUrl + "?status=invalid");
//        }
//    }

//    @GetMapping("/vnpay-return")
//    public void vnpayReturn(@RequestParam Map<String, String> vnpParams,
//                            @RequestHeader(value = "User-Agent", required = false) String userAgent,
//                            HttpServletResponse response) throws IOException {
//
//        String secureHash = vnpParams.remove("vnp_SecureHash");
//        vnpParams.remove("vnp_SecureHashType");
//
//        String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
//        String orderId = vnpParams.get("vnp_TxnRef");
//        String responseCode = vnpParams.get("vnp_ResponseCode");
//
//        String webUrl = "http://localhost:5173/payment-success";
//        String mobileDeepLink = "furnimartmobileapp://order-success";
//
//        boolean isMobile = userAgent != null && (
//                userAgent.toLowerCase().contains("android") ||
//                        userAgent.toLowerCase().contains("iphone") ||
//                        userAgent.toLowerCase().contains("mobile")
//        );
//
//        if (signValue.equalsIgnoreCase(secureHash)) {
//            if ("00".equals(responseCode)) {
//                if (isMobile) {
//                    response.sendRedirect(mobileDeepLink + "?status=success&orderId=" + orderId);
//
//
//                } else {
//                    response.sendRedirect(webUrl + "?status=success&orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8));
//                }
//            } else {
//                if (isMobile) {
//                    response.sendRedirect(mobileDeepLink + "?status=failed&code=" + responseCode);
//                } else {
//                    response.sendRedirect(webUrl + "?status=failed&code=" + URLEncoder.encode(responseCode, StandardCharsets.UTF_8));
//                }
//            }
//        } else {
//            if (isMobile) {
//                response.sendRedirect(mobileDeepLink + "?status=invalid");
//            } else {
//                response.sendRedirect(webUrl + "?status=invalid");
//            }
//        }
//    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(
            @RequestParam Map<String, String> vnpParams,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/html; charset=UTF-8");

        String secureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
        String orderId = vnpParams.get("vnp_TxnRef");
        String responseCode = vnpParams.get("vnp_ResponseCode");

        if (orderId == null || orderId.isEmpty()) orderId = "unknown";

        String webUrl = "http://localhost:5173/payment-success";
        String mobileDeepLink = "furnimartmobileapp://order-success";

        boolean isMobile = userAgent != null && (
                userAgent.toLowerCase().contains("android") ||
                        userAgent.toLowerCase().contains("iphone") ||
                        userAgent.toLowerCase().contains("ipad") ||
                        userAgent.toLowerCase().contains("mobile")
        );

        String redirectUrl = "";
        String title = "Đang xử lý...";
        String message = "Vui lòng chờ";
        String color = "#3B6C46";

        if (signValue.equalsIgnoreCase(secureHash)) {
            if ("00".equals(responseCode)) {
                title = "Thanh toán thành công!";
                message = "Đơn hàng #" + orderId + " đã được thanh toán.";
                color = "#3B6C46";
                redirectUrl = isMobile
                        ? mobileDeepLink + "?status=success&orderId=" + orderId
                        : webUrl + "?status=success&orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8);
            } else {
                title = "Thanh toán thất bại";
                message = "Mã lỗi: " + responseCode;
                color = "#dc3545";
                redirectUrl = isMobile
                        ? mobileDeepLink + "?status=failed&code=" + responseCode
                        : webUrl + "?status=failed&code=" + URLEncoder.encode(responseCode, StandardCharsets.UTF_8);
            }
        } else {
            title = "Giao dịch không hợp lệ";
            message = "Chữ ký không khớp.";
            color = "#dc3545";
            redirectUrl = isMobile
                    ? mobileDeepLink + "?status=invalid"
                    : webUrl + "?status=invalid";
        }

        // DÙNG String.format → KHÔNG LỖI %
        String html = String.format("""
        <!DOCTYPE html>
        <html lang="vi">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>%s</title>
            <style>
                body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin: 0; padding: 40px; text-align: center; }
                .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                h1 { color: %s; margin: 0 0 16px; font-size: 24px; }
                p { color: #555; margin: 0 0 24px; font-size: 16px; }
                .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius: 12px; text-decoration: none; display: inline-block; font-weight: 600; font-size: 16px; }
                .spinner { border: 4px solid #f3f3f3; border-top: 4px solid %s; border-radius: 50%%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 20px auto; }
                @keyframes spin { 
                    0%% { transform: rotate(0deg); } 
                    100%% { transform: rotate(360deg); } 
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="spinner"></div>
                <h1>%s</h1>
                <p>%s</p>
                <a href="%s" class="btn">Quay lại ứng dụng</a>
            </div>
            <script>
                setTimeout(() => {
                    window.location.href = "%s";
                }, 2000);
            </script>
        </body>
        </html>
        """, title, color, color, title, message, redirectUrl, redirectUrl);

        response.getWriter().write(html);
    }


    @GetMapping("/vnpay-mobile")
    public String createPaymentForMobile(@RequestParam Double amount,
                                         @RequestParam Long orderId) throws Exception {
        String returnUrl = "https://your-domain.com/api/v1/payment/vnpay-return-mobile";
        return vnPayService.createPaymentUrl(orderId, amount, returnUrl);
    }

    @GetMapping("/vnpay-return-mobile")
    public void vnpayReturnMobile(@RequestParam Map<String, String> vnpParams,
                                  HttpServletResponse response) throws IOException {
        String secureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
        String orderId = vnpParams.get("vnp_TxnRef");
        String responseCode = vnpParams.get("vnp_ResponseCode");

        String deepLink = "furnimartmobileapp://order-success";

        if (signValue.equalsIgnoreCase(secureHash)) {
            if ("00".equals(responseCode)) {
                response.sendRedirect(deepLink + "?status=success&orderId=" + orderId);
            } else {
                response.sendRedirect(deepLink + "?status=failed&code=" + responseCode);
            }
        } else {
            response.sendRedirect(deepLink + "?status=invalid");
        }
    }



}
