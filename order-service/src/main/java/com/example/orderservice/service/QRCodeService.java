package com.example.orderservice.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Slf4j
public class QRCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    /**
     * Generate a unique QR code string for an order
     */
    public String generateQRCodeString(Long orderId) {
        try {
            String data = "ORDER_" + orderId + "_" + LocalDateTime.now();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "QR_" + hexString.substring(0, 16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating QR code string", e);
            return "QR_" + orderId + "_" + System.currentTimeMillis();
        }
    }

    /**
     * Generate QR code image as Base64 string
     */
    public String generateQRCodeImage(String qrCodeString) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeString, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code image", e);
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    /**
     * Generate both QR code string and image for an order
     */
    public QRCodeResult generateQRCode(Long orderId) {
        String qrCodeString = generateQRCodeString(orderId);
        String qrCodeImage = generateQRCodeImage(qrCodeString);
        
        return QRCodeResult.builder()
                .qrCodeString(qrCodeString)
                .qrCodeImage(qrCodeImage)
                .build();
    }

    public static class QRCodeResult {
        private String qrCodeString;
        private String qrCodeImage;

        public static QRCodeResultBuilder builder() {
            return new QRCodeResultBuilder();
        }

        public String getQrCodeString() {
            return qrCodeString;
        }

        public void setQrCodeString(String qrCodeString) {
            this.qrCodeString = qrCodeString;
        }

        public String getQrCodeImage() {
            return qrCodeImage;
        }

        public void setQrCodeImage(String qrCodeImage) {
            this.qrCodeImage = qrCodeImage;
        }

        public static class QRCodeResultBuilder {
            private String qrCodeString;
            private String qrCodeImage;

            public QRCodeResultBuilder qrCodeString(String qrCodeString) {
                this.qrCodeString = qrCodeString;
                return this;
            }

            public QRCodeResultBuilder qrCodeImage(String qrCodeImage) {
                this.qrCodeImage = qrCodeImage;
                return this;
            }

            public QRCodeResult build() {
                QRCodeResult result = new QRCodeResult();
                result.setQrCodeString(this.qrCodeString);
                result.setQrCodeImage(this.qrCodeImage);
                return result;
            }
        }
    }
}
