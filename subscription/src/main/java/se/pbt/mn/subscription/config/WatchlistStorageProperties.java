package se.pbt.mn.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Holds configuration for the file-based watchlist storage.
 * <p>
 * Maps the {@code watchlist.storage} section in configuration, providing the file path
 * used by {@link se.pbt.mn.subscription.persistence.WatchlistStorage} to load the user's
 * watchlist for the startup digest.
 */
@Configuration
@ConfigurationProperties(prefix = "watchlist.storage")
@Getter
@Setter
public class WatchlistStorageProperties {
    private String path = "watchlist.yml";
}
