package se.pbt.mn.dispatch.digest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.dispatch.config.SubscriptionDigestProperties;
import se.pbt.mn.dispatch.notification.ChannelRecipientResolver;
import se.pbt.mn.dispatch.notification.SubscriptionDigestBuilder;
import se.pbt.mn.subscription.model.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * Builds a subscription's digest once and delivers that same {@link Notification} to every
 * channel the subscriber has configured.
 * <p>
 * Shared by the recurring {@code NewsDispatchScheduler} and the {@code digest-now}
 * {@link SubscriptionDigestRunner}, so the content a subscriber receives doesn't depend on
 * which of them triggered the send, or on which channel it arrives through.
 */
@Component
public class SubscriptionDigestSender {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDigestSender.class);

    private final List<NotificationChannel> channels;
    private final SubscriptionDigestProperties properties;

    public SubscriptionDigestSender(List<NotificationChannel> channels, SubscriptionDigestProperties properties) {
        this.channels = channels;
        this.properties = properties;
    }

    public void send(Subscription subscription, List<NewsGroup> groups) {
        Optional<Notification> notification =
                SubscriptionDigestBuilder.build(groups, subscription, properties.getTopPerCategory());
        if (notification.isEmpty()) {
            log.debug("Nothing matched for subscription {}, skipping", subscription.getId());
            return;
        }

        boolean sent = false;
        for (NotificationChannel channel : channels) {
            String recipient = ChannelRecipientResolver.resolve(channel, subscription);
            if (recipient != null) {
                channel.send(recipient, notification.get());
                sent = true;
            }
        }

        if (sent) {
            log.info("Sent digest for subscription {}", subscription.getId());
        } else {
            log.warn("Subscription {} matched but has no configured channel (chatId or email), skipping", subscription.getId());
        }
    }
}
