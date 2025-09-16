package se.pbt.ddplus.subscription.factory;

import org.springframework.stereotype.Service;
import se.pbt.ddplus.core.schedule.SchedulePreset;
import se.pbt.ddplus.core.subscription.SubscribeCommand;
import se.pbt.ddplus.subscription.model.Subscription;
import se.pbt.ddplus.subscription.model.SubscriptionFilter;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Builds {@link Subscription} instances from incoming {@link SubscribeCommand}, applying defaults.
 */
@Service
public class SubscriptionFactory {

    // TODO: Enable setting timezone
    private static final TimeZone DEFAULT_TZ = TimeZone.getTimeZone("Europe/Stockholm");

    /**
     * Creates a {@link Subscription} from a {@link SubscribeCommand} and normalized keywords.
     *
     * @throws IllegalArgumentException if normalizedKeywords is null/empty or first keyword is blank
     * @throws NullPointerException if SubscribeCommand is null
     */
    public Subscription from(SubscribeCommand cmd, List<String> normalizedKeywords) {
        Objects.requireNonNull(cmd, "Subscribe command must not be null");

        if (normalizedKeywords == null || normalizedKeywords.isEmpty() ||
                normalizedKeywords.get(0) == null || normalizedKeywords.get(0).trim().isBlank()) {
            throw new IllegalArgumentException("First keyword must be non-blank");
        }

        List<String> safeKeywords = List.copyOf(normalizedKeywords);
        List<String> safeTickers = List.of();

        Subscription sub = new Subscription();
        sub.setChatId(cmd.chatId());
        sub.setSchedule(cmd.schedule() != null ? cmd.schedule() : SchedulePreset.MORNING_EVENING);
        sub.setTimezone(DEFAULT_TZ);
        sub.setMaxItems(cmd.maxItems());
        sub.setEnabled(true);

        SubscriptionFilter filter = new SubscriptionFilter();
        filter.setKeywords(safeKeywords);
        filter.setTickers(safeTickers);
        filter.setLanguage(normalizeLanguage(cmd.language()));
        sub.setFilter(filter);

        return sub;
    }

    /**
     * Normalizes the language value to ensure consistent storage and comparison.
     */
    private String normalizeLanguage(String language) {
        if (language == null) return null;
        String trimmed = language.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}