package se.pbt.ddplus.subscription.policy;

import org.springframework.stereotype.Component;
import se.pbt.ddplus.subscription.model.Subscription;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates unique subscription IDs within the same chat.
 * Pattern: sub-{chatId}-{slug-of-first-keyword}, appending -N on collision.
 */
@Component
public class SubscriptionIdGenerator {

    /**
     * Builds a unique ID for a subscription based on chatId and first keyword.
     *
     * @throws IllegalArgumentException if slug cannot be produced.
     */
    public String generateUniqueId(Subscription candidate, List<Subscription> existing) {
        String slug = slug(firstKeyword(candidate));
        if (slug.isBlank()) {
            throw new IllegalArgumentException("Keyword does not produce a valid slug");
        }

        String base = "sub-" + candidate.getChatId() + "-" + slug;
        Set<String> taken = collectTakenIds(existing, candidate.getChatId());
        return nextFreeId(base, taken);
    }

    /**
     * Gets the first keyword of a subscription.
     *
     * @throws  IllegalArgumentException if no keywords exist.
     */
    private String firstKeyword(Subscription s) {
        if (s.getFilter() == null || s.getFilter().getKeywords() == null || s.getFilter().getKeywords().isEmpty()) {
            throw new IllegalArgumentException("Subscription has no keywords");
        }
        return s.getFilter().getKeywords().get(0);
    }

    /**
     * Converts text to a lowercase slug safe for IDs.
     * Replaces whitespace with dashes, removes invalid characters.
     */
    private String slug(String text) {
        String t = (text == null) ? "" : text;
        return t.toLowerCase(Locale.ROOT)
                .replaceAll("[/._+]+", "-")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
    }

    /**
     * Collects all non-null subscription IDs taken within the given chat.
     */
    private Set<String> collectTakenIds(List<Subscription> existing, long chatId) {
        return existing.stream()
                .filter(s -> s.getChatId() == chatId)
                .map(Subscription::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the base ID if free, otherwise appends the next available numeric suffix.
     */
    private String nextFreeId(String base, Set<String> taken) {
        if (!taken.contains(base)) {
            return base;
        }
        int i = 2;
        String id;
        do {
            id = base + "-" + i++;
        } while (taken.contains(id));
        return id;
    }
}
