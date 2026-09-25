package se.pbt.mn.dispatch.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.core.subscription.SchedulePreset;
import se.pbt.mn.dispatch.config.SubscriptionDigestProperties;
import se.pbt.mn.dispatch.digest.SubscriptionDigestSender;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("NewsDispatchScheduler")
class NewsDispatchSchedulerTest {

    private NewsSource sourceA;
    private NewsSource sourceB;
    private NotificationChannel channel;
    private SubscriptionService subscriptionService;
    private NewsDispatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        sourceA = mock(NewsSource.class);
        sourceB = mock(NewsSource.class);
        channel = mock(NotificationChannel.class);
        when(channel.id()).thenReturn("telegram");
        subscriptionService = mock(SubscriptionService.class);

        scheduler = schedulerWith(List.of(channel));
    }

    private NewsDispatchScheduler schedulerWith(List<NotificationChannel> channels) {
        var digestSender = new SubscriptionDigestSender(channels, new SubscriptionDigestProperties());
        return new NewsDispatchScheduler(new NewsFetcher(List.of(sourceA, sourceB)), digestSender, subscriptionService);
    }

    /**
     * The body of the single digest sent through the default Telegram channel.
     */
    private String sentBody() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(channel, times(1)).send(anyString(), captor.capture());
        return captor.getValue().body();
    }

    private static int occurrences(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    private static NewsItem item(String id, String title, List<String> tickers) {
        return item(id, title, "Example", tickers, Instant.EPOCH);
    }

    private static NewsItem item(String id, String title, String source, List<String> tickers, Instant publishedAt) {
        return new NewsItem(
                title, "desc", URI.create("https://example.com/" + id), null,
                publishedAt, source, tickers, Map.of(),
                new NewsItem.ProviderRef("test", id), null
        );
    }

    private static Subscription subscription(long chatId, int maxItems, List<String> keywords) {
        var filter = new SubscriptionFilter();
        filter.setKeywords(keywords);
        filter.setTickers(List.of());
        filter.setLanguage(null);

        var sub = new Subscription();
        sub.setChatId(chatId);
        sub.setMaxItems(maxItems);
        sub.setFilter(filter);
        sub.setEnabled(true);
        sub.setSchedule(SchedulePreset.MORNING);
        return sub;
    }

    @Nested
    @DisplayName("When no subscriptions are due")
    class NoSubscriptionsDue {

        @Test
        @DisplayName("Does not fetch any source or send any notification")
        void dispatch_withNoDueSubscriptions_doesNothing() {
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            verifyNoInteractions(sourceA, sourceB, channel);
        }
    }

    @Nested
    @DisplayName("Fetching and merging sources")
    class FetchingSources {

        @Test
        @DisplayName("Isolates a failing source so the other source's items still get delivered")
        void dispatch_withOneSourceFailing_stillDeliversFromTheOther() {
            var sub = subscription(1L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));

            when(sourceA.fetchLatest()).thenThrow(new RuntimeException("API down"));
            when(sourceB.fetchLatest()).thenReturn(List.of(item("1", "Tesla rallies", List.of())));

            scheduler.dispatch(SchedulePreset.MORNING);

            verify(channel).send(eq("1"), any(Notification.class));
        }

        @Test
        @DisplayName("Dedupes items sharing the same provider reference across sources")
        void dispatch_withDuplicateProviderRefAcrossSources_sendsOnce() {
            var sub = subscription(1L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));

            var duplicate = item("1", "Tesla rallies", List.of());
            when(sourceA.fetchLatest()).thenReturn(List.of(duplicate));
            when(sourceB.fetchLatest()).thenReturn(List.of(duplicate));

            scheduler.dispatch(SchedulePreset.MORNING);

            assertEquals(1, occurrences(sentBody(), "Tesla rallies"));
        }
    }

    @Nested
    @DisplayName("Per-subscription filtering and delivery")
    class FilteringAndDelivery {

        @Test
        @DisplayName("Only delivers items matching the subscription's filter")
        void dispatch_withNonMatchingItem_isNotDelivered() {
            var sub = subscription(1L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(item("1", "Unrelated weather report", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            verifyNoInteractions(channel);
        }

        @Test
        @DisplayName("Truncates matched items to the subscription's maxItems")
        void dispatch_withMoreMatchesThanMaxItems_truncates() {
            var sub = subscription(1L, 1, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "Tesla news one", List.of()),
                    item("2", "Tesla news two", List.of())
            ));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            String body = sentBody();
            assertTrue(body.contains("Tesla news one"));
            assertFalse(body.contains("Tesla news two"));
        }

        @Test
        @DisplayName("Sends all of a subscription's matches as one digest rather than one message each")
        void dispatch_withSeveralMatches_sendsOneDigest() {
            var sub = subscription(1L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "Tesla news one", List.of()),
                    item("2", "Tesla news two", List.of())
            ));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            String body = sentBody();
            assertTrue(body.contains("Tesla news one"));
            assertTrue(body.contains("Tesla news two"));
        }

        @Test
        @DisplayName("Sends a digest to every due subscription")
        void dispatch_withMultipleDueSubscriptions_sendsToEachChatId() {
            var subOne = subscription(1L, 10, List.of("tesla"));
            var subTwo = subscription(2L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(subOne, subTwo));
            when(sourceA.fetchLatest()).thenReturn(List.of(item("1", "Tesla rallies", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            verify(channel).send(eq("1"), any(Notification.class));
            verify(channel).send(eq("2"), any(Notification.class));
        }

        @Test
        @DisplayName("Does not send arbitrary items to a subscription with only companies/categories")
        void dispatch_withNoKeywordsOrTickers_sendsNothing() {
            var sub = subscription(1L, 10, List.of());
            sub.getFilter().setCompanies(List.of("Tesla"));
            sub.getFilter().setCategories(List.of("AI"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(item("1", "Unrelated weather report", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            verifyNoInteractions(channel);
        }
    }

    @Nested
    @DisplayName("Companies and categories on a scheduled subscription")
    class CompaniesAndCategories {

        @Test
        @DisplayName("Delivers company matches to a subscription with no keywords or tickers")
        void dispatch_withCompanyOnlySubscription_deliversCompanyMatches() {
            var sub = subscription(1L, 10, List.of());
            sub.getFilter().setCompanies(List.of("Tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "Tesla rallies", List.of()),
                    item("2", "Unrelated weather report", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            String body = sentBody();
            assertTrue(body.contains("COMPANIES"));
            assertTrue(body.contains("Tesla rallies"));
            assertFalse(body.contains("Unrelated weather report"));
        }

        @Test
        @DisplayName("Delivers category matches to a subscription with no keywords or tickers")
        void dispatch_withCategoryOnlySubscription_deliversCategoryMatches() {
            var sub = subscription(1L, 10, List.of());
            sub.getFilter().setCategories(List.of("AI"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "AI model released", List.of()),
                    item("2", "Unrelated weather report", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            String body = sentBody();
            assertTrue(body.contains("AI\n"));
            assertTrue(body.contains("AI model released"));
            assertFalse(body.contains("Unrelated weather report"));
        }
    }

    @Nested
    @DisplayName("Grouping same-event items before delivery")
    class Grouping {

        @Test
        @DisplayName("Lists two same-ticker items published close together once, with both sources")
        void dispatch_withSameEventFromTwoSources_listsOneGroupWithBothSources() {
            var sub = subscription(1L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));

            Instant now = Instant.now();
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "Tesla rallies", "Reuters", List.of("TSLA"), now)));
            when(sourceB.fetchLatest()).thenReturn(List.of(
                    item("2", "Tesla stock jumps", "MarketWatch", List.of("TSLA"), now.plus(1, ChronoUnit.HOURS))));

            scheduler.dispatch(SchedulePreset.MORNING);

            String body = sentBody();
            assertTrue(body.contains("Reuters"));
            assertTrue(body.contains("MarketWatch"));
            assertFalse(body.contains("Tesla stock jumps"));
        }

        @Test
        @DisplayName("Lists items with no overlapping tickers as separate entries")
        void dispatch_withUnrelatedItems_listsSeparateEntries() {
            var sub = subscription(1L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));

            Instant now = Instant.now();
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "Tesla rallies", "Reuters", List.of("TSLA"), now)));
            when(sourceB.fetchLatest()).thenReturn(List.of(
                    item("2", "Tesla plant opens", "MarketWatch", List.of("AAPL"), now)));

            scheduler.dispatch(SchedulePreset.MORNING);

            String body = sentBody();
            assertTrue(body.contains("Tesla rallies"));
            assertTrue(body.contains("Tesla plant opens"));
        }

        @Test
        @DisplayName("maxItems limits the number of groups, not raw articles")
        void dispatch_withMaxItemsOne_limitsToOneGroupEvenWithTwoArticlesInIt() {
            var sub = subscription(1L, 1, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));

            Instant now = Instant.now();
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "Tesla rallies", "Reuters", List.of("TSLA"), now),
                    item("2", "Tesla stock jumps", "MarketWatch", List.of("TSLA"), now.plus(1, ChronoUnit.HOURS))));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            scheduler.dispatch(SchedulePreset.MORNING);

            String body = sentBody();
            assertTrue(body.contains("Reuters"));
            assertTrue(body.contains("MarketWatch"));
        }
    }

    @Nested
    @DisplayName("Resolving the recipient per channel")
    class RecipientResolution {

        @Test
        @DisplayName("Delivers to both Telegram and email when the subscription has both configured")
        void dispatch_withTelegramAndEmailChannels_deliversToBoth() {
            NotificationChannel emailChannel = mock(NotificationChannel.class);
            when(emailChannel.id()).thenReturn("email");
            var multiChannelScheduler = schedulerWith(List.of(channel, emailChannel));

            var sub = subscription(1L, 10, List.of("tesla"));
            sub.setEmail("user@example.com");
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(item("1", "Tesla rallies", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            multiChannelScheduler.dispatch(SchedulePreset.MORNING);

            verify(channel).send(eq("1"), any(Notification.class));
            verify(emailChannel).send(eq("user@example.com"), any(Notification.class));
        }

        @Test
        @DisplayName("Sends the exact same notification to Telegram and email")
        void dispatch_withTelegramAndEmailChannels_sendsSameNotificationToBoth() {
            NotificationChannel emailChannel = mock(NotificationChannel.class);
            when(emailChannel.id()).thenReturn("email");
            var multiChannelScheduler = schedulerWith(List.of(channel, emailChannel));

            var sub = subscription(1L, 10, List.of("tesla"));
            sub.setEmail("user@example.com");
            sub.getFilter().setCompanies(List.of("Volvo"));
            sub.getFilter().setCategories(List.of("AI"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(
                    item("1", "Tesla rallies", List.of()),
                    item("2", "Volvo unveils new model", List.of()),
                    item("3", "AI model released", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            multiChannelScheduler.dispatch(SchedulePreset.MORNING);

            ArgumentCaptor<Notification> telegram = ArgumentCaptor.forClass(Notification.class);
            ArgumentCaptor<Notification> email = ArgumentCaptor.forClass(Notification.class);
            verify(channel).send(eq("1"), telegram.capture());
            verify(emailChannel).send(eq("user@example.com"), email.capture());
            assertSame(telegram.getValue(), email.getValue());
        }

        @Test
        @DisplayName("Skips the email channel entirely when the subscription has no email set")
        void dispatch_withoutEmailConfigured_skipsEmailChannel() {
            NotificationChannel emailChannel = mock(NotificationChannel.class);
            when(emailChannel.id()).thenReturn("email");
            var multiChannelScheduler = schedulerWith(List.of(channel, emailChannel));

            var sub = subscription(1L, 10, List.of("tesla"));
            when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
            when(sourceA.fetchLatest()).thenReturn(List.of(item("1", "Tesla rallies", List.of())));
            when(sourceB.fetchLatest()).thenReturn(List.of());

            multiChannelScheduler.dispatch(SchedulePreset.MORNING);

            verify(channel).send(eq("1"), any(Notification.class));
            verify(emailChannel, never()).send(anyString(), any(Notification.class));
        }
    }
}
