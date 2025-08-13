package se.pbt.ddplus.newsprovider.marketaux.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.ddplus.core.news.NewsItem;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MarketauxNewsMapper")
class MarketauxNewsMapperTest {

    ObjectMapper om;
    ObjectNode baseNode;

    String expectedTitle;
    String expectedDescription;
    URI expectedUrl;
    URI expectedImageUrl;
    Instant expectedPublishedAt;
    String expectedSource;
    List<String> expectedTickers;
    Map<String, String> expectedExtras;
    String expectedLanguage;

    // TODO: Move hardcoded String to Constants class/file
    @BeforeEach
    void setUp() throws Exception {
        om = new ObjectMapper();
        baseNode = readFixture(om, "test-data/marketaux_article_ok.json");

        expectedTitle = "Tesla: The End Of The Road For My Bull Case (NASDAQ:TSLA)";
        expectedDescription = "Tesla faces demand headwinds...";
        expectedUrl = URI.create("https://seekingalpha.com/article/4810897-tesla-stock-the-end-of-the-road-for-my-bull-case-downgrade-sell");
        expectedImageUrl = URI.create("https://static.seekingalpha.com/cdn/s3/uploads/getty_images/2169757844/image_2169757844.jpg?io=getty-c-w1536");
        expectedPublishedAt = Instant.parse("2025-08-08T08:27:27.000000Z");
        expectedSource = "seekingalpha.com";
        expectedTickers = List.of("TSLA");
        expectedExtras = Map.of(
                "marketaux.snippet", "Small deep value individual investor...",
                "marketaux.uuid", "1ff83b8b-289d-41d2-b141-6c8584e0769f"
        );
        expectedLanguage = "en";
    }

    @Test
    @DisplayName("Maps full fixture correctly")
    void maps_full_fixture_correctly() {
        NewsItem item = MarketauxNewsMapper.map(baseNode.deepCopy());

        assertEquals(expectedTitle, item.title());
        assertEquals(expectedDescription, item.description());
        assertEquals(expectedUrl, item.url());
        assertEquals(expectedImageUrl, item.imageUrl());
        assertEquals(expectedPublishedAt, item.publishedAt());
        assertEquals(expectedSource, item.source());
        assertIterableEquals(expectedTickers, item.tickers());
        assertEquals(expectedExtras, item.extras());
        assertEquals(expectedLanguage, item.language());
        assertNull(item.providerRef());
    }

    @Test
    @DisplayName("Uses fallback when title is blank")
    void maps_with_blank_title() {
        ObjectNode node = baseNode.deepCopy();
        set(node, "title", " ");
        NewsItem item = MarketauxNewsMapper.map(node);
        assertEquals("(no title)", item.title());
    }

    @Test
    @DisplayName("Handles missing entities and invalid image URL")
    void maps_when_missing_entities_and_invalid_image() {
        ObjectNode node = baseNode.deepCopy();
        remove(node, "entities");
        set(node, "image_url", "http://[bad-url");

        expectedTickers = List.of();
        expectedImageUrl = null;

        NewsItem item = MarketauxNewsMapper.map(node);

        assertIterableEquals(expectedTickers, item.tickers());
        assertNull(item.imageUrl());
    }

    @Test
    @DisplayName("Handles missing published_at and language fields")
    void maps_when_missing_published_at_and_language() {
        ObjectNode node = baseNode.deepCopy();
        remove(node, "published_at");
        remove(node, "language");

        expectedPublishedAt = Instant.EPOCH;
        expectedLanguage = null;

        NewsItem item = MarketauxNewsMapper.map(node);

        assertEquals(expectedPublishedAt, item.publishedAt());
        assertNull(item.language());
    }

    @Test
    @DisplayName("Handles invalid published_at format")
    void maps_with_invalid_published_at() {
        ObjectNode node = baseNode.deepCopy();
        set(node, "published_at", "invalid-date");
        NewsItem item = MarketauxNewsMapper.map(node);
        assertEquals(Instant.EPOCH, item.publishedAt());
    }

    @Test
    @DisplayName("Produces unmodifiable extras map")
    void extras_is_unmodifiable() {
        NewsItem item = MarketauxNewsMapper.map(baseNode.deepCopy());
        assertThrows(UnsupportedOperationException.class, () -> item.extras().put("test", "value"));
    }


    // Helpers TODO: Move to separate test utils module

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
