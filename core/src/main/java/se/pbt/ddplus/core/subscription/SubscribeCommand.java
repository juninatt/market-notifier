package se.pbt.ddplus.core.subscription;

import se.pbt.ddplus.core.schedule.SchedulePreset;

import java.util.List;

/**
 * Represents a parsed /subscribe command sent from a Telegram user.
 */
public record SubscribeCommand(
        long chatId,
        String language,
        int maxItems,
        List<String> keywords,
        SchedulePreset schedule
) {}
