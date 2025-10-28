package se.pbt.tvm.newsprovider.marketaux.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for retrieving news articles from the Marketaux API.
 * <p>
 * Uses an injected {@link WebClient} configured with base URL and token.
 */
@Service
public class MarketauxNewsService {

    @Qualifier("marketauxClient")
    private final WebClient webClient;

    public MarketauxNewsService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches the latest U.S. news from Marketaux as a reactive {@link Mono} of raw JSON string.
     *
     * @return Mono containing the response body from the API
     */
    public Mono<String> fetchLatestNews() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/news/all")
                        .queryParam("countries", "us")
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
}
