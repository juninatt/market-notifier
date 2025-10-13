package se.pbt.ddplus.notifier.telegram.mapper;

import org.springframework.stereotype.Component;
import se.pbt.ddplus.core.subscription.SchedulePreset;
import se.pbt.ddplus.core.subscription.TelegramSubscribeCommand;
import se.pbt.ddplus.subscription.model.Subscription;
import se.pbt.ddplus.subscription.model.SubscriptionFilter;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Converts a {@link TelegramSubscribeCommand} into a {@link Subscription} domain object.
 */
@Component
public class TelegramSubscriptionMapper {

    private static final TimeZone DEFAULT_TZ = TimeZone.getTimeZone("Europe/Stockholm");

    /**
     * Maps a {@link TelegramSubscribeCommand} into a {@link Subscription} domain object.
     * <p>
     * Applies normalization, default schedule, and timezone, and wraps keywords
     * and language into a {@link SubscriptionFilter}.
     */
    public Subscription map(TelegramSubscribeCommand cmd, List<String> normalizedKeywords) {
        Objects.requireNonNull(cmd, "Subscribe command must not be null");

        if (normalizedKeywords == null || normalizedKeywords.isEmpty() ||
                normalizedKeywords.get(0) == null || normalizedKeywords.get(0).trim().isBlank()) {
            throw new IllegalArgumentException("First keyword must be non-blank");
        }

        Subscription sub = new Subscription();
        sub.setChatId(cmd.chatId());
        sub.setSchedule(cmd.schedule() != null ? cmd.schedule() : SchedulePreset.MORNING_EVENING);
        sub.setTimezone(DEFAULT_TZ);
        sub.setMaxItems(cmd.maxItems());
        sub.setEnabled(true);

        SubscriptionFilter filter = new SubscriptionFilter();
        filter.setKeywords(List.copyOf(normalizedKeywords));
        filter.setTickers(List.of());
        filter.setLanguage(normalizeLanguage(cmd.language()));
        sub.setFilter(filter);

        return sub;
    }

    private String normalizeLanguage(String language) {
        if (language == null) return null;
        String trimmed = language.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
