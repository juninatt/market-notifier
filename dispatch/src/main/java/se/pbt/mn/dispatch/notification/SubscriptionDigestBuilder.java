package se.pbt.mn.dispatch.notification;

import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.dispatch.filter.SubscriptionFilterMatcher;
import se.pbt.mn.dispatch.matching.SubscriptionGroupMatcher;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds the one-off {@code digest-now} {@link Notification} for a single subscription:
 * its regular keyword/ticker/language matches (capped to {@code maxItems}, same as the
 * recurring dispatcher), every group matching a followed company (no limit), and the most
 * recently published groups per followed category, capped at {@code topPerCategory}.
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

        List<NewsGroup> filterMatches = SubscriptionGroupMatcher.match(groups, subscription);
        if (!filterMatches.isEmpty()) {
            appendSection(body, MATCHES_HEADING, filterMatches);
        }

        List<NewsGroup> companyMatches = matchCompanies(groups, filter.getCompanies());
        if (!companyMatches.isEmpty()) {
            appendSection(body, COMPANIES_HEADING, companyMatches);
        }

        List<String> categories = filter.getCategories() == null ? List.of() : filter.getCategories();
        for (String category : categories) {
            List<NewsGroup> topForCategory = matchCategory(groups, category, topPerCategory);
            if (!topForCategory.isEmpty()) {
                appendSection(body, category, topForCategory);
            }
        }

        return body.isEmpty()
                ? Optional.empty()
                : Optional.of(new Notification(TITLE, body.toString(), null, null, Instant.now(), List.of()));
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

    private static void appendSection(StringBuilder body, String heading, List<NewsGroup> groups) {
        if (!body.isEmpty()) {
            body.append("\n\n");
        }
        body.append(heading.toUpperCase(Locale.ROOT)).append("\n");
        body.append(groups.stream().map(SubscriptionDigestBuilder::describe).collect(Collectors.joining("\n\n")));
    }

    private static String describe(NewsGroup group) {
        NewsItem primary = group.primary();
        return primary.url() == null ? primary.title() : primary.title() + "\n" + primary.url();
    }
}
