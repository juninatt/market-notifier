package se.pbt.ddplus.notifier.telegram.command;

import se.pbt.ddplus.core.subscription.SubscribeCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses incoming Telegram commands into structured DTOs.
 * Does not perform business logic or external validation.
 */
public class TelegramCommandParser {

    // Matches either quoted strings or single non-whitespace tokens
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]+)\"|(\\S+)");

    /**
     * Parses a /subscribe command into a {@link SubscribeCommand}.
     */
    public SubscribeCommand parseSubscribeCommand(long chatId, String message) {
        List<String> tokens = extractTokens(message.strip());
        validateCommandFormat(tokens);

        String language = extractAndValidateLanguage(tokens);
        int maxItems = extractAndValidateMaxItems(tokens);
        List<String> keywords = extractKeywords(tokens);

        return new SubscribeCommand(chatId, language, maxItems, keywords);
    }

    /**
     * Extracts quoted or unquoted tokens from input.
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
     * Validates command format and token count.
     */
    private void validateCommandFormat(List<String> tokens) {
        if (tokens.size() < 4 || !tokens.get(0).equalsIgnoreCase("/subscribe")) {
            throw new IllegalArgumentException("Usage: /subscribe <keywords> <language> <maxItems>");
        }
    }

    /**
     * Extracts and validates the language token.
     */
    private String extractAndValidateLanguage(List<String> tokens) {
        String lang = tokens.get(tokens.size() - 2);
        if (!lang.matches("^[a-zA-Z]{2}$")) {
            throw new IllegalArgumentException("Language code must be exactly two letters");
        }
        return lang.toLowerCase();
    }

    /**
     * Extracts and validates maxItems token.
     */
    private int extractAndValidateMaxItems(List<String> tokens) {
        try {
            return Integer.parseInt(tokens.get(tokens.size() - 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("maxItems must be an integer");
        }
    }

    /**
     * Extracts keywords from the token list.
     */
    private List<String> extractKeywords(List<String> tokens) {
        List<String> keywords = tokens.subList(1, tokens.size() - 2);
        if (keywords.isEmpty()) {
            throw new IllegalArgumentException("At least one keyword is required");
        }
        return keywords;
    }
}
