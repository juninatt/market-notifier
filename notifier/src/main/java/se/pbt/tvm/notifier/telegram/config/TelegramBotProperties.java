package se.pbt.tvm.notifier.telegram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Holds configuration values for the Telegram notifier.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {
    private boolean enabled = true;
    private String botToken;
    private List<Long> chatIds = List.of();
    private String baseUrl;
    private int initialOffset;
    private int longPollTimeoutSeconds;
}

