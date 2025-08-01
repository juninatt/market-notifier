package se.pbt.ddplus.newsprovider.marketaux.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for setting up a WebClient bean to interact with the Marketaux API.
 * <p>
 * This bean is automatically injected with values from {@link MarketauxApiProperties},
 * which are populated from the application’s configuration.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MarketauxApiProperties.class)
public class MarketauxConfig {

    /**
     * Creates and configures the {@code marketauxClient} bean used for making HTTP requests to Marketaux.
     */
    @Bean("marketauxClient")
    public WebClient marketauxWebClient(MarketauxApiProperties properties) {
        log.debug("Creating WebClient for Marketaux with base URL: {}", properties.getBaseUrl());

        WebClient client = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .build();

        log.info("Marketaux WebClient bean successfully created");
        return client;
    }
}

