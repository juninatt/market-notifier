package se.pbt.mn.dispatch.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.dispatch.config.WatchlistDigestProperties;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.subscription.config.WatchlistStorageProperties;
import se.pbt.mn.subscription.model.Watchlist;
import se.pbt.mn.subscription.persistence.WatchlistStorage;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("WatchlistStartupRunner")
class WatchlistStartupRunnerTest {

    private final WatchlistStorage watchlistStorage = mock(WatchlistStorage.class);
    private final WatchlistStorageProperties storageProperties = new WatchlistStorageProperties();
    private final WatchlistDigestProperties digestProperties = new WatchlistDigestProperties();
    private final NewsSource source = mock(NewsSource.class);
    private final NotificationChannel emailChannel = mock(NotificationChannel.class);

    private WatchlistStartupRunner runner;

    private void init() {
        when(emailChannel.id()).thenReturn("email");
        runner = new WatchlistStartupRunner(
                watchlistStorage, storageProperties, digestProperties,
                new NewsFetcher(List.of(source)), List.of(emailChannel));
    }

    private static Watchlist watchlist(String email, List<String> companies) {
        var watchlist = new Watchlist();
        watchlist.setEmail(email);
        watchlist.setCompanies(companies);
        watchlist.setCategories(List.of());
        return watchlist;
    }

    private static NewsItem item(String title, List<String> tickers) {
        return new NewsItem(
                title, "desc", URI.create("https://example.com/1"), null,
                Instant.now(), "Example", tickers, Map.of(),
                new NewsItem.ProviderRef("test", "1"), null
        );
    }

    @Nested
    @DisplayName("When the digest is disabled")
    class DigestDisabled {

        @Test
        @DisplayName("Does not load the watchlist or fetch any news")
        void run_withDigestDisabled_doesNothing() {
            digestProperties.setEnabled(false);
            init();

            runner.run(mock(ApplicationArguments.class));

            verifyNoInteractions(watchlistStorage, source, emailChannel);
        }
    }

    @Nested
    @DisplayName("When no watchlist is configured")
    class NoWatchlist {

        @Test
        @DisplayName("Does not fetch any news or send anything")
        void run_withNoWatchlistFile_doesNothing() {
            init();
            when(watchlistStorage.loadWatchlist(storageProperties.getPath())).thenReturn(Optional.empty());

            runner.run(mock(ApplicationArguments.class));

            verifyNoInteractions(source, emailChannel);
        }
    }

    @Nested
    @DisplayName("When a watchlist is configured")
    class WatchlistConfigured {

        @Test
        @DisplayName("Sends a digest email when something matches")
        void run_withMatchingNews_sendsDigest() {
            init();
            when(watchlistStorage.loadWatchlist(storageProperties.getPath()))
                    .thenReturn(Optional.of(watchlist("you@example.com", List.of("Tesla"))));
            when(source.fetchLatest()).thenReturn(List.of(item("Tesla rallies", List.of())));

            runner.run(mock(ApplicationArguments.class));

            verify(emailChannel).send(eq("you@example.com"), any(Notification.class));
        }

        @Test
        @DisplayName("Sends nothing when nothing matches")
        void run_withNoMatchingNews_sendsNothing() {
            init();
            when(watchlistStorage.loadWatchlist(storageProperties.getPath()))
                    .thenReturn(Optional.of(watchlist("you@example.com", List.of("Tesla"))));
            when(source.fetchLatest()).thenReturn(List.of(item("Unrelated weather report", List.of())));

            runner.run(mock(ApplicationArguments.class));

            verify(emailChannel, never()).send(any(), any());
        }

        @Test
        @DisplayName("Skips sending when the watchlist has no email address")
        void run_withNoEmailConfigured_sendsNothing() {
            init();
            when(watchlistStorage.loadWatchlist(storageProperties.getPath()))
                    .thenReturn(Optional.of(watchlist(null, List.of("Tesla"))));

            runner.run(mock(ApplicationArguments.class));

            verifyNoInteractions(source, emailChannel);
        }

        @Test
        @DisplayName("Skips sending when no email channel is registered")
        void run_withNoEmailChannelRegistered_sendsNothing() {
            var nonEmailChannel = mock(NotificationChannel.class);
            when(nonEmailChannel.id()).thenReturn("telegram");
            var runnerWithoutEmail = new WatchlistStartupRunner(
                    watchlistStorage, storageProperties, digestProperties,
                    new NewsFetcher(List.of(source)), List.of(nonEmailChannel));
            when(watchlistStorage.loadWatchlist(storageProperties.getPath()))
                    .thenReturn(Optional.of(watchlist("you@example.com", List.of("Tesla"))));

            runnerWithoutEmail.run(mock(ApplicationArguments.class));

            verifyNoInteractions(source);
            verify(nonEmailChannel, never()).send(any(), any());
        }
    }
}
