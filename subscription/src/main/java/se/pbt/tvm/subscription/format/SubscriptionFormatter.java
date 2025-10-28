package se.pbt.tvm.subscription.format;

import org.springframework.stereotype.Component;
import se.pbt.tvm.subscription.model.Subscription;
import se.pbt.tvm.subscription.policy.SubscriptionSanitizer;

import java.util.List;

/**
 * Converts {@link Subscription} objects into clean, human-readable text lines.
 * <p>
 * The formatter is responsible only for presentation — not modification or validation.
 * Any normalization logic belongs in {@link SubscriptionSanitizer}.
 */
@Component
public class SubscriptionFormatter {

    /**
     * Produces a single-line summary of the subscription, including its ID,
     * keywords, language, and enabled status.
     */
    public String format(Subscription s) {
        if (s == null) {
            return "(invalid subscription)";
        }

        String id = nonNullOr(s.getId(), "(no id)");
        String keywords = joinKeywords(s);
        String language = (s.getFilter() != null)
                ? nonNullOr(s.getFilter().getLanguage(), "(unknown)")
                : "(unknown)";
        String enabled = String.valueOf(s.isEnabled());

        return String.format("ID: %s | Keywords: %s | Lang: %s | Enabled: %s",
                id, keywords, language, enabled);
    }

    /**
     * Extracts and formats keywords from the subscription's filter.
     * Keywords are joined with commas, preserving their original order.
     */
    private String joinKeywords(Subscription s) {
        List<String> keywords = (s.getFilter() != null) ? s.getFilter().getKeywords() : null;
        return (keywords == null || keywords.isEmpty())
                ? "(no keywords)"
                : String.join(", ", keywords);
    }

    /**
     * Replaces {@code null} or blank values with a defined fallback string.
     */
    private String nonNullOr(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
