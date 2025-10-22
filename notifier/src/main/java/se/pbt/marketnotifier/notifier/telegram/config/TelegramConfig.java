package se.pbt.marketnotifier.notifier.telegram.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.pbt.marketnotifier.notifier.telegram.client.TelegramApiClient;

/**
 * Configures Telegram notification support.
 * <p>
 * Exposes beans to format notifications for MarkdownV2 and deliver them via the Telegram Bot API.
 */
@Configuration
public class TelegramConfig {

    /**
     * Creates a configured Telegram API client using properties from {@link TelegramBotProperties}.
     */
    @Bean
    TelegramApiClient telegramApiClient(TelegramBotProperties properties) {
        return new TelegramApiClient(properties.getBaseUrl(), properties.getBotToken());
    }



}
