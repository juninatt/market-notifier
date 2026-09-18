package se.pbt.mn.sources.marketaux.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.pbt.mn.core.news.NewsItem;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MarketauxNewsService")
class MarketauxNewsServiceTest {

    private MarketauxNewsService spyService;

    @BeforeEach
    void setUp() {
        WebClient webClient = mock(WebClient.class);
        MarketauxNewsService service = new MarketauxNewsService(webClient, new ObjectMapper());
        spyService = spy(service);
    }

    @Test
    @DisplayName("id() returns 'marketaux'")
    void id_returnsMarketaux() {
        assertEquals("marketaux", spyService.id());
    }

    @Nested
    @DisplayName("fetchLatest")
    class FetchLatest {

        @Test
        @DisplayName("Maps every article in the 'data' envelope")
        void fetchLatest_withDataEnvelope_mapsEachArticle() throws Exception {
            String json = readFixture("test-data/marketaux_news_ok.json");
            doReturn(Mono.just(json)).when(spyService).fetchLatestNews();

            List<NewsItem> items = spyService.fetchLatest();

            assertEquals(2, items.size());
            assertEquals("Tesla: The End Of The Road For My Bull Case (NASDAQ:TSLA)", items.get(0).title());
            assertEquals(List.of("AAPL"), items.get(1).tickers());
        }

        @Test
        @DisplayName("Returns empty list when the response body is empty")
        void fetchLatest_withEmptyBody_returnsEmptyList() {
            doReturn(Mono.just("")).when(spyService).fetchLatestNews();
            assertTrue(spyService.fetchLatest().isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when the fetch call fails")
        void fetchLatest_withFetchFailure_returnsEmptyList() {
            doReturn(Mono.error(new RuntimeException("API down"))).when(spyService).fetchLatestNews();
            assertTrue(spyService.fetchLatest().isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when the 'data' field is missing")
        void fetchLatest_withoutDataField_returnsEmptyList() {
            doReturn(Mono.just("{\"meta\":{}}")).when(spyService).fetchLatestNews();
            assertTrue(spyService.fetchLatest().isEmpty());
        }
    }

    private String readFixture(String path) throws Exception {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            Objects.requireNonNull(is, "Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
