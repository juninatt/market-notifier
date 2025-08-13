package se.pbt.ddplus.newsprovider.finnhub.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for retrieving general news articles from the Finnhub API.
 * <p>
 * Uses an injected {@link WebClient} configured with base URL and token.
 */
@Service
public class FinnhubNewsService {

    @Qualifier("finnhubClient")
    private final WebClient webClient;

    public FinnhubNewsService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches general news from Finnhub as a reactive {@link Mono} of raw JSON string.
     *
     * @return Mono containing the response body from the API
     */
    public Mono<String> fetchGeneralNews() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/news")
                        .queryParam("category", "general")
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
}
