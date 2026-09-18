package se.pbt.mn.sources.marketaux.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.sources.marketaux.mapper.MarketauxNewsMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NewsSource} implementation backed by the Marketaux API.
 * <p>
 * Uses an injected {@link WebClient} configured with base URL and token.
 */
@Slf4j
@Service
public class MarketauxNewsService implements NewsSource {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public MarketauxNewsService(@Qualifier("marketauxClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "marketaux";
    }

    /**
     * Fetches and maps the latest news from Marketaux.
     * <p>
     * Returns an empty list (rather than throwing) on any fetch or parse failure,
     * so a dead provider doesn't block dispatch for the others.
     */
    @Override
    public List<NewsItem> fetchLatest() {
        try {
            String body = fetchLatestNews().block();
            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode data = objectMapper.readTree(body).path("data");
            List<NewsItem> items = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode article : data) {
                    items.add(MarketauxNewsMapper.map(article));
                }
            }
            return items;
        } catch (Exception e) {
            log.warn("Failed to fetch/parse Marketaux news: {}", e.toString());
            return List.of();
        }
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
