package se.pbt.mn.sources.spaceflight.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.sources.spaceflight.mapper.SpaceflightNewsMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NewsSource} implementation backed by the Spaceflight News API.
 * <p>
 * Uses an injected {@link WebClient} configured with base URL. Unlike Finnhub/Marketaux,
 * this API requires no authentication token.
 */
@Slf4j
@Service
public class SpaceflightNewsService implements NewsSource {

    private static final int FETCH_LIMIT = 20;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public SpaceflightNewsService(@Qualifier("spaceflightClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "spaceflightnews";
    }

    /**
     * Fetches and maps the latest articles from the Spaceflight News API.
     * <p>
     * Returns an empty list (rather than throwing) on any fetch or parse failure,
     * so a dead provider doesn't block dispatch for the others.
     */
    @Override
    public List<NewsItem> fetchLatest() {
        try {
            String body = fetchLatestArticles().block();
            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode results = objectMapper.readTree(body).path("results");
            List<NewsItem> items = new ArrayList<>();
            if (results.isArray()) {
                for (JsonNode article : results) {
                    items.add(SpaceflightNewsMapper.map(article));
                }
            }
            return items;
        } catch (Exception e) {
            log.warn("Failed to fetch/parse Spaceflight News articles: {}", e.toString());
            return List.of();
        }
    }

    /**
     * Fetches the latest articles from the Spaceflight News API as a reactive {@link Mono} of raw JSON string.
     *
     * @return Mono containing the response body from the API
     */
    public Mono<String> fetchLatestArticles() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/articles/")
                        .queryParam("limit", FETCH_LIMIT)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
}
