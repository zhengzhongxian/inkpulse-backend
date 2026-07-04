package com.inkpulse.corehelpers;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugHelper {
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private SlugHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        
        // 1. Normalize diacritics (remove accents)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");

        // 2. Handle Vietnamese specific letters 'đ' and 'Đ'
        withoutDiacritics = withoutDiacritics
                .replace("đ", "d")
                .replace("Đ", "D")
                .replace("ō", "o") // Sometimes specific characters aren't covered
                .replace("Ō", "O");

        // 3. Convert whitespace to dash
        String slug = WHITESPACE.matcher(withoutDiacritics).replaceAll("-");

        // 4. Remove non-latin and non-dash/non-alphanumeric chars
        slug = NONLATIN.matcher(slug).replaceAll("");

        // 5. Lowercase and clean up multiple dashes
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }
}
