package se.pbt.ddplus.newsprovider.marketaux.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import se.pbt.ddplus.core.news.NewsItem;
import se.pbt.ddplus.newsprovider.common.MappingUtils;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Maps a Marketaux news JSON node to a {@link NewsItem} with safe parsing and fallbacks.
 */
public final class MarketauxNewsMapper {

    private MarketauxNewsMapper() { }

    /**
     * Converts a Marketaux JSON node to a {@link NewsItem}, applying formatting rules and safe defaults for missing or invalid values.
     */
    public static NewsItem map(JsonNode n) {
        String title = MappingUtils.validateTitle(n.path("title").asText(""));
        String description = MappingUtils.parseTextField(n, "description");
        URI url = MappingUtils.parseUri(n.path("url").asText(null));
        URI imageUrl = MappingUtils.parseUri(n.path("image_url").asText(null));

        Instant publishedAt = MappingUtils.parseInstant(n.path("published_at").asText(null));
        if (publishedAt == null) {
            publishedAt = Instant.EPOCH;
        }

        String source = MappingUtils.parseTextField(n, "source");

        List<String> tickers = MappingUtils.parseFieldValuesToUppercaseList(
                (n.path("entities").isArray() ? (ArrayNode) n.path("entities") : null),
                "symbol"
        );

        Map<String, String> extras = new LinkedHashMap<>();
        MappingUtils.putIfHasText(extras, "marketaux.snippet", n.path("snippet").asText(null));
        MappingUtils.putIfHasText(extras, "marketaux.uuid", n.path("uuid").asText(null));

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
