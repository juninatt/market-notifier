package se.pbt.marketnotifier.newsprovider.finnhub.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.core.news.NewsItem;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FinnhubNewsMapper")
class FinnhubNewsMapperTest {

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
    String expectedProvider;
    String expectedProviderId;

    // TODO: Move hardcoded String to Constants class/file
    @BeforeEach
    void setUp() throws Exception {
        om = new ObjectMapper();
        baseNode = readFixture(om, "test-data/finnhub_article_ok.json");

        expectedTitle = "Instacart says its grocery partners are starting to ‘embrace more competitive pricing,’ as demand forecast tops estimates";
        expectedDescription = "Shares of Instacart rallied late Thursday after the grocery-delivery app’s forecast for a key demand metric came in above Wall Street’s estimates.";
        expectedUrl = URI.create("https://www.marketwatch.com/story/instacart-says-its-grocery-partners-are-starting-to-embrace-more-competitive-pricing-as-demand-forecast-tops-estimates-fd30777a");
        expectedImageUrl = URI.create("https://static2.finnhub.io/file/publicdatany/finnhubimage/market_watch_logo.png");
        expectedPublishedAt = Instant.ofEpochSecond(1754602020L);
        expectedSource = "MarketWatch";
        expectedTickers = List.of("TSLA", "AAPL", "MSFT");
        expectedExtras = Map.of(
                "finnhub.category", "top news",
                "finnhub.image", expectedImageUrl.toString()
        );
        expectedProvider = "finnhub";
        expectedProviderId = "7504927";
    }

    @Test
    @DisplayName("Maps full fixture correctly")
    void maps_test_data_correctly() {
        NewsItem item = FinnhubNewsMapper.map(baseNode.deepCopy());

        assertEquals(expectedTitle, item.title());
        assertEquals(expectedDescription, item.description());
        assertEquals(expectedUrl, item.url());
        assertEquals(expectedImageUrl, item.imageUrl());
        assertEquals(expectedPublishedAt, item.publishedAt());
        assertEquals(expectedSource, item.source());
        assertIterableEquals(expectedTickers, item.tickers());
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
        set(node, "headline", "  ");
        NewsItem item = FinnhubNewsMapper.map(node);
        assertEquals("(no title)", item.title());
    }

    @Test
    @DisplayName("Handles missing optional fields")
    void maps_when_missing_optional_fields() {
        ObjectNode node = baseNode.deepCopy();
        remove(node, "summary");
        remove(node, "image");

        expectedDescription = null;
        expectedImageUrl = null;
        expectedExtras = Map.of("finnhub.category", "top news");

        NewsItem item = FinnhubNewsMapper.map(node);

        assertEquals(expectedTitle, item.title());
        assertNull(item.description());
        assertEquals(expectedUrl, item.url());
        assertNull(item.imageUrl());
        assertEquals(expectedPublishedAt, item.publishedAt());
        assertEquals(expectedSource, item.source());
        assertIterableEquals(expectedTickers, item.tickers());
        assertEquals(expectedExtras, item.extras());
    }

    @Test
    @DisplayName("Handles invalid URL and blank related field")
    void maps_with_invalid_url_and_blank_related() {
        ObjectNode node = baseNode.deepCopy();
        set(node, "url", "::::");
        set(node, "related", " , ,");

        expectedUrl = null;
        expectedTickers = List.of();

        NewsItem item = FinnhubNewsMapper.map(node);

        assertNull(item.url());
        assertIterableEquals(expectedTickers, item.tickers());
    }

    @Test
    @DisplayName("Produces unmodifiable extras map")
    void extras_is_unmodifiable() {
        NewsItem item = FinnhubNewsMapper.map(baseNode.deepCopy());
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

