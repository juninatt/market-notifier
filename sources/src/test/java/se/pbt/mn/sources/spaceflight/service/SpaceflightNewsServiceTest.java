package se.pbt.mn.sources.spaceflight.service;

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

@DisplayName("SpaceflightNewsService")
class SpaceflightNewsServiceTest {

    private SpaceflightNewsService spyService;

    @BeforeEach
    void setUp() {
        WebClient webClient = mock(WebClient.class);
        SpaceflightNewsService service = new SpaceflightNewsService(webClient, new ObjectMapper());
        spyService = spy(service);
    }

    @Test
    @DisplayName("id() returns 'spaceflightnews'")
    void id_returnsSpaceflightnews() {
        assertEquals("spaceflightnews", spyService.id());
    }

    @Nested
    @DisplayName("fetchLatest")
    class FetchLatest {

        @Test
        @DisplayName("Maps every article in the 'results' envelope")
        void fetchLatest_withResultsEnvelope_mapsEachArticle() throws Exception {
            String json = readFixture("test-data/spaceflight_news_ok.json");
            doReturn(Mono.just(json)).when(spyService).fetchLatestArticles();

            List<NewsItem> items = spyService.fetchLatest();

            assertEquals(2, items.size());
            assertEquals("spaceflightnews", items.get(0).providerRef().provider());
            assertEquals("40000", items.get(0).providerRef().id());
            assertEquals("39999", items.get(1).providerRef().id());
        }

        @Test
        @DisplayName("Returns empty list when the response body is empty")
        void fetchLatest_withEmptyBody_returnsEmptyList() {
            doReturn(Mono.just("")).when(spyService).fetchLatestArticles();
            assertTrue(spyService.fetchLatest().isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when the fetch call fails")
        void fetchLatest_withFetchFailure_returnsEmptyList() {
            doReturn(Mono.error(new RuntimeException("API down"))).when(spyService).fetchLatestArticles();
            assertTrue(spyService.fetchLatest().isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when the response body is not valid JSON")
        void fetchLatest_withMalformedJson_returnsEmptyList() {
            doReturn(Mono.just("not json")).when(spyService).fetchLatestArticles();
            assertTrue(spyService.fetchLatest().isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when the 'results' field is missing")
        void fetchLatest_withoutResultsField_returnsEmptyList() {
            doReturn(Mono.just("{\"count\":0}")).when(spyService).fetchLatestArticles();
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
