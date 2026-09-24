package se.pbt.mn.dispatch.filter;

import se.pbt.mn.core.news.NewsItem;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Matches a {@link NewsItem} against watchlist entries: companies and topic categories.
 * <p>
 * Unlike {@link SubscriptionFilterMatcher}, a company entry matches on either its ticker or
 * its name appearing in the title/description -- a user shouldn't need to know whether
 * "Tesla" is best expressed as a keyword or a ticker to follow it. A category has no ticker
 * equivalent and is matched the same way keywords are.
 */
public final class WatchlistMatcher {

    private WatchlistMatcher() {}

    /**
     * Whether the item matches any of the given companies.
     */
    public static boolean matchesAnyCompany(NewsItem item, List<String> companies) {
        return companies.stream().filter(Objects::nonNull).anyMatch(company -> matchesCompany(item, company));
    }

    public static boolean matchesCompany(NewsItem item, String company) {
        return matchesTicker(item, company) || containsText(item, company);
    }

    public static boolean matchesCategory(NewsItem item, String category) {
        return containsText(item, category);
    }

    private static boolean matchesTicker(NewsItem item, String company) {
        return item.tickers() != null && item.tickers().stream().anyMatch(ticker -> ticker.equalsIgnoreCase(company));
    }

    private static boolean containsText(NewsItem item, String term) {
        String haystack = (nullToEmpty(item.title()) + " " + nullToEmpty(item.description()))
                .toLowerCase(Locale.ROOT);
        return haystack.contains(term.toLowerCase(Locale.ROOT));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
