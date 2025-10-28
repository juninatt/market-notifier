package se.pbt.tvm.notifier.telegram.format;

import java.util.regex.Pattern;

/**
 * Formats outgoing text messages for Telegram.
 * <p>
 * Handles MarkdownV2 escaping and JSON-safe quoting
 * so messages can be sent without syntax or parsing errors.
 */
public final class TelegramOutputFormatter {

    /**
     * Matches all characters that must be escaped for Telegram MarkdownV2.
     * See: https://core.telegram.org/bots/api#markdownv2-style
     */
    private static final Pattern MDV2_SPECIALS =
            // There is no redundant character escape! Changing the regex breaks Telegram communication!
            Pattern.compile("([_\\*\\[\\]\\(\\)~`>#+\\-=|\\{\\}\\.\\!])");

    private TelegramOutputFormatter() {}

    /**
     * Escapes all special MarkdownV2 characters in the given text.
     * <p>
     * This ensures that Telegram displays the text literally, without misinterpreting
     * it as Markdown formatting.
     */
    public static String escapeMarkdown(String text) {
        if (text == null || text.isEmpty()) return "";
        return MDV2_SPECIALS.matcher(text).replaceAll("\\\\$1");
    }

    /**
     * Converts the given text into a JSON-safe string literal.
     * <p>
     * Escapes double quotes and backslashes to ensure the payload is valid JSON.
     */
    public static String json(String text) {
        if (text == null) return "\"\"";
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                + "\"";
    }
}
