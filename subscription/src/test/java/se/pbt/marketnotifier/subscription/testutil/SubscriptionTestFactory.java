package se.pbt.marketnotifier.subscription.testutil;

import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.model.SubscriptionFilter;

import java.util.List;

/**
 * Provides reusable factory methods for building test instances
 * of {@link Subscription} and {@link SubscriptionFilter}.
 */
public final class SubscriptionTestFactory {

    private SubscriptionTestFactory() {}


    /**
     * Creates a fully populated SubscriptionFilter with the provided parameters.
     */
    public static SubscriptionFilter filter(List<String> keywords, List<String> tickers, String language) {
        var filter = new SubscriptionFilter();
        filter.setKeywords(keywords);
        filter.setTickers(tickers);
        filter.setLanguage(language);
        return filter;
    }

    /**
     * Creates a basic English filter with one keyword and ticker.
     */
    public static SubscriptionFilter defaultFilter() {
        return filter(List.of("news"), List.of("AAPL"), "en");
    }

    /**
     * Creates an empty SubscriptionFilter (no keywords, no tickers, no language).
     */
    public static SubscriptionFilter emptyFilter() {
        return filter(List.of(), List.of(), null);
    }


    /**
     * Creates a Subscription with a given id, filter and enabled flag.
     */
    public static Subscription subscription(String id, SubscriptionFilter filter, boolean enabled) {
        var sub = new Subscription();
        sub.setId(id);
        sub.setFilter(filter);
        sub.setEnabled(enabled);
        return sub;
    }

    /**
     * Creates a Subscription with default filter and provided id.
     */
    public static Subscription subscription(String id) {
        return subscription(id, defaultFilter(), true);
    }

    /**
     * Creates a disabled Subscription without ID (useful for invalid/edge-case testing).
     */
    public static Subscription subscriptionWithoutId() {
        return subscription(null, defaultFilter(), false);
    }

    /**
     * Creates a Subscription with only ID (no filter).
     */
    public static Subscription subscriptionWithIdOnly(String id) {
        var sub = new Subscription();
        sub.setId(id);
        return sub;
    }

    /**
     * Creates a list of N subscriptions with sequential IDs.
     */
    public static List<Subscription> subscriptionList(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> subscription("sub-" + i))
                .toList();
    }

    public static Subscription subscription(SubscriptionFilter filter) {
        return subscription(null, filter, false);
    }

}
