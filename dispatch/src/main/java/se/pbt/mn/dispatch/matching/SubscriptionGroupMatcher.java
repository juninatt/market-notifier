package se.pbt.mn.dispatch.matching;

import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.dispatch.filter.SubscriptionFilterMatcher;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.util.List;

/**
 * Selects the {@link NewsGroup}s that match a subscription's filter, truncated to its
 * {@code maxItems}. A group matches if any item in it does -- the whole group travels
 * together so a subscriber sees every outlet covering a story they're subscribed to.
 * <p>
 * A filter with neither keywords nor tickers matches nothing here, even though
 * {@link SubscriptionFilterMatcher#matches} would accept every item -- such a subscription
 * only exists for its companies/categories, and shouldn't be sent arbitrary news.
 */
public final class SubscriptionGroupMatcher {

    private SubscriptionGroupMatcher() {}

    public static List<NewsGroup> match(List<NewsGroup> groups, Subscription subscription) {
        SubscriptionFilter filter = subscription.getFilter();
        if (isEmpty(filter.getKeywords()) && isEmpty(filter.getTickers())) {
            return List.of();
        }
        return groups.stream()
                .filter(group -> group.items().stream()
                        .anyMatch(item -> SubscriptionFilterMatcher.matches(item, subscription.getFilter())))
                .limit(Math.max(subscription.getMaxItems(), 0))
                .toList();
    }

    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }
}
