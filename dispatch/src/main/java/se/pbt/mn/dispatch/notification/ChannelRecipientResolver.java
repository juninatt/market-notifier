package se.pbt.mn.dispatch.notification;

import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.subscription.model.Subscription;

/**
 * Resolves the address a given {@link NotificationChannel} should deliver to for a
 * subscription, or {@code null} if the subscriber hasn't configured that channel (e.g. no
 * email set). Shared by every dispatch path that delivers per-subscription notifications.
 */
public final class ChannelRecipientResolver {

    private ChannelRecipientResolver() {}

    public static String resolve(NotificationChannel channel, Subscription subscription) {
        return switch (channel.id()) {
            case "telegram" -> subscription.getChatId() > 0 ? String.valueOf(subscription.getChatId()) : null;
            case "email" -> subscription.getEmail();
            default -> null;
        };
    }
}
