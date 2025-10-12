package se.pbt.ddplus.notifier.telegram.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.pbt.ddplus.notifier.telegram.client.TelegramApiClient;

import java.util.regex.Pattern;

/**
 * Configures Telegram notification support.
 * <p>
 * Exposes beans to format notifications for MarkdownV2 and deliver them via the Telegram Bot API.
 */
@Configuration
public class TelegramConfig {

    /**
     * Regex pattern matching all Telegram MarkdownV2 special characters.
     * */
    private static final Pattern MDV2_SPECIALS = Pattern.compile(
            "([_*\\[\\]()~`>#+\\-=|{}.!\\\\])"
    );

    /**
     * Escapes special characters for Telegram's MarkdownV2 parse mode
     * so they are displayed literally instead of being interpreted as formatting.
     */
    // TODO: Extract to util/common package
    static String escapeMarkdownV2(String text) {
        if (text == null || text.isEmpty()) return "";
        return MDV2_SPECIALS.matcher(text).replaceAll("\\\\$1");
    }


    /**
     * Creates a configured Telegram API client using properties from {@link TelegramBotProperties}.
     */
    @Bean
    TelegramApiClient telegramApiClient(TelegramBotProperties properties) {
        return new TelegramApiClient(properties.getBaseUrl(), properties.getBotToken());
    }



}
