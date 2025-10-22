package se.pbt.marketnotifier.newsprovider.marketaux.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Marketaux WebClient.
 * <p>
 * Values are loaded from the application's configuration (module: {@code app-runner})
 * using the prefix {@code marketaux.api}.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "marketaux.api")
public class MarketauxApiProperties {

    /**
     * Base URL for the Marketaux API.
     */
    private String baseUrl;

    /**
     * API token for authenticating with Marketaux.
     */
    private String token;
}
