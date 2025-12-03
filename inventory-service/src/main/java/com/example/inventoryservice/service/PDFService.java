package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryItem;
import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.feign.UserClient;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.ProductColorResponse;
import com.example.inventoryservice.response.UserResponse;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.layout.font.FontProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private final CloudinaryService cloudinaryService;

    @Value("${app.pdf.directory:./pdfs}")
    private String pdfDirectory;

    public String generateExportPDF(Inventory inventory) {
        // Tạo tên file duy nhất bằng timestamp để tránh trùng lặp
        String fileName = "export_" + inventory.getId() + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = pdfDirectory + File.separator + fileName;
        File pdfFile = null;

        try {
            // 1. Tạo thư mục chứa PDF tạm nếu chưa tồn tại
            Path pdfPath = Paths.get(pdfDirectory);
            if (!Files.exists(pdfPath)) {
                Files.createDirectories(pdfPath);
            }

            UserResponse employee = getEmployee(inventory.getEmployeeId());

            String htmlContent = generateExportHTML(inventory, employee);

            ConverterProperties properties = new ConverterProperties();
            FontProvider fontProvider = new DefaultFontProvider(false, true, false);
            properties.setFontProvider(fontProvider);

            // 5. Convert HTML sang PDF và lưu vào file tạm
            // Dùng try-with-resources để tự động đóng Stream
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                HtmlConverter.convertToPdf(htmlContent, outputStream, properties);
            }

            // 6. Upload file PDF lên Cloudinary
            pdfFile = new File(filePath);
            String publicId = "export_inventory_" + inventory.getId();

            // Upload file PDF gốc (không convert sang ảnh để giữ chất lượng và text)
            String cloudinaryUrl = cloudinaryService.uploadPDF(convertPdfToImage(pdfFile), publicId);

            log.info("☁️ PDF uploaded to Cloudinary successfully: {}", cloudinaryUrl);
            return cloudinaryUrl;

        } catch (Exception e) {
            log.error("Error generating PDF for inventory {}: {}", inventory.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        } finally {
            // 7. Quan trọng: Xóa file tạm sau khi upload xong để giải phóng dung lượng server
            if (pdfFile != null && pdfFile.exists()) {
                try {
                    Files.delete(pdfFile.toPath());
                    log.info("Deleted temporary PDF file: {}", filePath);
                } catch (IOException ex) {
                    log.warn("Failed to delete temporary file: {}", ex.getMessage());
                }
            }
        }
    }

    public File convertPdfToImage(File pdfFile) throws Exception {
        PDDocument document = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 200); // page 0 → 200 DPI
        File imageFile = new File("converted.png");
        ImageIO.write(bim, "PNG", imageFile);

        document.close();
        return imageFile;
    }

    private String generateExportHTML(Inventory inventory, UserResponse employee) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        // Ưu tiên font Unicode phổ biến
        html.append("body { font-family: 'Times New Roman', Arial, sans-serif; margin: 20px; line-height: 1.6; }");
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
        // Xử lý null date an toàn
        String dateStr = inventory.getDate() != null ? inventory.getDate().format(dateFormatter) : dateFormat.format(new Date());
        html.append("<span class='info-value'>").append(dateStr).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Kho xuất:</span>");
        String warehouseName = inventory.getWarehouse() != null ? inventory.getWarehouse().getWarehouseName() : "N/A";
        html.append("<span class='info-value'>").append(warehouseName).append("</span>");
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

        // Table Header
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

        // Table Body - Kiểm tra null list
        if (inventory.getInventoryItems() != null && !inventory.getInventoryItems().isEmpty()) {
            for (InventoryItem item : inventory.getInventoryItems()) {
                String productName = "N/A";
                String colorName = "N/A";
                String locationCode = item.getLocationItem() != null ? item.getLocationItem().getCode() : "N/A";
                int quantity = Math.abs(item.getQuantity());

                try {
                    // Gọi Feign Client lấy thông tin chi tiết
                    ProductColorResponse productColor = getProductColor(item.getProductColorId());
                    if (productColor != null) {
                        if (productColor.getProduct() != null) productName = productColor.getProduct().getName();
                        if (productColor.getColor() != null) colorName = productColor.getColor().getColorName();
                    }
                } catch (Exception e) {
                    log.warn("Error getting product info for item {}", item.getId());
                }

                totalQuantity += quantity;

                html.append("<tr>");
                html.append("<td>").append(index++).append("</td>");
                html.append("<td style='text-align: left;'>").append(productName).append("</td>");
                html.append("<td>").append(colorName).append("</td>");
                html.append("<td>").append(quantity).append("</td>");
                html.append("<td>").append(locationCode).append("</td>");
                html.append("<td>-</td>");
                html.append("</tr>");
            }
        } else {
            html.append("<tr><td colspan='6' style='text-align:center;'>Không có sản phẩm nào</td></tr>");
        }

        // Total Row
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
        if (productColorId == null) return null;
        try {
            ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
            return (response != null) ? response.getData() : null;
        } catch (Exception e) {
            log.warn("Error fetching product color {}: {}", productColorId, e.getMessage());
            return null;
        }
    }

    private UserResponse getEmployee(String employeeId) {
        if (employeeId == null) return null;
        try {
            ApiResponse<UserResponse> response = userClient.getUserById(employeeId);
            return (response != null) ? response.getData() : null;
        } catch (Exception e) {
            log.warn("Error fetching employee {}: {}", employeeId, e.getMessage());
            return null;
        }
    }
}