package se.pbt.newsprovider.finnhub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for setting up a WebClient bean to interact with the Finnhub API.
 * <p>
 * This bean is automatically injected with values from {@link FinnhubApiProperties},
 * which are populated from the application’s configuration.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FinnhubApiProperties.class)
public class FinnhubConfig {

    /**
     * Creates and configures the {@code finnhubClient} bean used for making HTTP requests to Finnhub.
     */
    @Bean("finnhubClient")
    public WebClient finnhubWebClient(FinnhubApiProperties properties) {
        log.debug("Creating WebClient for Finnhub with base URL: {}", properties.getBaseUrl());

        WebClient client = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-Finnhub-Token", properties.getToken())
                .build();

        log.info("Finnhub WebClient bean successfully created");
        return client;
    }
}
