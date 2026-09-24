package se.pbt.mn.dispatch.notification;

import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.dispatch.filter.WatchlistMatcher;
import se.pbt.mn.subscription.model.Watchlist;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds the one-off startup digest {@link Notification} for a {@link Watchlist}: every
 * group matching a followed company (no limit), plus the most recently published groups
 * per followed category, capped at {@code topPerCategory}.
 * <p>
 * Returns an empty {@link Optional} when nothing matches, so the caller never sends a
 * blank digest.
 */
public final class WatchlistDigestBuilder {

    private static final String TITLE = "Your morning watchlist digest";
    private static final String COMPANIES_HEADING = "Companies";

    private WatchlistDigestBuilder() {}

    public static Optional<Notification> build(List<NewsGroup> groups, Watchlist watchlist, int topPerCategory) {
        StringBuilder body = new StringBuilder();

        List<NewsGroup> companyMatches = matchCompanies(groups, watchlist.getCompanies());
        if (!companyMatches.isEmpty()) {
            appendSection(body, COMPANIES_HEADING, companyMatches);
        }

        for (String category : watchlist.getCategories()) {
            List<NewsGroup> topForCategory = matchCategory(groups, category, topPerCategory);
            if (!topForCategory.isEmpty()) {
                appendSection(body, category, topForCategory);
            }
        }

        return body.isEmpty()
                ? Optional.empty()
                : Optional.of(new Notification(TITLE, body.toString(), null, null, Instant.now(), List.of()));
    }

    private static List<NewsGroup> matchCompanies(List<NewsGroup> groups, List<String> companies) {
        if (companies == null || companies.isEmpty()) {
            return List.of();
        }
        return groups.stream()
                .filter(group -> group.items().stream()
                        .anyMatch(item -> WatchlistMatcher.matchesAnyCompany(item, companies)))
                .sorted(byMostRecent())
                .toList();
    }

    private static List<NewsGroup> matchCategory(List<NewsGroup> groups, String category, int limit) {
        return groups.stream()
                .filter(group -> group.items().stream()
                        .anyMatch(item -> WatchlistMatcher.matchesCategory(item, category)))
                .sorted(byMostRecent())
                .limit(Math.max(limit, 0))
                .toList();
    }

    private static Comparator<NewsGroup> byMostRecent() {
        return Comparator.comparing(
                (NewsGroup group) -> group.primary().publishedAt(),
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static void appendSection(StringBuilder body, String heading, List<NewsGroup> groups) {
        if (!body.isEmpty()) {
            body.append("\n\n");
        }
        body.append(heading.toUpperCase(Locale.ROOT)).append("\n");
        body.append(groups.stream().map(WatchlistDigestBuilder::describe).collect(Collectors.joining("\n\n")));
    }

    private static String describe(NewsGroup group) {
        NewsItem primary = group.primary();
        return primary.url() == null ? primary.title() : primary.title() + "\n" + primary.url();
    }
}
