package se.pbt.mn.sources.finnhub.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.sources.finnhub.mapper.FinnhubNewsMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NewsSource} implementation backed by the Finnhub API.
 * <p>
 * Uses an injected {@link WebClient} configured with base URL and token.
 */
@Slf4j
@Service
public class FinnhubNewsService implements NewsSource {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FinnhubNewsService(@Qualifier("finnhubClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "finnhub";
    }

    /**
     * Fetches and maps the latest general news from Finnhub.
     * <p>
     * Returns an empty list (rather than throwing) on any fetch or parse failure,
     * so a dead provider doesn't block dispatch for the others.
     */
    @Override
    public List<NewsItem> fetchLatest() {
        try {
            String body = fetchGeneralNews().block();
            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(body);
            List<NewsItem> items = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode article : root) {
                    items.add(FinnhubNewsMapper.map(article));
                }
            }
            return items;
        } catch (Exception e) {
            log.warn("Failed to fetch/parse Finnhub news: {}", e.toString());
            return List.of();
        }
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
