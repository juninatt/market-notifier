package se.pbt.mn.dispatch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Holds configuration for the {@code digest-now} profile's one-off subscription digest.
 * <p>
 * Maps the {@code digest} section in configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "digest")
@Getter
@Setter
public class SubscriptionDigestProperties {

    /**
     * Maximum number of items included per followed category. Company matches, and the
     * regular keyword/ticker matches, are capped elsewhere ({@code maxItems}) or not at all.
     */
    private int topPerCategory = 10;
}
