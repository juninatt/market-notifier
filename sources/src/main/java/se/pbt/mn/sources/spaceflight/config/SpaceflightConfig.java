package se.pbt.mn.sources.spaceflight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for setting up a WebClient bean to interact with the Spaceflight News API.
 * <p>
 * This bean is automatically injected with values from {@link SpaceflightApiProperties},
 * which are populated from the application’s configuration. Unlike Finnhub/Marketaux, this
 * API requires no authentication token.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SpaceflightApiProperties.class)
public class SpaceflightConfig {

    /**
     * Creates and configures the {@code spaceflightClient} bean used for making HTTP requests
     * to the Spaceflight News API.
     */
    @Bean("spaceflightClient")
    public WebClient spaceflightWebClient(SpaceflightApiProperties properties) {
        log.debug("Creating WebClient for Spaceflight News with base URL: {}", properties.getBaseUrl());

        WebClient client = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();

        log.info("Spaceflight News WebClient bean successfully created");
        return client;
    }
}
