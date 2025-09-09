package se.pbt.ddplus.subscription.service.policy;

import org.springframework.stereotype.Component;
import se.pbt.ddplus.subscription.model.Subscription;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Encapsulates normalization and equality policies
 * for subscription inputs (keywords, language).
 */
@Component
public class SubscriptionSanitizer {

    /**
     * Normalizes a list of keywords by trimming, lowercasing,
     * removing blanks, nulls, and duplicates.
     */
    public List<String> normalizeKeywords(List<String> incoming) {
        if (incoming == null) return List.of();
        return incoming.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalizeString)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Checks if two subscriptions contain the same keywords,
     * ignoring case and order.
     */
    public boolean containsSameKeywords(Subscription a, Subscription b) {
        Set<String> as = getKeywords(a).stream()
                .map(this::normalizeString)
                .collect(Collectors.toSet());

        Set<String> bs = getKeywords(b).stream()
                .map(this::normalizeString)
                .collect(Collectors.toSet());

        return as.equals(bs);
    }

    /**
     * Checks if two subscriptions use the same language,
     * ignoring case.
     */
    public boolean usesSameLanguage(Subscription a, Subscription b) {
        String la = normalizeLanguage(a);
        String lb = normalizeLanguage(b);
        return Objects.equals(la, lb);
    }


    /**
     * Extracts keywords from a subscription, or empty list if missing.
     */
    private List<String> getKeywords(Subscription s) {
        return s.getFilter() != null && s.getFilter().getKeywords() != null
                ? s.getFilter().getKeywords()
                : List.of();
    }

    /**
     * Extracts and normalizes language from a subscription.
     */
    private String normalizeLanguage(Subscription s) {
        String lang = s.getFilter() != null ? s.getFilter().getLanguage() : null;
        return lang == null ? null : normalizeString(lang);
    }

    /**
     * Lowercases a string using ROOT locale, null-safe.
     */
    private String normalizeString(String input) {
        return input == null ? null : input.toLowerCase(Locale.ROOT);
    }
}
