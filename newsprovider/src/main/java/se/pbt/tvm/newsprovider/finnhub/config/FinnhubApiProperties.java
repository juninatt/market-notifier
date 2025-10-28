package se.pbt.tvm.newsprovider.finnhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Finnhub WebClient.
 * <p>
 * Values are loaded from the application's configuration (module: {@code app-runner})
 * using the prefix {@code finnhub.api}.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "finnhub.api")
public class FinnhubApiProperties {

    /**
     * Base URL for the Finnhub API.
     */
    private String baseUrl;

    /**
     * API token for authenticating with Finnhub.
     */
    private String token;
}
