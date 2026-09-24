package se.pbt.mn.dispatch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Holds configuration for the one-off watchlist digest sent when the application starts.
 * <p>
 * Maps the {@code watchlist.digest} section in configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "watchlist.digest")
@Getter
@Setter
public class WatchlistDigestProperties {

    /**
     * Whether the startup digest runs at all. Set to {@code false} to keep a watchlist file
     * around without triggering a send on every application start.
     */
    private boolean enabled = true;

    /**
     * Maximum number of items included per followed category. Company matches are never
     * capped.
     */
    private int topPerCategory = 10;
}
