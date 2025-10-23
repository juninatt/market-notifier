package se.pbt.marketnotifier.notifier.model;

/**
 * Represents a notification message used as the input model
 * for sending notifications through different channels.
 */
public record Notification(
        String title,
        String body,
        String url
) {}

