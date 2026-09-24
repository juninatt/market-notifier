package se.pbt.mn.dispatch.digest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.dispatch.config.SubscriptionDigestProperties;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("SubscriptionDigestRunner")
class SubscriptionDigestRunnerTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final SubscriptionDigestProperties properties = new SubscriptionDigestProperties();
    private final NewsSource source = mock(NewsSource.class);
    private final NotificationChannel emailChannel = mock(NotificationChannel.class);
    private final ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

    private int[] capturedExitCode;
    private TestableRunner runner;

    /**
     * Overrides the JVM-terminating {@code exit} call so the test process survives, and
     * records the exit code it would have used.
     */
    private static class TestableRunner extends SubscriptionDigestRunner {
        private final int[] capturedExitCode;

        TestableRunner(SubscriptionService subscriptionService, NewsFetcher newsFetcher,
                       List<NotificationChannel> channels, SubscriptionDigestProperties properties,
                       ConfigurableApplicationContext context, int[] capturedExitCode) {
            super(subscriptionService, newsFetcher, channels, properties, context);
            this.capturedExitCode = capturedExitCode;
        }

        @Override
        void exit(int exitCode) {
            capturedExitCode[0] = exitCode;
        }
    }

    private void init() {
        when(emailChannel.id()).thenReturn("email");
        capturedExitCode = new int[]{Integer.MIN_VALUE};
        runner = new TestableRunner(
                subscriptionService, new NewsFetcher(List.of(source)), List.of(emailChannel),
                properties, context, capturedExitCode);
    }

    private static Subscription subscription(String id, String email, List<String> companies) {
        var filter = new SubscriptionFilter();
        filter.setKeywords(List.of());
        filter.setTickers(List.of());
        filter.setCompanies(companies);
        filter.setCategories(List.of());

        var subscription = new Subscription();
        subscription.setId(id);
        subscription.setEmail(email);
        subscription.setMaxItems(10);
        subscription.setFilter(filter);
        subscription.setEnabled(true);
        return subscription;
    }

    private static NewsItem item(String title, List<String> tickers) {
        return new NewsItem(
                title, "desc", URI.create("https://example.com/1"), null,
                Instant.now(), "Example", tickers, Map.of(),
                new NewsItem.ProviderRef("test", "1"), null
        );
    }

    @Nested
    @DisplayName("When no subscriptions are enabled")
    class NoSubscriptions {

        @Test
        @DisplayName("Does not fetch any source or send anything, and exits successfully")
        void run_withNoEnabledSubscriptions_doesNothing() {
            init();
            when(subscriptionService.findAllEnabled()).thenReturn(List.of());

            runner.run(mock(ApplicationArguments.class));

            verifyNoInteractions(source, emailChannel);
            assertEquals(0, capturedExitCode[0]);
        }
    }

    @Nested
    @DisplayName("When subscriptions are enabled")
    class SubscriptionsEnabled {

        @Test
        @DisplayName("Sends a digest to each subscription whose filter matches")
        void run_withMatchingNews_sendsDigestToEach() {
            init();
            var sub = subscription("sub-1", "you@example.com", List.of("Tesla"));
            when(subscriptionService.findAllEnabled()).thenReturn(List.of(sub));
            when(source.fetchLatest()).thenReturn(List.of(item("Tesla rallies", List.of())));

            runner.run(mock(ApplicationArguments.class));

            verify(emailChannel).send(eq("you@example.com"), any(Notification.class));
            assertEquals(0, capturedExitCode[0]);
        }

        @Test
        @DisplayName("Skips a subscription whose filter matches nothing")
        void run_withNoMatchingNews_sendsNothing() {
            init();
            var sub = subscription("sub-1", "you@example.com", List.of("Tesla"));
            when(subscriptionService.findAllEnabled()).thenReturn(List.of(sub));
            when(source.fetchLatest()).thenReturn(List.of(item("Unrelated weather report", List.of())));

            runner.run(mock(ApplicationArguments.class));

            verify(emailChannel, never()).send(any(), any());
        }

        @Test
        @DisplayName("Does not send anything when no news could be fetched")
        void run_withNoNewsFetched_doesNothing() {
            init();
            var sub = subscription("sub-1", "you@example.com", List.of("Tesla"));
            when(subscriptionService.findAllEnabled()).thenReturn(List.of(sub));
            when(source.fetchLatest()).thenReturn(List.of());

            runner.run(mock(ApplicationArguments.class));

            verifyNoInteractions(emailChannel);
        }

        @Test
        @DisplayName("Exits with a non-zero code when the run fails")
        void run_whenSendingThrows_exitsWithFailureCode() {
            init();
            var sub = subscription("sub-1", "you@example.com", List.of("Tesla"));
            when(subscriptionService.findAllEnabled()).thenThrow(new RuntimeException("storage unavailable"));

            runner.run(mock(ApplicationArguments.class));

            assertEquals(1, capturedExitCode[0]);
        }
    }
}
