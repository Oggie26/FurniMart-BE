package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentStatus;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.service.VNPayService;
import com.example.orderservice.service.inteface.OrderService;
import com.example.orderservice.util.VNPayUtils;
import com.example.orderservice.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    private final VNPayService vnPayService;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/vnpay")
    public String createPayment(@RequestParam Double amount,
            @RequestParam Long orderId) throws Exception {
        String returnUrl = "http://localhost:8085/api/v1/payment/vnpay-return";
        return vnPayService.createPaymentUrl(orderId, amount, returnUrl);
    }

    @PostMapping("/retry")
    public ApiResponse<String> retryPayment(@RequestBody com.example.orderservice.request.RetryPaymentRequest request)
            throws Exception {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == EnumProcessOrder.PAYMENT ||
                order.getStatus() == EnumProcessOrder.DELIVERED ||
                order.getStatus() == EnumProcessOrder.CANCELLED) {
            throw new AppException(ErrorCode.PAYMENT_CANNOT_BE_RETRIED);
        }

        // Ensure payment status is not PAID
        if (order.getPayment() != null
                && order.getPayment().getPaymentStatus() == com.example.orderservice.enums.PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        String returnUrl = "http://localhost:8085/api/v1/payment/vnpay-return";
        String paymentUrl = vnPayService.createPaymentUrl(order.getId(), order.getTotal(), returnUrl);

        return ApiResponse.<String>builder()
                .status(200)
                .message("Payment retry URL generated successfully")
                .data(paymentUrl)
                .build();
    }

    // @GetMapping("/vnpay-return")
    // public void vnpayReturn(@RequestParam Map<String, String> vnpParams,
    // HttpServletResponse response) throws IOException {
    //
    // String secureHash = vnpParams.remove("vnp_SecureHash");
    // vnpParams.remove("vnp_SecureHashType");
    //
    // String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
    //
    // String frontendUrl = "http://localhost:5173/payment-success";
    //
    // if (signValue.equalsIgnoreCase(secureHash)) {
    // String responseCode = vnpParams.get("vnp_ResponseCode");
    // String orderId = vnpParams.get("vnp_TxnRef");
    //
    // if ("00".equals(responseCode)) {
    // Order order = orderRepository.findById(Long.parseLong(orderId))
    // .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    // response.sendRedirect(frontendUrl + "?status=success&orderId=" +
    // URLEncoder.encode(orderId, StandardCharsets.UTF_8));
    // } else {
    // response.sendRedirect(frontendUrl + "?status=failed&code=" +
    // URLEncoder.encode(responseCode, StandardCharsets.UTF_8));
    // }
    // } else {
    // response.sendRedirect(frontendUrl + "?status=invalid");
    // }
    // }

    // @GetMapping("/vnpay-return")
    // public void vnpayReturn(@RequestParam Map<String, String> vnpParams,
    // @RequestHeader(value = "User-Agent", required = false) String userAgent,
    // HttpServletResponse response) throws IOException {
    //
    // String secureHash = vnpParams.remove("vnp_SecureHash");
    // vnpParams.remove("vnp_SecureHashType");
    //
    // String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
    // String orderId = vnpParams.get("vnp_TxnRef");
    // String responseCode = vnpParams.get("vnp_ResponseCode");
    //
    // String webUrl = "http://localhost:5173/payment-success";
    // String mobileDeepLink = "furnimartmobileapp://order-success";
    //
    // boolean isMobile = userAgent != null && (
    // userAgent.toLowerCase().contains("android") ||
    // userAgent.toLowerCase().contains("iphone") ||
    // userAgent.toLowerCase().contains("mobile")
    // );
    //
    // if (signValue.equalsIgnoreCase(secureHash)) {
    // if ("00".equals(responseCode)) {
    // if (isMobile) {
    // try {
    // orderService.updateOrderStatus(Long.parseLong(orderId),
    // EnumProcessOrder.PAYMENT);
    // System.out.println("Đơn hàng #" + orderId + " → PAYMENT");
    // } catch (Exception e) {
    // System.err.println("Lỗi cập nhật đơn hàng: " + e.getMessage());
    // }
    // response.sendRedirect(mobileDeepLink + "?status=success&orderId=" + orderId);
    // } else {
    // response.sendRedirect(webUrl + "?status=success&orderId=" +
    // URLEncoder.encode(orderId, StandardCharsets.UTF_8));
    // }
    // } else {
    // if (isMobile) {
    // response.sendRedirect(mobileDeepLink + "?status=failed&code=" +
    // responseCode);
    // } else {
    // response.sendRedirect(webUrl + "?status=failed&code=" +
    // URLEncoder.encode(responseCode, StandardCharsets.UTF_8));
    // }
    // }
    // } else {
    // if (isMobile) {
    // response.sendRedirect(mobileDeepLink + "?status=invalid");
    // } else {
    // response.sendRedirect(webUrl + "?status=invalid");
    // }
    // }
    // }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(
            @RequestParam Map<String, String> vnpParams,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/html; charset=UTF-8");

        // === KIỂM TRA CHỮ KÝ ===
        String secureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");
        String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
        String orderId = vnpParams.get("vnp_TxnRef");
        String responseCode = vnpParams.get("vnp_ResponseCode");

        if (orderId == null || orderId.isEmpty())
            orderId = "unknown";

        String webUrl = "https://furnimart-web.vercel.app/payment-success";
        String mobileDeepLink = "furnimartmobileapp://order-success";

        boolean isMobile = userAgent != null && (userAgent.toLowerCase().contains("android") ||
                userAgent.toLowerCase().contains("iphone") ||
                userAgent.toLowerCase().contains("mobile"));

        // === XỬ LÝ KẾT QUẢ ===
        if (signValue.equalsIgnoreCase(secureHash)) {
            if ("00".equals(responseCode)) {
                if (isMobile) {
                    try {
                        orderService.updateOrderStatus(Long.parseLong(orderId), EnumProcessOrder.PAYMENT);
                        Payment payment = paymentRepository.findByOrderId(Long.valueOf(orderId))
                                        .orElseThrow((() ->  new AppException(ErrorCode.ORDER_NOT_FOUND)));
                        payment.setPaymentStatus(PaymentStatus.PAID);
                        paymentRepository.save(payment);
                        System.out.println("Đơn hàng #" + orderId + " → PAYMENT");
                    } catch (Exception e) {
                        System.err.println("Lỗi cập nhật DB đơn hàng #" + orderId + ": " + e.getMessage());
                        e.printStackTrace();
                    }

                    String html = String.format(
                            """
                                    <!DOCTYPE html>
                                    <html lang="vi">
                                    <head>
                                        <meta charset="UTF-8">
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <title>Thanh toán thành công</title>
                                        <style>
                                            body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin: 0; padding: 40px; text-align: center; }
                                            .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); position: relative; }
                                            h1 { color: #3B6C46; margin: 0 0 16px; font-size: 24px; }
                                            p { color: #555; margin: 0 0 24px; font-size: 16px; }
                                            .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius: 12px; text-decoration: none; display: inline-block; font-weight: 600; font-size: 16px; }
                                            .close-btn {
                                                position: absolute; top: 10px; right: 10px;
                                                background: none; border: none; font-size: 28px;
                                                cursor: pointer; color: #999; font-weight: bold;
                                            }
                                            .close-btn:hover { color: #333; }
                                        </style>
                                    </head>
                                    <body>
                                        <div class="container">
                                            <button class="close-btn" onclick="window.location.href='%s'">×</button>
                                            <h1>Thanh toán thành công!</h1>
                                            <p>Đơn hàng #%s đã được thanh toán.</p>
                                            <a href="%s" class="btn">Quay lại ứng dụng</a>
                                        </div>
                                    </body>
                                    </html>
                                    """,
                            mobileDeepLink + "?status=success&orderId=" + orderId, orderId,
                            mobileDeepLink + "?status=success&orderId=" + orderId);

                    response.getWriter().write(html);
                } else {
                    orderService.updateOrderStatus(Long.parseLong(orderId), EnumProcessOrder.PAYMENT);
                    Payment payment = paymentRepository.findByOrderId(Long.valueOf(orderId))
                            .orElseThrow((() ->  new AppException(ErrorCode.ORDER_NOT_FOUND)));
                    payment.setPaymentStatus(PaymentStatus.PAID);
                    paymentRepository.save(payment);
                    response.sendRedirect(webUrl + "?status=success&orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8));
                }
            } else {
                if (isMobile) {
                    // HIỆN TRANG THẤT BẠI + NÚT ĐÓNG
                    String html = String.format(
                            """
                                    <!DOCTYPE html>
                                    <html lang="vi">
                                    <head>
                                        <meta charset="UTF-8">
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <title>Thanh toán thất bại</title>
                                        <style>
                                            body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin: 0; padding: 40px; text-align: center; }
                                            .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); position: relative; }
                                            h1 { color: #dc3545; margin: 0 0 16px; font-size: 24px; }
                                            p { color: #555; margin: 0 0 24px; font-size: 16px; }
                                            .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius: 12px; text-decoration: none; display: inline-block; font-weight: 600; font-size: 16px; }
                                            .close-btn { position: absolute; top: 10px; right: 10px; background: none; border: none; font-size: 28px; cursor: pointer; color: #999; }
                                        </style>
                                    </head>
                                    <body>
                                        <div class="container">
                                            <button class="close-btn" onclick="window.location.href='%s'">×</button>
                                            <h1>Thanh toán thất bại</h1>
                                            <p>Mã lỗi: %s</p>
                                            <a href="%s" class="btn">Quay lại ứng dụng</a>
                                        </div>
                                    </body>
                                    </html>
                                    """,
                            mobileDeepLink + "?status=failed&code=" + responseCode, responseCode,
                            mobileDeepLink + "?status=failed&code=" + responseCode);

                    response.getWriter().write(html);
                } else {
                    response.sendRedirect(
                            webUrl + "?status=failed&code=" + URLEncoder.encode(responseCode, StandardCharsets.UTF_8));
                }
            }
        } else {
            if (isMobile) {
                // HIỆN TRANG KHÔNG HỢP LỆ + NÚT ĐÓNG
                String html = String.format(
                        """
                                <!DOCTYPE html>
                                <html lang="vi">
                                <head>
                                    <meta charset="UTF-8">
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <title>Giao dịch không hợp lệ</title>
                                    <style>
                                        body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin: 0; padding: 40px; text-align: center; }
                                        .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); position: relative; }
                                        h1 { color: #dc3545; margin: 0 0 16px; font-size: 24px; }
                                        p { color: #555; margin: 0 0 24px; font-size: 16px; }
                                        .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius: 12px; text-decoration: none; display: inline-block; font-weight: 600; font-size: 16px; }
                                        .close-btn { position: absolute; top: 10px; right: 10px; background: none; border: none; font-size: 28px; cursor: pointer; color: #999; }
                                    </style>
                                </head>
                                <body>
                                    <div class="container">
                                        <button class="close-btn" onclick="window.location.href='%s'">×</button>
                                        <h1>Giao dịch không hợp lệ</h1>
                                        <p>Chữ ký không khớp.</p>
                                        <a href="%s" class="btn">Quay lại ứng dụng</a>
                                    </div>
                                </body>
                                </html>
                                """,
                        mobileDeepLink + "?status=invalid", mobileDeepLink + "?status=invalid");

                response.getWriter().write(html);
            } else {
                response.sendRedirect(webUrl + "?status=invalid");
            }
        }
    }

    // @GetMapping("/vnpay-return")
    // public void vnpayReturn(
    // @RequestParam Map<String, String> vnpParams,
    // @RequestHeader(value = "User-Agent", required = false) String userAgent,
    // HttpServletResponse response) throws IOException {
    //
    // response.setContentType("text/html; charset=UTF-8");
    //
    // String secureHash = vnpParams.remove("vnp_SecureHash");
    // vnpParams.remove("vnp_SecureHashType");
    // String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
    // String orderId = vnpParams.get("vnp_TxnRef");
    // String responseCode = vnpParams.get("vnp_ResponseCode");
    //
    // if (orderId == null || orderId.isEmpty()) orderId = "unknown";
    //
    //
    // boolean isMobile = userAgent != null && (
    // userAgent.toLowerCase().contains("android") ||
    // userAgent.toLowerCase().contains("iphone") ||
    // userAgent.toLowerCase().contains("mobile")
    // );
    //
    // if (!isMobile) {
    // response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Chỉ hỗ trợ mobile");
    // return;
    // }
    //
    // String deepLink = "furnimartmobileapp://order-success";
    // String title = "Giao dịch không hợp lệ";
    // String message = "Chữ ký không khớp.";
    // String color = "#dc3545";
    // String redirectUrl = deepLink + "?status=invalid";
    //
    // // === XỬ LÝ KẾT QUẢ ===
    // if (signValue.equalsIgnoreCase(secureHash)) {
    // if ("00".equals(responseCode)) {
    // // CẬP NHẬT DB
    // try {
    // orderService.updateOrderStatus(Long.parseLong(orderId),
    // EnumProcessOrder.PAYMENT);
    // System.out.println("Đơn hàng #" + orderId + " → PAYMENT");
    // } catch (Exception e) {
    // System.err.println("Lỗi cập nhật: " + e.getMessage());
    // }
    //
    // title = "Thanh toán thành công!";
    // message = "Đơn hàng #" + orderId + " đã được thanh toán.";
    // color = "#3B6C46";
    // redirectUrl = deepLink + "?status=success&orderId=" + orderId;
    // } else {
    // title = "Thanh toán thất bại";
    // message = "Mã lỗi: " + responseCode;
    // color = "#dc3545";
    // redirectUrl = deepLink + "?status=failed&code=" + responseCode;
    // }
    // }
    //
    // String html = String.format("""
    // <!DOCTYPE html>
    // <html lang="vi">
    // <head>
    // <meta charset="UTF-8">
    // <meta name="viewport" content="width=device-width, initial-scale=1.0">
    // <title>%s</title>
    // <style>
    // body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin:
    // 0; padding: 40px; text-align: center; }
    // .container { max-width: 400px; margin: 0 auto; background: white; padding:
    // 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    // h1 { color: %s; margin: 0 0 16px; font-size: 24px; }
    // p { color: #555; margin: 0 0 24px; font-size: 16px; }
    // .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius:
    // 12px; text-decoration: none; display: inline-block; font-weight: 600;
    // font-size: 16px; }
    // .close-btn { position: absolute; top: 20px; right: 20px; background: none;
    // border: none; font-size: 24px; cursor: pointer; color: #999; }
    // </style>
    // </head>
    // <body>
    // <button class="close-btn" onclick="window.location.href='%s'">×</button>
    // <div class="container">
    // <h1>%s</h1>
    // <p>%s</p>
    // <a href="%s" class="btn">Quay lại ứng dụng</a>
    // </div>
    // </body>
    // </html>
    // """,
    // title, color, redirectUrl, title, message, redirectUrl
    // );
    //
    // response.getWriter().write(html);
    // }

    // @GetMapping("/vnpay-return")
    // public void vnpayReturn(
    // @RequestParam Map<String, String> vnpParams,
    // @RequestHeader(value = "User-Agent", required = false) String userAgent,
    // HttpServletResponse response) throws IOException {
    //
    // response.setContentType("text/html; charset=UTF-8");
    //
    // String secureHash = vnpParams.remove("vnp_SecureHash");
    // vnpParams.remove("vnp_SecureHashType");
    //
    // String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);
    // String orderId = vnpParams.get("vnp_TxnRef");
    // String responseCode = vnpParams.get("vnp_ResponseCode");
    //
    // if (orderId == null || orderId.isEmpty()) orderId = "unknown";
    //
    // String webUrl = "http://localhost:5173/payment-success";
    //
    // // DEEP LINK CHUẨN: TRUYỀN orderId + status
    // String mobileDeepLink = "furnimartmobileapp://order-success?orderId=" +
    // orderId +
    // "&status=" + ("00".equals(responseCode) ? "success" : "failed");
    //
    // boolean isMobile = userAgent != null && (
    // userAgent.toLowerCase().contains("android") ||
    // userAgent.toLowerCase().contains("iphone") ||
    // userAgent.toLowerCase().contains("ipad") ||
    // userAgent.toLowerCase().contains("mobile")
    // );
    //
    // String redirectUrl = "";
    // String title = "Đang xử lý...";
    // String message = "Vui lòng chờ";
    // String color = "#3B6C46";
    //
    // if (signValue.equalsIgnoreCase(secureHash)) {
    // if ("00".equals(responseCode)) {
    // title = "Thanh toán thành công!";
    // message = "Đơn hàng #" + orderId + " đã được thanh toán.";
    // color = "#3B6C46";
    // } else {
    // title = "Thanh toán thất bại";
    // message = "Mã lỗi: " + responseCode;
    // color = "#dc3545";
    // }
    // } else {
    // title = "Giao dịch không hợp lệ";
    // message = "Chữ ký không khớp.";
    // color = "#dc3545";
    // }
    //
    // // redirectUrl: mobile → deep link, web → webUrl
    // redirectUrl = isMobile ? mobileDeepLink : webUrl +
    // "?status=" + ("00".equals(responseCode) ? "success" : "failed") +
    // "&orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8);
    //
    // // HTML: HIỆN 5S → TỰ ĐỘNG VỀ APP
    // String html = String.format("""
    // <!DOCTYPE html>
    // <html lang="vi">
    // <head>
    // <meta charset="UTF-8">
    // <meta name="viewport" content="width=device-width, initial-scale=1.0">
    // <title>%s</title>
    // <style>
    // body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin:
    // 0; padding: 40px; text-align: center; }
    // .container { max-width: 400px; margin: 0 auto; background: white; padding:
    // 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    // h1 { color: %s; margin: 0 0 16px; font-size: 24px; }
    // p { color: #555; margin: 0 0 24px; font-size: 16px; }
    // .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius:
    // 12px; text-decoration: none; display: inline-block; font-weight: 600;
    // font-size: 16px; }
    // .spinner { border: 4px solid #f3f3f3; border-top: 4px solid %s;
    // border-radius: 50%%; width: 40px; height: 40px; animation: spin 1s linear
    // infinite; margin: 20px auto; }
    // @keyframes spin { 0%% { transform: rotate(0deg); } 100%% { transform:
    // rotate(360deg); } }
    // .countdown { font-size: 14px; color: #666; margin-top: 10px; }
    // </style>
    // </head>
    // <body>
    // <div class="container">
    // <div class="spinner"></div>
    // <h1>%s</h1>
    // <p>%s</p>
    // <a href="%s" class="btn" id="returnBtn">Quay lại ứng dụng ngay</a>
    // <div class="countdown">Tự động quay lại sau <span
    // id="timer">5</span>s...</div>
    // </div>
    //
    // <script>
    // let timeLeft = 5;
    // const timerEl = document.getElementById('timer');
    // const redirectUrl = "%s";
    //
    // // ĐẾM NGƯỢC 5S
    // const countdown = setInterval(() => {
    // timeLeft--;
    // timerEl.textContent = timeLeft;
    // if (timeLeft <= 0) {
    // clearInterval(countdown);
    // window.location.href = redirectUrl;
    // }
    // }, 1000);
    //
    // // Bấm nút → về ngay
    // document.getElementById('returnBtn').onclick = (e) => {
    // e.preventDefault();
    // clearInterval(countdown);
    // window.location.href = redirectUrl;
    // };
    // </script>
    // </body>
    // </html>
    // """,
    // title, color, color, title, message,
    // redirectUrl, redirectUrl
    // );
    //
    // response.getWriter().write(html);
    // }

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
