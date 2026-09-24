package se.pbt.mn.dispatch.filter;

import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matches a {@link NewsItem} against a {@link SubscriptionFilter}.
 * <p>
 * Each non-empty filter category (keywords, tickers, language) must match for the item to
 * be included; within a category, any one of multiple values matching is enough (OR).
 * Language is only enforced when the item declares one — most providers don't tag language,
 * and an unknown language shouldn't silently exclude an otherwise-matching item.
 * <p>
 * {@code companies} and {@code categories} are matched separately via
 * {@link #matchesAnyCompany} and {@link #matchesCategory} — they're independent of
 * {@link #matches}, not additional categories ANDed into it.
 */
public final class SubscriptionFilterMatcher {

    private SubscriptionFilterMatcher() {}

    public static boolean matches(NewsItem item, SubscriptionFilter filter) {
        return matchesKeywords(item, filter.getKeywords())
                && matchesTickers(item, filter.getTickers())
                && matchesLanguage(item, filter.getLanguage());
    }

    /**
     * Whether the item matches any of the given companies, each matched on ticker OR name
     * (unlike {@link #matches}, where keywords and tickers are separate, ANDed categories).
     */
    public static boolean matchesAnyCompany(NewsItem item, List<String> companies) {
        if (companies == null || companies.isEmpty()) {
            return false;
        }
        return companies.stream().filter(Objects::nonNull).anyMatch(company -> matchesCompany(item, company));
    }

    private static boolean matchesCompany(NewsItem item, String company) {
        return matchesTicker(item, company) || matchesText(item, company);
    }

    /**
     * Whether the item's title or description contains the given free-text category term.
     */
    public static boolean matchesCategory(NewsItem item, String category) {
        return matchesText(item, category);
    }

    private static boolean matchesTicker(NewsItem item, String ticker) {
        return item.tickers() != null && item.tickers().stream().anyMatch(t -> t.equalsIgnoreCase(ticker));
    }

    private static boolean matchesKeywords(NewsItem item, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        return keywords.stream().filter(Objects::nonNull).anyMatch(keyword -> matchesText(item, keyword));
    }

    private static boolean matchesText(NewsItem item, String term) {
        String haystack = (nullToEmpty(item.title()) + " " + nullToEmpty(item.description()))
                .toLowerCase(Locale.ROOT);
        return haystack.contains(term.toLowerCase(Locale.ROOT));
    }

    private static boolean matchesTickers(NewsItem item, List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return true;
        }
        if (item.tickers() == null || item.tickers().isEmpty()) {
            return false;
        }

        Set<String> itemTickers = item.tickers().stream()
                .map(t -> t.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return tickers.stream()
                .filter(Objects::nonNull)
                .map(t -> t.toUpperCase(Locale.ROOT))
                .anyMatch(itemTickers::contains);
    }

    private static boolean matchesLanguage(NewsItem item, String language) {
        if (language == null || language.isBlank()) {
            return true;
        }
        if (item.language() == null || item.language().isBlank()) {
            return true;
        }
        return language.equalsIgnoreCase(item.language());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
