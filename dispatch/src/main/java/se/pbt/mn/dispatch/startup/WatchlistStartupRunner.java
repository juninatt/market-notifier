package se.pbt.mn.dispatch.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.dispatch.config.WatchlistDigestProperties;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.dispatch.grouping.NewsGrouper;
import se.pbt.mn.dispatch.notification.WatchlistDigestBuilder;
import se.pbt.mn.subscription.config.WatchlistStorageProperties;
import se.pbt.mn.subscription.model.Watchlist;
import se.pbt.mn.subscription.persistence.WatchlistStorage;

import java.util.List;
import java.util.Optional;

/**
 * Sends a one-off watchlist digest email as soon as the application starts, so the user
 * gets today's news right away instead of waiting for the next scheduled
 * {@link se.pbt.mn.core.subscription.SchedulePreset}.
 * <p>
 * Reads the watchlist via {@link WatchlistStorage}; does nothing if the digest is disabled,
 * no watchlist file exists, or nothing in it matches any currently available news.
 */
@Component
public class WatchlistStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WatchlistStartupRunner.class);
    private static final String EMAIL_CHANNEL_ID = "email";

    private final WatchlistStorage watchlistStorage;
    private final WatchlistStorageProperties storageProperties;
    private final WatchlistDigestProperties digestProperties;
    private final NewsFetcher newsFetcher;
    private final List<NotificationChannel> channels;

    public WatchlistStartupRunner(
            WatchlistStorage watchlistStorage,
            WatchlistStorageProperties storageProperties,
            WatchlistDigestProperties digestProperties,
            NewsFetcher newsFetcher,
            List<NotificationChannel> channels
    ) {
        this.watchlistStorage = watchlistStorage;
        this.storageProperties = storageProperties;
        this.digestProperties = digestProperties;
        this.newsFetcher = newsFetcher;
        this.channels = channels;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!digestProperties.isEnabled()) {
            log.debug("Watchlist digest disabled, skipping startup run");
            return;
        }

        Optional<Watchlist> watchlist = watchlistStorage.loadWatchlist(storageProperties.getPath());
        if (watchlist.isEmpty()) {
            log.debug("No watchlist configured at '{}', skipping startup digest", storageProperties.getPath());
            return;
        }

        try {
            send(watchlist.get());
        } catch (Exception e) {
            log.warn("Failed to send watchlist startup digest: {}", e.toString());
        }
    }

    private void send(Watchlist watchlist) {
        String email = watchlist.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Watchlist has no email address configured, skipping startup digest");
            return;
        }

        NotificationChannel emailChannel = channels.stream()
                .filter(channel -> EMAIL_CHANNEL_ID.equals(channel.id()))
                .findFirst()
                .orElse(null);
        if (emailChannel == null) {
            log.warn("No email channel registered, cannot send watchlist startup digest to {}", email);
            return;
        }

        List<NewsGroup> allGroups = NewsGrouper.group(newsFetcher.fetchAll());
        if (allGroups.isEmpty()) {
            log.debug("No news fetched, skipping watchlist startup digest");
            return;
        }

        WatchlistDigestBuilder.build(allGroups, watchlist, digestProperties.getTopPerCategory())
                .ifPresentOrElse(
                        notification -> {
                            emailChannel.send(email, notification);
                            log.info("Sent watchlist startup digest to {}", email);
                        },
                        () -> log.debug("Nothing in the watchlist matched current news, skipping startup digest")
                );
    }
}
