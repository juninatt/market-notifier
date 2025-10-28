package se.pbt.tvm.notifier.telegram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Holds configuration for Telegram's file-based subscription storage.
 * <p>
 * Maps the {@code telegram.storage} section in {@code application-telegram.yml},
 * providing the file path used by {@code SubscriptionStorage} to load and save
 * Telegram-specific user subscriptions.
 */
@Configuration
@ConfigurationProperties(prefix = "telegram.storage")
@Getter
@Setter
public class TelegramStorageProperties {
    private String subscriptions;
}

