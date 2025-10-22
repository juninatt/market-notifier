package se.pbt.marketnotifier.core.subscription;

import java.util.List;

/**
 * DTO representing a Telegram /subscribe command.
 * <p>
 * This is a transport object created by parsing the raw Telegram message,
 * before mapping it into the domain-level Subscription in the subscription module for storage.
 */
public record TelegramSubscribeCommand(
        long chatId,
        String language,
        int maxItems,
        List<String> keywords,
        SchedulePreset schedule
) {}
