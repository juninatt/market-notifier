package se.pbt.mn.sources.spaceflight.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Spaceflight News WebClient.
 * <p>
 * Values are loaded from the application's configuration (module: {@code app-runner})
 * using the prefix {@code spaceflight.api}.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "spaceflight.api")
public class SpaceflightApiProperties {

    /**
     * Base URL for the Spaceflight News API.
     */
    private String baseUrl;
}
