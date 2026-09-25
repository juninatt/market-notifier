package se.pbt.mn.dispatch.digest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.dispatch.config.SubscriptionDigestProperties;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SubscriptionDigestSender")
class SubscriptionDigestSenderTest {

    private final NotificationChannel telegram = mock(NotificationChannel.class);
    private final NotificationChannel email = mock(NotificationChannel.class);
    private SubscriptionDigestSender sender;

    @BeforeEach
    void setUp() {
        when(telegram.id()).thenReturn("telegram");
        when(email.id()).thenReturn("email");
        sender = new SubscriptionDigestSender(List.of(telegram, email), new SubscriptionDigestProperties());
    }

    private static List<NewsGroup> groups(String... titles) {
        return Arrays.stream(titles)
                .map(title -> new NewsGroup(List.of(new NewsItem(
                        title, "desc", URI.create("https://example.com/" + title.hashCode()), null,
                        Instant.EPOCH, "Example", List.of(), Map.of(),
                        new NewsItem.ProviderRef("test", title), null))))
                .toList();
    }

    private static Subscription subscription(long chatId, String emailAddress) {
        var filter = new SubscriptionFilter();
        filter.setKeywords(List.of("Tesla"));

        var subscription = new Subscription();
        subscription.setId("sub-1");
        subscription.setChatId(chatId);
        subscription.setEmail(emailAddress);
        subscription.setMaxItems(10);
        subscription.setFilter(filter);
        return subscription;
    }

    @Test
    @DisplayName("Sends the same notification instance to every configured channel")
    void send_withBothChannelsConfigured_sendsSameNotificationToBoth() {
        sender.send(subscription(1L, "you@example.com"), groups("Tesla rallies"));

        ArgumentCaptor<Notification> viaTelegram = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<Notification> viaEmail = ArgumentCaptor.forClass(Notification.class);
        verify(telegram).send(eq("1"), viaTelegram.capture());
        verify(email).send(eq("you@example.com"), viaEmail.capture());
        assertSame(viaTelegram.getValue(), viaEmail.getValue());
    }

    @Test
    @DisplayName("Skips a channel the subscriber hasn't configured")
    void send_withOnlyEmailConfigured_skipsTelegram() {
        sender.send(subscription(0L, "you@example.com"), groups("Tesla rallies"));

        verify(telegram, never()).send(anyString(), any());
        verify(email).send(eq("you@example.com"), any(Notification.class));
    }

    @Test
    @DisplayName("Sends nothing when nothing matches")
    void send_withNoMatches_sendsNothing() {
        sender.send(subscription(1L, "you@example.com"), groups("Unrelated weather report"));

        verify(telegram, never()).send(any(), any());
        verify(email, never()).send(any(), any());
    }
}
