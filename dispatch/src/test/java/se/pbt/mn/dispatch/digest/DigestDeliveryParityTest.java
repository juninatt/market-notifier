package se.pbt.mn.dispatch.digest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.core.subscription.SchedulePreset;
import se.pbt.mn.dispatch.config.SubscriptionDigestProperties;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.dispatch.scheduler.NewsDispatchScheduler;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A subscription must get the same digest whether it was sent by the recurring scheduler or
 * the {@code digest-now} profile, and whether it arrives through Telegram or email.
 */
@DisplayName("Digest delivery parity")
class DigestDeliveryParityTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final NewsSource source = mock(NewsSource.class);
    private final NotificationChannel telegram = mock(NotificationChannel.class);
    private final NotificationChannel email = mock(NotificationChannel.class);

    private NewsDispatchScheduler scheduler;
    private SubscriptionDigestRunner runner;

    @BeforeEach
    void setUp() {
        when(telegram.id()).thenReturn("telegram");
        when(email.id()).thenReturn("email");

        var fetcher = new NewsFetcher(List.of(source));
        var properties = new SubscriptionDigestProperties();
        properties.setTopPerCategory(1);
        var digestSender = new SubscriptionDigestSender(List.of(telegram, email), properties);

        scheduler = new NewsDispatchScheduler(fetcher, digestSender, subscriptionService);
        runner = new SubscriptionDigestRunner(subscriptionService, fetcher, digestSender, mock(ConfigurableApplicationContext.class)) {
            @Override
            void exit(int exitCode) {
                // Keep the test JVM alive.
            }
        };
    }

    private static NewsItem item(String id, String title, String source, List<String> tickers, Instant publishedAt) {
        return new NewsItem(
                title, "desc", URI.create("https://example.com/" + id), null,
                publishedAt, source, tickers, Map.of(),
                new NewsItem.ProviderRef("test", id), null
        );
    }

    private static Subscription subscription() {
        var filter = new SubscriptionFilter();
        filter.setKeywords(List.of("Volvo"));
        filter.setTickers(List.of());
        filter.setCompanies(List.of("TSLA"));
        filter.setCategories(List.of("AI"));

        var subscription = new Subscription();
        subscription.setId("sub-1");
        subscription.setChatId(1L);
        subscription.setEmail("you@example.com");
        subscription.setMaxItems(10);
        subscription.setSchedule(SchedulePreset.MORNING);
        subscription.setEnabled(true);
        subscription.setFilter(filter);
        return subscription;
    }

    /**
     * Captures the notification each channel received and clears the mocks for the next run.
     */
    private List<Notification> captureSent() {
        ArgumentCaptor<Notification> viaTelegram = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<Notification> viaEmail = ArgumentCaptor.forClass(Notification.class);
        verify(telegram).send(eq("1"), viaTelegram.capture());
        verify(email).send(eq("you@example.com"), viaEmail.capture());
        reset(telegram, email);
        when(telegram.id()).thenReturn("telegram");
        when(email.id()).thenReturn("email");
        return List.of(viaTelegram.getValue(), viaEmail.getValue());
    }

    @Test
    @DisplayName("Scheduled and digest-now runs send identical notifications on every channel")
    void scheduledAndDigestNow_sendIdenticalNotificationsOnEveryChannel() {
        Instant now = Instant.now();
        when(source.fetchLatest()).thenReturn(List.of(
                item("1", "Volvo unveils new model", "Reuters", List.of(), now),
                item("2", "Tesla rallies", "Reuters", List.of("TSLA"), now),
                item("3", "Tesla stock jumps", "MarketWatch", List.of("TSLA"), now.plus(1, ChronoUnit.HOURS)),
                item("4", "AI model released", "Example", List.of(), now.minus(1, ChronoUnit.DAYS)),
                item("5", "AI breakthrough announced", "Example", List.of(), now),
                item("6", "Unrelated weather report", "Example", List.of(), now)
        ));
        var sub = subscription();
        when(subscriptionService.findEnabledBySchedule(SchedulePreset.MORNING)).thenReturn(List.of(sub));
        when(subscriptionService.findAllEnabled()).thenReturn(List.of(sub));

        scheduler.dispatch(SchedulePreset.MORNING);
        List<Notification> scheduled = captureSent();

        runner.run(mock(ApplicationArguments.class));
        List<Notification> digestNow = captureSent();

        Notification expected = scheduled.get(0);
        assertEquals(expected, scheduled.get(1));
        assertEquals(expected, digestNow.get(0));
        assertEquals(expected, digestNow.get(1));

        String body = expected.body();
        assertTrue(body.contains("MATCHES\nVolvo unveils new model"));
        assertTrue(body.contains("COMPANIES\nTesla rallies"));
        assertTrue(body.contains("AI\nAI breakthrough announced"));
        assertFalse(body.contains("AI model released"));
        assertFalse(body.contains("Unrelated weather report"));
    }
}
