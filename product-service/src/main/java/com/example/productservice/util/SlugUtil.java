package com.example.productservice.util;

import java.text.Normalizer;

public class SlugUtil {

    public static String toSlug(String input) {
        if (input == null) return null;
        String nowhitespace = input.trim().replaceAll("\\s+", "-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        return normalized.replaceAll("[^\\w-]", "").toLowerCase();
    }
}
