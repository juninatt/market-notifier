package se.pbt.mn.dispatch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.core.subscription.SchedulePreset;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.dispatch.grouping.NewsGrouper;
import se.pbt.mn.dispatch.matching.SubscriptionGroupMatcher;
import se.pbt.mn.dispatch.notification.ChannelRecipientResolver;
import se.pbt.mn.dispatch.notification.NotificationBuilder;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.time.Instant;
import java.util.List;

/**
 * Ties subscription schedules to news fetching, filtering, and delivery.
 * <p>
 * Runs a per-minute tick and checks every {@link SchedulePreset} against
 * {@link FiringMinuteDetector} rather than one {@code @Scheduled} method per preset, so
 * adding a new preset later needs no changes here.
 * <p>
 * Note: each {@link SchedulePreset} fires in its own configured timezone (see
 * {@link SchedulePreset#getZone()}); {@link Subscription#getTimezone()} is not applied on
 * top of that yet -- every subscriber on a given preset fires at the same instant
 * regardless of their own timezone setting.
 * <p>
 * Excluded under the {@code digest-now} profile, which sends every enabled subscription
 * once and exits rather than running an ongoing recurring schedule.
 */
@Component
@Profile("!digest-now")
public class NewsDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsDispatchScheduler.class);

    private final NewsFetcher newsFetcher;
    private final List<NotificationChannel> channels;
    private final SubscriptionService subscriptionService;

    public NewsDispatchScheduler(
            NewsFetcher newsFetcher,
            List<NotificationChannel> channels,
            SubscriptionService subscriptionService
    ) {
        this.newsFetcher = newsFetcher;
        this.channels = channels;
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedRate = 60_000)
    public void tick() {
        Instant now = Instant.now();
        for (SchedulePreset preset : SchedulePreset.values()) {
            if (FiringMinuteDetector.isFiring(preset, now)) {
                dispatch(preset);
            }
        }
    }

    /**
     * Fetches all sources once, groups items that likely cover the same event, and
     * delivers matching groups to every subscription due for the given preset.
     */
    void dispatch(SchedulePreset preset) {
        List<Subscription> due = subscriptionService.findEnabledBySchedule(preset);
        if (due.isEmpty()) {
            return;
        }

        List<NewsItem> allNews = newsFetcher.fetchAll();
        if (allNews.isEmpty()) {
            log.debug("No news fetched for preset={}, skipping {} due subscription(s)", preset, due.size());
            return;
        }

        List<NewsGroup> allGroups = NewsGrouper.group(allNews);

        for (Subscription subscription : due) {
            List<NewsGroup> matched = SubscriptionGroupMatcher.match(allGroups, subscription);

            for (NewsGroup group : matched) {
                Notification notification = NotificationBuilder.fromGroup(group);
                for (NotificationChannel channel : channels) {
                    String recipient = resolveRecipient(channel, subscription);
                    if (recipient != null) {
                        channel.send(recipient, notification);
                    }
                }
            }
        }
    }

    private String resolveRecipient(NotificationChannel channel, Subscription subscription) {
        return ChannelRecipientResolver.resolve(channel, subscription);
    }
}
