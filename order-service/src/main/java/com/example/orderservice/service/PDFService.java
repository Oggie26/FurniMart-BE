package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderDetail;
import com.example.orderservice.response.*;
import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.feign.UserClient;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFService {

    private final ProductClient productClient;

    @Value("${app.pdf.directory:./pdfs}")
    private String pdfDirectory;

    public String generateOrderPDF(Order order, UserResponse user, AddressResponse address) {
        try {
            // Create PDF directory if it doesn't exist
            Path pdfPath = Paths.get(pdfDirectory);
            if (!Files.exists(pdfPath)) {
                Files.createDirectories(pdfPath);
            }
            // Generate HTML content
            String htmlContent = generateOrderHTML(order, user, address);

            // Generate PDF file name
            String fileName = "order_" + order.getId() + "_" + System.currentTimeMillis() + ".pdf";
            String filePath = pdfDirectory + File.separator + fileName;

            // Convert HTML to PDF
            FileOutputStream outputStream = new FileOutputStream(filePath);
            HtmlConverter.convertToPdf(htmlContent, outputStream);
            outputStream.close();

            log.info("PDF generated successfully: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Error generating PDF for order {}: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String generateOrderHTML(Order order, UserResponse user, AddressResponse address) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append(".order-info { margin-bottom: 20px; }");
        html.append(".section { margin-bottom: 25px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".total { font-weight: bold; font-size: 18px; text-align: right; margin-top: 20px; }");
        html.append("</style>");
        html.append("</head><body>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>FurniMart</h1>");
        html.append("<h2>HÓA ĐƠN BÁN HÀNG</h2>");
        html.append("</div>");

        // Order Information
        html.append("<div class='order-info'>");
        html.append("<h3>Thông tin đơn hàng</h3>");
        html.append("<p><strong>Mã đơn hàng:</strong> #").append(order.getId()).append("</p>");
        html.append("<p><strong>Ngày đặt:</strong> ").append(dateFormat.format(order.getOrderDate())).append("</p>");
        html.append("<p><strong>Trạng thái:</strong> ").append(order.getStatus()).append("</p>");
        if (order.getNote() != null && !order.getNote().isEmpty()) {
            html.append("<p><strong>Ghi chú:</strong> ").append(order.getNote()).append("</p>");
        }
        html.append("</div>");

        // Customer Information
        if (user != null) {
            html.append("<div class='section'>");
            html.append("<h3>Thông tin khách hàng</h3>");
            html.append("<p><strong>Họ tên:</strong> ").append(user.getFullName() != null ? user.getFullName() : "").append("</p>");
            html.append("<p><strong>Email:</strong> ").append(user.getEmail() != null ? user.getEmail() : "").append("</p>");
            html.append("<p><strong>Số điện thoại:</strong> ").append(user.getPhone() != null ? user.getPhone() : "").append("</p>");
            html.append("</div>");
        }

        // Address Information
        if (address != null) {
            html.append("<div class='section'>");
            html.append("<h3>Địa chỉ giao hàng</h3>");
            html.append("<p>").append(address.getAddressLine() != null ? address.getAddressLine() : "").append("</p>");
            if (address.getWard() != null || address.getDistrict() != null || address.getCity() != null) {
                html.append("<p>");
                if (address.getWard() != null) {
                    html.append(address.getWard()).append(", ");
                }
                if (address.getDistrict() != null) {
                    html.append(address.getDistrict()).append(", ");
                }
                if (address.getCity() != null) {
                    html.append(address.getCity());
                }
                html.append("</p>");
            }
            html.append("</div>");
        }

        // Order Items
        html.append("<div class='section'>");
        html.append("<h3>Chi tiết sản phẩm</h3>");
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th>STT</th>");
        html.append("<th>Tên sản phẩm</th>");
        html.append("<th>Màu sắc</th>");
        html.append("<th>Số lượng</th>");
        html.append("<th>Đơn giá</th>");
        html.append("<th>Thành tiền</th>");
        html.append("</tr></thead>");
        html.append("<tbody>");

        int index = 1;
        for (OrderDetail detail : order.getOrderDetails()) {
            try {
                ProductColorResponse productColor = getProductColor(detail.getProductColorId());
                String productName = productColor != null && productColor.getProduct() != null 
                    ? productColor.getProduct().getName() : "N/A";
                String colorName = productColor != null && productColor.getColor() != null 
                    ? productColor.getColor().getColorName() : "N/A";
                
                double subtotal = detail.getPrice() * detail.getQuantity();
                
                html.append("<tr>");
                html.append("<td>").append(index++).append("</td>");
                html.append("<td>").append(productName).append("</td>");
                html.append("<td>").append(colorName).append("</td>");
                html.append("<td>").append(detail.getQuantity()).append("</td>");
                html.append("<td>").append(String.format("%,.0f", detail.getPrice())).append(" VNĐ</td>");
                html.append("<td>").append(String.format("%,.0f", subtotal)).append(" VNĐ</td>");
                html.append("</tr>");
            } catch (Exception e) {
                log.warn("Error getting product info for productColorId {}: {}", detail.getProductColorId(), e.getMessage());
                html.append("<tr>");
                html.append("<td>").append(index++).append("</td>");
                html.append("<td>N/A</td>");
                html.append("<td>N/A</td>");
                html.append("<td>").append(detail.getQuantity()).append("</td>");
                html.append("<td>").append(String.format("%,.0f", detail.getPrice())).append(" VNĐ</td>");
                html.append("<td>").append(String.format("%,.0f", detail.getPrice() * detail.getQuantity())).append(" VNĐ</td>");
                html.append("</tr>");
            }
        }

        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");

        // Total
        html.append("<div class='total'>");
        html.append("<p><strong>Tổng tiền: ").append(String.format("%,.0f", order.getTotal())).append(" VNĐ</strong></p>");
        html.append("</div>");

        // Footer
        html.append("<div style='margin-top: 50px; text-align: center; font-size: 12px; color: #666;'>");
        html.append("<p>Cảm ơn quý khách đã sử dụng dịch vụ của FurniMart!</p>");
        html.append("<p>Hóa đơn được tạo tự động vào ").append(dateFormat.format(new Date())).append("</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private ProductColorResponse getProductColor(String productColorId) {
        try {
            ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching product color {}: {}", productColorId, e.getMessage());
        }
        return null;
    }
}

