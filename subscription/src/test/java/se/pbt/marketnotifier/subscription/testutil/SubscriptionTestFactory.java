package se.pbt.marketnotifier.subscription.testutil;

import se.pbt.marketnotifier.core.subscription.SchedulePreset;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.model.SubscriptionFilter;

import java.util.List;

/**
 * Provides reusable factory methods for building test instances
 * of {@link Subscription} and {@link SubscriptionFilter}.
 */
public final class SubscriptionTestFactory {

    private SubscriptionTestFactory() {}

    public static SubscriptionFilter filter(List<String> keywords, List<String> tickers, String language) {
        var filter = new SubscriptionFilter();
        filter.setKeywords(keywords);
        filter.setTickers(tickers);
        filter.setLanguage(language);
        return filter;
    }

    public static SubscriptionFilter defaultFilter() {
        return filter(List.of("news"), List.of("AAPL"), "en");
    }

    public static SubscriptionFilter emptyFilter() {
        return filter(List.of(), List.of(), null);
    }

    public static Subscription subscription(String id, SubscriptionFilter filter, boolean enabled) {
        var sub = new Subscription();
        sub.setId(id);
        sub.setFilter(filter);
        sub.setEnabled(enabled);
        return sub;
    }

    public static Subscription subscription(String id) {
        return subscription(id, defaultFilter(), true);
    }

    public static Subscription subscriptionWithoutId() {
        return subscription(null, defaultFilter(), false);
    }

    public static Subscription subscriptionWithIdOnly(String id) {
        var sub = new Subscription();
        sub.setId(id);
        return sub;
    }

    public static List<Subscription> subscriptionList(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> subscription("sub-" + i))
                .toList();
    }

    public static Subscription subscription(SubscriptionFilter filter) {
        return subscription(null, filter, false);
    }

    /**
     * Creates a Subscription with only keywords and language (used in sanitizer/validator tests).
     */
    public static Subscription subscriptionWithKeywords(List<String> keywords, String language) {
        var filter = filter(keywords, List.of(), language);
        return subscription(null, filter, true);
    }

    /**
     * Creates a Subscription with keywords, language, and a schedule preset (used in sanitizer tests).
     */
    public static Subscription subscriptionWithSchedule(List<String> keywords, String language, SchedulePreset schedule) {
        var sub = subscriptionWithKeywords(keywords, language);
        sub.setSchedule(schedule);
        return sub;
    }
}
