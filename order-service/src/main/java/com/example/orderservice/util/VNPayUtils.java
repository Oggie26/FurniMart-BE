package com.example.orderservice.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility class for hashing VNPay fields with HMAC SHA512.
 * Must match exactly the format used when creating VNPay payment URL.
 */
public class VNPayUtils {

    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        try {
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);

            StringBuilder sb = new StringBuilder();
            for (Iterator<String> it = fieldNames.iterator(); it.hasNext();) {
                String name = it.next();
                String value = fields.get(name);
                if (value != null && !value.isEmpty()) {
                    String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
                    sb.append(name).append('=').append(encodedValue);
                    if (it.hasNext()) {
                        sb.append('&');
                    }
                }
            }

            String dataToHash = sb.toString();

            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secretKeySpec);

            byte[] hashBytes = sha512_HMAC.doFinal(dataToHash.getBytes(StandardCharsets.UTF_8));

            StringBuilder hash = new StringBuilder();
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b));
            }

            return hash.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error hashing VNPay data", e);
        }
    }
}
