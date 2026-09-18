package se.pbt.mn.sources.finnhub.service;

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

@DisplayName("FinnhubNewsService")
class FinnhubNewsServiceTest {

    private FinnhubNewsService spyService;

    @BeforeEach
    void setUp() {
        WebClient webClient = mock(WebClient.class);
        FinnhubNewsService service = new FinnhubNewsService(webClient, new ObjectMapper());
        spyService = spy(service);
    }

    @Test
    @DisplayName("id() returns 'finnhub'")
    void id_returnsFinnhub() {
        assertEquals("finnhub", spyService.id());
    }

    @Nested
    @DisplayName("fetchLatest")
    class FetchLatest {

        @Test
        @DisplayName("Maps every article in a top-level array response")
        void fetchLatest_withArrayResponse_mapsEachArticle() throws Exception {
            String json = readFixture("test-data/finnhub_general_news_ok.json");
            doReturn(Mono.just(json)).when(spyService).fetchGeneralNews();

            List<NewsItem> items = spyService.fetchLatest();

            assertEquals(2, items.size());
            assertEquals("finnhub", items.get(0).providerRef().provider());
            assertEquals("7504927", items.get(0).providerRef().id());
            assertEquals("7504928", items.get(1).providerRef().id());
        }

        @Test
        @DisplayName("Returns empty list when the response body is empty")
        void fetchLatest_withEmptyBody_returnsEmptyList() {
            doReturn(Mono.just("")).when(spyService).fetchGeneralNews();
            assertTrue(spyService.fetchLatest().isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when the fetch call fails")
        void fetchLatest_withFetchFailure_returnsEmptyList() {
            doReturn(Mono.error(new RuntimeException("API down"))).when(spyService).fetchGeneralNews();
            assertTrue(spyService.fetchLatest().isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when the response body is not valid JSON")
        void fetchLatest_withMalformedJson_returnsEmptyList() {
            doReturn(Mono.just("not json")).when(spyService).fetchGeneralNews();
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
