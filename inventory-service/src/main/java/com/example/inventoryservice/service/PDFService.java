package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryItem;
import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.feign.UserClient;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.ProductColorResponse;
import com.example.inventoryservice.response.UserResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFService {

    private final ProductClient productClient;
    private final UserClient userClient;

    @Value("${app.pdf.directory:./pdfs}")
    private String pdfDirectory;

    public String generateExportPDF(Inventory inventory) {
        try {
            // Create PDF directory if it doesn't exist
            Path pdfPath = Paths.get(pdfDirectory);
            if (!Files.exists(pdfPath)) {
                Files.createDirectories(pdfPath);
            }

            // Get employee information
            UserResponse employee = getEmployee(inventory.getEmployeeId());

            // Generate HTML content
            String htmlContent = generateExportHTML(inventory, employee);

            // Generate PDF file name
            String fileName = "export_" + inventory.getId() + "_" + System.currentTimeMillis() + ".pdf";
            String filePath = pdfDirectory + File.separator + fileName;

            // Convert HTML to PDF
            FileOutputStream outputStream = new FileOutputStream(filePath);
            HtmlConverter.convertToPdf(htmlContent, outputStream);
            outputStream.close();

            log.info("PDF generated successfully: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Error generating PDF for inventory {}: {}", inventory.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String generateExportHTML(Inventory inventory, UserResponse employee) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Times New Roman', serif; margin: 20px; line-height: 1.6; }");
        html.append(".header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #000; padding-bottom: 20px; }");
        html.append(".header h1 { font-size: 24px; margin: 5px 0; font-weight: bold; }");
        html.append(".header h2 { font-size: 20px; margin: 5px 0; text-transform: uppercase; letter-spacing: 2px; }");
        html.append(".info-section { margin-bottom: 25px; }");
        html.append(".info-row { display: flex; margin-bottom: 10px; }");
        html.append(".info-label { width: 150px; font-weight: bold; }");
        html.append(".info-value { flex: 1; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; margin-bottom: 20px; }");
        html.append("th, td { border: 1px solid #000; padding: 10px; text-align: left; }");
        html.append("th { background-color: #f0f0f0; font-weight: bold; text-align: center; }");
        html.append("td { text-align: center; }");
        html.append(".signature-section { margin-top: 50px; display: flex; justify-content: space-between; }");
        html.append(".signature-box { width: 45%; text-align: center; }");
        html.append(".signature-line { border-top: 1px solid #000; margin-top: 60px; padding-top: 5px; }");
        html.append(".footer { margin-top: 30px; text-align: center; font-size: 12px; color: #666; }");
        html.append(".note-section { margin-top: 20px; padding: 10px; background-color: #f9f9f9; border: 1px solid #ddd; }");
        html.append("</style>");
        html.append("</head><body>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>FurniMart</h1>");
        html.append("<h2>PHIẾU XUẤT KHO</h2>");
        html.append("</div>");

        // Inventory Information
        html.append("<div class='info-section'>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Mã phiếu:</span>");
        html.append("<span class='info-value'>").append(inventory.getCode() != null ? inventory.getCode() : "N/A").append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Ngày xuất:</span>");
        html.append("<span class='info-value'>").append(inventory.getDate().format(dateFormatter)).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Kho xuất:</span>");
        html.append("<span class='info-value'>").append(inventory.getWarehouse() != null ? inventory.getWarehouse().getWarehouseName() : "N/A").append("</span>");
        html.append("</div>");
        if (employee != null) {
            html.append("<div class='info-row'>");
            html.append("<span class='info-label'>Nhân viên xuất:</span>");
            html.append("<span class='info-value'>").append(employee.getFullName() != null ? employee.getFullName() : "N/A").append("</span>");
            html.append("</div>");
        }
        if (inventory.getOrderId() != null) {
            html.append("<div class='info-row'>");
            html.append("<span class='info-label'>Mã đơn hàng:</span>");
            html.append("<span class='info-value'>#").append(inventory.getOrderId()).append("</span>");
            html.append("</div>");
        }
        html.append("</div>");

        // Inventory Items Table
        html.append("<div class='info-section'>");
        html.append("<h3 style='margin-bottom: 10px;'>Chi tiết sản phẩm xuất kho</h3>");
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th style='width: 5%;'>STT</th>");
        html.append("<th style='width: 30%;'>Tên sản phẩm</th>");
        html.append("<th style='width: 15%;'>Màu sắc</th>");
        html.append("<th style='width: 10%;'>Số lượng</th>");
        html.append("<th style='width: 20%;'>Vị trí kho</th>");
        html.append("<th style='width: 20%;'>Ghi chú</th>");
        html.append("</tr></thead>");
        html.append("<tbody>");

        int index = 1;
        int totalQuantity = 0;
        if (inventory.getInventoryItems() != null && !inventory.getInventoryItems().isEmpty()) {
            for (InventoryItem item : inventory.getInventoryItems()) {
                try {
                    ProductColorResponse productColor = getProductColor(item.getProductColorId());
                    String productName = productColor != null && productColor.getProduct() != null 
                        ? productColor.getProduct().getName() : "N/A";
                    String colorName = productColor != null && productColor.getColor() != null 
                        ? productColor.getColor().getColorName() : "N/A";
                    
                    String locationCode = item.getLocationItem() != null 
                        ? item.getLocationItem().getCode() : "N/A";
                    
                    int quantity = Math.abs(item.getQuantity()); // Export is negative, so we take absolute value
                    totalQuantity += quantity;
                    
                    html.append("<tr>");
                    html.append("<td>").append(index++).append("</td>");
                    html.append("<td style='text-align: left;'>").append(productName).append("</td>");
                    html.append("<td>").append(colorName).append("</td>");
                    html.append("<td>").append(quantity).append("</td>");
                    html.append("<td>").append(locationCode).append("</td>");
                    html.append("<td>-</td>");
                    html.append("</tr>");
                } catch (Exception e) {
                    log.warn("Error getting product info for productColorId {}: {}", item.getProductColorId(), e.getMessage());
                    int quantity = Math.abs(item.getQuantity());
                    totalQuantity += quantity;
                    html.append("<tr>");
                    html.append("<td>").append(index++).append("</td>");
                    html.append("<td style='text-align: left;'>N/A</td>");
                    html.append("<td>N/A</td>");
                    html.append("<td>").append(quantity).append("</td>");
                    html.append("<td>").append(item.getLocationItem() != null ? item.getLocationItem().getCode() : "N/A").append("</td>");
                    html.append("<td>-</td>");
                    html.append("</tr>");
                }
            }
        }

        // Total row
        html.append("<tr style='font-weight: bold; background-color: #f0f0f0;'>");
        html.append("<td colspan='3' style='text-align: right;'>TỔNG CỘNG:</td>");
        html.append("<td>").append(totalQuantity).append("</td>");
        html.append("<td colspan='2'>-</td>");
        html.append("</tr>");

        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");

        // Note section
        if (inventory.getNote() != null && !inventory.getNote().isEmpty()) {
            html.append("<div class='note-section'>");
            html.append("<strong>Ghi chú:</strong> ").append(inventory.getNote());
            html.append("</div>");
        }

        // Signature section
        html.append("<div class='signature-section'>");
        html.append("<div class='signature-box'>");
        html.append("<div><strong>Người lập phiếu</strong></div>");
        html.append("<div class='signature-line'>");
        if (employee != null) {
            html.append(employee.getFullName() != null ? employee.getFullName() : "");
        }
        html.append("</div>");
        html.append("</div>");
        html.append("<div class='signature-box'>");
        html.append("<div><strong>Người xuất kho</strong></div>");
        html.append("<div class='signature-line'></div>");
        html.append("</div>");
        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Phiếu xuất kho được tạo tự động vào ").append(dateFormat.format(new Date())).append("</p>");
        html.append("<p>FurniMart - Hệ thống quản lý kho hàng</p>");
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

    private UserResponse getEmployee(String employeeId) {
        try {
            ApiResponse<UserResponse> response = userClient.getUserById(employeeId);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching employee {}: {}", employeeId, e.getMessage());
        }
        return null;
    }
}

