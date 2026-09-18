package se.pbt.mn.sources.marketaux.mapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.sources.common.MappingUtils;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a Marketaux news JSON node to a {@link NewsItem} with safe parsing and fallbacks.
 */
public final class MarketauxNewsMapper {

    private MarketauxNewsMapper() { }

    /**
     * Converts a Marketaux JSON node to a {@link NewsItem}, applying formatting rules and safe defaults for missing or invalid values.
     */
    public static NewsItem map(JsonNode n) {
        String title = MappingUtils.validateTitle(n.path("title").asString(""));
        String description = MappingUtils.parseTextField(n, "description");
        URI url = MappingUtils.parseUri(n.path("url").asString(null));
        URI imageUrl = MappingUtils.parseUri(n.path("image_url").asString(null));

        Instant publishedAt = MappingUtils.parseInstant(n.path("published_at").asString(null));
        if (publishedAt == null) {
            publishedAt = Instant.EPOCH;
        }

        String source = MappingUtils.parseTextField(n, "source");

        List<String> tickers = MappingUtils.parseFieldValuesToUppercaseList(
                (n.path("entities").isArray() ? (ArrayNode) n.path("entities") : null),
                "symbol"
        );

        Map<String, String> extras = new LinkedHashMap<>();
        MappingUtils.putIfHasText(extras, "marketaux.snippet", n.path("snippet").asString(null));
        MappingUtils.putIfHasText(extras, "marketaux.uuid", n.path("uuid").asString(null));

        String language = MappingUtils.parseTextField(n, "language");

        return new NewsItem(
                title,
                description,
                url,
                imageUrl,
                publishedAt,
                source,
                tickers,
                Map.copyOf(extras),
                null,
                language
        );
    }
}
