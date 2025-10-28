package se.pbt.tvm.notifier.telegram.format;

import org.springframework.stereotype.Component;
import se.pbt.tvm.core.subscription.SchedulePreset;
import se.pbt.tvm.core.subscription.TelegramSubscribeCommand;
import se.pbt.tvm.notifier.telegram.model.TelegramCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for interpreting raw Telegram input and converting it into structured command objects.
 * <p>
 * The parser provides a standardized way to translate user messages into a format
 * that can be handled consistently by the application’s command logic.
 * It focuses solely on syntactic interpretation and normalization, not business validation.
 */
@Component
public class TelegramInputParser {

    // Matches either quoted strings or single non-whitespace tokens
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]+)\"|(\\S+)");

    private static final Map<String, SchedulePreset> SCHEDULE_ALIASES = Map.ofEntries(
            Map.entry("morning", SchedulePreset.MORNING),
            Map.entry("m", SchedulePreset.MORNING),
            Map.entry("evening", SchedulePreset.EVENING),
            Map.entry("e", SchedulePreset.EVENING),
            Map.entry("morning_evening", SchedulePreset.MORNING_EVENING),
            Map.entry("me", SchedulePreset.MORNING_EVENING),
            Map.entry("morning_lunch_evening", SchedulePreset.MORNING_LUNCH_EVENING),
            Map.entry("mle", SchedulePreset.MORNING_LUNCH_EVENING)
    );

    /**
     * Parses a /subscribe command into a {@link TelegramSubscribeCommand}.
     * Format:
     *   /subscribe <keywords...> <language> [schedule] <maxItems>
     * Where [schedule] is optional and can be one of:
     *   morning|m, evening|e, morning_evening|me, morning_lunch_evening|mle
     */
    public TelegramSubscribeCommand parseSubscribeCommand(TelegramCommand command) {
        List<String> tokens = extractTokens(command.message().strip());
        validateCommandFormat(tokens);

        int maxItems = extractAndValidateMaxItems(tokens.get(tokens.size() - 1));

        String maybeSchedule = tokens.get(tokens.size() - 2);
        SchedulePreset schedule = parseScheduleOrNull(maybeSchedule);

        final String language;
        final List<String> keywords;

        if (schedule != null) {
            language = extractAndValidateLanguage(tokens.get(tokens.size() - 3));
            keywords = extractKeywords(tokens,tokens.size() - 3);
        } else {
            language = extractAndValidateLanguage(maybeSchedule);
            keywords = extractKeywords(tokens,tokens.size() - 2);
        }

        return new TelegramSubscribeCommand(command.chatId(), language, maxItems, keywords, schedule);
    }

    /**
     * Splits the incoming Telegram message into tokens.
     * <p>
     * Supports both quoted and unquoted segments, allowing
     * multi-word keyword phrases such as {@code "AI stocks"}.
     */
    private List<String> extractTokens(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        while (matcher.find()) {
            tokens.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }
        return tokens;
    }

    /**
     * Ensures that the parsed token sequence starts with {@code /subscribe}
     * and contains at least the required arguments.
     */
    private void validateCommandFormat(List<String> tokens) {
        if (tokens.size() < 4 || !tokens.get(0).equalsIgnoreCase("/subscribe")) {
            throw new IllegalArgumentException(
                    "Usage: /subscribe <keywords> <language> [schedule] <maxItems>");
        }
    }

    /**
     * Returns a SchedulePreset if token matches an alias; otherwise null.
     */
    private SchedulePreset parseScheduleOrNull(String token) {
        if (token == null) return null;
        return SCHEDULE_ALIASES.get(token.toLowerCase(Locale.ROOT));
    }

    /**
     *
     * Validates language token.
     */
    private String extractAndValidateLanguage(String langToken) {
        if (langToken == null || !langToken.matches("^[a-zA-Z]{2}$")) {
            throw new IllegalArgumentException("Language code must be exactly two letters");
        }
        return langToken.toLowerCase(Locale.ROOT);
    }

    /**
     * Parses maxItems as integer.
     */
    private int extractAndValidateMaxItems(String maxToken) {
        try {
            return Integer.parseInt(maxToken);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("maxItems must be an integer");
        }
    }

    /**
     * Collects all tokens representing subscription keywords.
     * <p>
     * Everything between the command itself and the language/schedule tokens
     * is considered part of the keyword list.
     */
    private List<String> extractKeywords(List<String> tokens, int toIndexExclusive) {
        if (toIndexExclusive <= 1) {
            throw new IllegalArgumentException("At least one keyword is required");
        }
        return tokens.subList(1, toIndexExclusive);
    }
}
