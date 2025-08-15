package se.pbt.ddplus.notifier.core;

/**
 * Port interface for sending a {@link Notification} through a specific channel.
 * Implementations handle the delivery details for that channel.
 */
public interface NotifierPort {
    void send(Notification notification);
}

