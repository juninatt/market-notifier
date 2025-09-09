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
     * Normalize a raw list of keywords:
     * - remove nulls/blanks
     * - trim whitespace
     * - lowercase
     * - deduplicate
     */
    public List<String> normalizeKeywords(List<String> incoming) {
        if (incoming == null) return List.of();
        return incoming.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Compare keywords case-insensitively and order-independently.
     */
    public boolean containsSameKeywords(Subscription a, Subscription b) {
        List<String> ak = a.getFilter() != null && a.getFilter().getKeywords() != null
                ? a.getFilter().getKeywords() : List.of();
        List<String> bk = b.getFilter() != null && b.getFilter().getKeywords() != null
                ? b.getFilter().getKeywords() : List.of();
        Set<String> as = ak.stream().map(k -> k.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> bs = bk.stream().map(k -> k.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return as.equals(bs);
    }

    /**
     * Compare languages case-insensitively.
     */
    public boolean usesSameLanguage(Subscription a, Subscription b) {
        String la = a.getFilter() != null ? a.getFilter().getLanguage() : null;
        String lb = b.getFilter() != null ? b.getFilter().getLanguage() : null;
        return Objects.equals(
                la == null ? null : la.toLowerCase(Locale.ROOT),
                lb == null ? null : lb.toLowerCase(Locale.ROOT)
        );
    }
}
