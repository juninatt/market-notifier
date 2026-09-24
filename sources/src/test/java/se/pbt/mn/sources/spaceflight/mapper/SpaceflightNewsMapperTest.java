package se.pbt.mn.sources.spaceflight.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.mn.core.news.NewsItem;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpaceflightNewsMapper")
class SpaceflightNewsMapperTest {

    ObjectMapper om;
    ObjectNode baseNode;

    String expectedTitle;
    String expectedDescription;
    URI expectedUrl;
    URI expectedImageUrl;
    Instant expectedPublishedAt;
    String expectedSource;
    Map<String, String> expectedExtras;
    String expectedProvider;
    String expectedProviderId;

    @BeforeEach
    void setUp() throws Exception {
        om = new ObjectMapper();
        baseNode = readFixture(om, "test-data/spaceflight_article_ok.json");

        expectedTitle = "Rocket Report: ULA flies into uncertain future; Falcon 9 family hits 700 launches";
        expectedDescription = "I can tell you SpaceX has already gone captive.";
        expectedUrl = URI.create("https://arstechnica.com/space/2026/09/rocket-report-ula-flies-into-uncertain-future/");
        expectedImageUrl = URI.create("https://cdn.arstechnica.net/wp-content/uploads/2026/09/rocket-report.jpg");
        expectedPublishedAt = Instant.parse("2026-09-18T11:00:11Z");
        expectedSource = "Arstechnica";
        expectedExtras = Map.of("spaceflightnews.author", "Eric Berger");
        expectedProvider = "spaceflightnews";
        expectedProviderId = "40000";
    }

    @Test
    @DisplayName("Maps full fixture correctly")
    void maps_test_data_correctly() {
        NewsItem item = SpaceflightNewsMapper.map(baseNode.deepCopy());

        assertEquals(expectedTitle, item.title());
        assertEquals(expectedDescription, item.description());
        assertEquals(expectedUrl, item.url());
        assertEquals(expectedImageUrl, item.imageUrl());
        assertEquals(expectedPublishedAt, item.publishedAt());
        assertEquals(expectedSource, item.source());
        assertTrue(item.tickers().isEmpty());
        assertEquals(expectedExtras, item.extras());
        assertNotNull(item.providerRef());
        assertEquals(expectedProvider, item.providerRef().provider());
        assertEquals(expectedProviderId, item.providerRef().id());
        assertNull(item.language());
    }

    @Test
    @DisplayName("Uses fallback when title is blank")
    void maps_with_blank_title() {
        ObjectNode node = baseNode.deepCopy();
        set(node, "title", "  ");
        NewsItem item = SpaceflightNewsMapper.map(node);
        assertEquals("(no title)", item.title());
    }

    @Test
    @DisplayName("Handles missing optional fields")
    void maps_when_missing_optional_fields() {
        ObjectNode node = baseNode.deepCopy();
        remove(node, "summary");
        remove(node, "image_url");
        remove(node, "authors");

        expectedDescription = null;
        expectedImageUrl = null;
        expectedExtras = Map.of();

        NewsItem item = SpaceflightNewsMapper.map(node);

        assertEquals(expectedTitle, item.title());
        assertNull(item.description());
        assertEquals(expectedUrl, item.url());
        assertNull(item.imageUrl());
        assertEquals(expectedPublishedAt, item.publishedAt());
        assertEquals(expectedSource, item.source());
        assertEquals(expectedExtras, item.extras());
    }

    @Test
    @DisplayName("Handles invalid URL and missing published_at")
    void maps_with_invalid_url_and_missing_published_at() {
        ObjectNode node = baseNode.deepCopy();
        set(node, "url", "::::");
        remove(node, "published_at");

        NewsItem item = SpaceflightNewsMapper.map(node);

        assertNull(item.url());
        assertEquals(Instant.EPOCH, item.publishedAt());
    }

    @Test
    @DisplayName("Produces unmodifiable extras map")
    void extras_is_unmodifiable() {
        NewsItem item = SpaceflightNewsMapper.map(baseNode.deepCopy());
        assertThrows(UnsupportedOperationException.class, () -> item.extras().put("test", "value"));
    }


    public static ObjectNode readFixture(ObjectMapper om, String path) throws Exception {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            Objects.requireNonNull(is, "Resource not found: " + path);
            var n = om.readTree(is);
            assertTrue(n.isObject(), "Test data must be a JSON object");
            return (ObjectNode) n;
        }
    }

    public static void set(ObjectNode node, String field, String value) {
        node.put(field, value);
    }

    public static void remove(ObjectNode node, String field) {
        node.remove(field);
    }
}
