package se.pbt.mn.dispatch.notification;

import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.dispatch.filter.SubscriptionFilterMatcher;
import se.pbt.mn.dispatch.matching.SubscriptionGroupMatcher;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the digest {@link Notification} for a single subscription: its regular
 * keyword/ticker/language matches (capped to {@code maxItems}), every group matching a
 * followed company (no limit), and the most recently published groups per followed
 * category, capped at {@code topPerCategory}.
 * <p>
 * Every delivery path -- the recurring scheduler and the {@code digest-now} profile --
 * sends exactly this notification, so a subscriber gets the same content regardless of how
 * the send was triggered or which channel it arrives on. A group is listed once, in the
 * first section it matches, even if it would also match a later one.
 * <p>
 * Returns an empty {@link Optional} when nothing matches, so the caller never sends a
 * blank digest.
 */
public final class SubscriptionDigestBuilder {

    private static final String TITLE = "Your subscription digest";
    private static final String MATCHES_HEADING = "Matches";
    private static final String COMPANIES_HEADING = "Companies";

    private SubscriptionDigestBuilder() {}

    public static Optional<Notification> build(List<NewsGroup> groups, Subscription subscription, int topPerCategory) {
        SubscriptionFilter filter = subscription.getFilter();
        StringBuilder body = new StringBuilder();
        Set<NewsGroup> listed = new HashSet<>();

        List<NewsGroup> filterMatches = SubscriptionGroupMatcher.match(groups, subscription);
        appendSection(body, MATCHES_HEADING, filterMatches, listed);

        List<NewsGroup> companyMatches = matchCompanies(unlisted(groups, listed), filter.getCompanies());
        appendSection(body, COMPANIES_HEADING, companyMatches, listed);

        List<String> categories = filter.getCategories() == null ? List.of() : filter.getCategories();
        for (String category : categories) {
            List<NewsGroup> topForCategory = matchCategory(unlisted(groups, listed), category, topPerCategory);
            appendSection(body, category, topForCategory, listed);
        }

        return body.isEmpty()
                ? Optional.empty()
                : Optional.of(new Notification(TITLE, body.toString(), null, null, null, List.of()));
    }

    private static List<NewsGroup> unlisted(List<NewsGroup> groups, Set<NewsGroup> listed) {
        return groups.stream().filter(group -> !listed.contains(group)).toList();
    }

    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    private static List<NewsGroup> matchCompanies(List<NewsGroup> groups, List<String> companies) {
        if (isEmpty(companies)) {
            return List.of();
        }
        return groups.stream()
                .filter(group -> group.items().stream()
                        .anyMatch(item -> SubscriptionFilterMatcher.matchesAnyCompany(item, companies)))
                .sorted(byMostRecent())
                .toList();
    }

    private static List<NewsGroup> matchCategory(List<NewsGroup> groups, String category, int limit) {
        return groups.stream()
                .filter(group -> group.items().stream()
                        .anyMatch(item -> SubscriptionFilterMatcher.matchesCategory(item, category)))
                .sorted(byMostRecent())
                .limit(Math.max(limit, 0))
                .toList();
    }

    private static Comparator<NewsGroup> byMostRecent() {
        return Comparator.comparing(
                (NewsGroup group) -> group.primary().publishedAt(),
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static void appendSection(StringBuilder body, String heading, List<NewsGroup> groups, Set<NewsGroup> listed) {
        if (groups.isEmpty()) {
            return;
        }
        if (!body.isEmpty()) {
            body.append("\n\n");
        }
        body.append(heading.toUpperCase(Locale.ROOT)).append("\n");
        body.append(groups.stream().map(SubscriptionDigestBuilder::describe).collect(Collectors.joining("\n\n")));
        listed.addAll(groups);
    }

    /**
     * The primary item's title and link, plus every distinct source in the group so a
     * subscriber can see a story is corroborated by more than one outlet.
     */
    private static String describe(NewsGroup group) {
        NewsItem primary = group.primary();
        StringBuilder sb = new StringBuilder(primary.title());
        if (primary.url() != null) {
            sb.append("\n").append(primary.url());
        }

        String sources = group.items().stream()
                .map(NewsItem::source)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));
        if (!sources.isBlank()) {
            sb.append("\nSources: ").append(sources);
        }
        return sb.toString();
    }
}
