package se.pbt.marketnotifier.subscription.policy;

import org.springframework.stereotype.Component;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.core.subscription.SchedulePreset;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Encapsulates normalization and equality policies
 * for subscription inputs (keywords, language, schedule).
 */
@Component
public class SubscriptionSanitizer {

    /**
     * Normalizes a list of keywords by trimming, lowercasing,
     * removing blanks, nulls, and duplicates. Returns an unmodifiable list.
     */
    public List<String> normalizeKeywords(List<String> incoming) {
        if (incoming == null) return List.of();
        List<String> out = incoming.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::normalizeString)
                .distinct()
                .toList();
        return List.copyOf(out);
    }

    /**
     * Checks if two subscriptions contain the same keywords, ignoring case and order.
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
     * Checks if two subscriptions use the same language, ignoring case.
     */
    public boolean usesSameLanguage(Subscription a, Subscription b) {
        String la = normalizeLanguage(a);
        String lb = normalizeLanguage(b);
        return Objects.equals(la, lb);
    }

    /**
     * Checks if two subscriptions use the same schedule preset.
     */
    public boolean usesSameSchedule(Subscription a, Subscription b) {
        SchedulePreset sa = getSchedule(a);
        SchedulePreset sb = getSchedule(b);
        return Objects.equals(sa, sb);
    }


    /**
     * Extracts keywords from a subscription, or empty list if missing.
     */
    private List<String> getKeywords(Subscription s) {
        return (s != null && s.getFilter() != null && s.getFilter().getKeywords() != null)
                ? s.getFilter().getKeywords()
                : List.of();
    }

    /**
     * Extracts and normalizes language from a subscription (lowercased), or null if missing.
     */
    private String normalizeLanguage(Subscription s) {
        String lang = (s != null && s.getFilter() != null) ? s.getFilter().getLanguage() : null;
        return lang == null ? null : normalizeString(lang);
    }

    /**
     * Extracts schedule preset or null if missing.
     */
    private SchedulePreset getSchedule(Subscription s) {
        return (s == null) ? null : s.getSchedule();
    }

    /**
     * Lowercases a string using ROOT locale.
     */
    private String normalizeString(String input) {
        return input == null ? null : input.toLowerCase(Locale.ROOT);
    }
}
