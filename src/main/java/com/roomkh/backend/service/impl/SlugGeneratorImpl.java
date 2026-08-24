package com.roomkh.backend.service.impl;

import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.service.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SlugGeneratorImpl implements SlugGenerator {

    private static final int MAX_SLUG_LENGTH = 300;
    private static final int SUFFIX_LENGTH = 8;
    private static final Pattern NON_LATIN = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s-]+");

    private final PropertyRepository propertyRepository;

    @Override
    public String generateUniqueSlug(String title) {
        String base = toBaseSlug(title);

        String candidate;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, SUFFIX_LENGTH);
            int maxBaseLength = MAX_SLUG_LENGTH - suffix.length() - 1;
            String trimmedBase = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
            trimmedBase = trimHyphens(trimmedBase);
            candidate = trimmedBase.isEmpty() ? "property-" + suffix : trimmedBase + "-" + suffix;
        } while (propertyRepository.existsBySlug(candidate));

        return candidate;
    }

    private String toBaseSlug(String title) {
        if (title == null) {
            return "";
        }

        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        String withoutSpecialChars = NON_LATIN.matcher(normalized).replaceAll("");
        String hyphenated = WHITESPACE.matcher(withoutSpecialChars.trim()).replaceAll("-");
        return trimHyphens(hyphenated);
    }

    private String trimHyphens(String value) {
        return value.replaceAll("^-+", "").replaceAll("-+$", "");
    }
}