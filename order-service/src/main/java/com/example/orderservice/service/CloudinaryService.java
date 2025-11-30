package com.example.orderservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadPDF(File file, String publicId) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "resource_type", "raw",  // "raw" cho file PDF
                    "public_id", publicId,
                    "folder", "furnimart/invoices"  // Lưu trong folder invoices
            ));

            String url = (String) uploadResult.get("secure_url");
            log.info("✅ PDF uploaded to Cloudinary successfully: {}", url);
            
            if (file.exists()) {
                file.delete();
                log.info("🗑️  Local file deleted: {}", file.getPath());
            }
            
            return url;
            
        } catch (IOException e) {
            log.error("❌ Error uploading PDF to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload PDF to Cloudinary: " + e.getMessage());
        }
    }


    public void deletePDF(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "raw"
            ));
            log.info("🗑️  PDF deleted from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("❌ Error deleting PDF from Cloudinary: {}", e.getMessage(), e);
        }
    }
}
