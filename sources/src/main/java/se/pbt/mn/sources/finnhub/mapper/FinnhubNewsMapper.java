package se.pbt.mn.sources.finnhub.mapper;

import tools.jackson.databind.JsonNode;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.sources.common.MappingUtils;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a Finnhub news JSON node to a {@link NewsItem} with safe parsing and fallbacks.
 */
public final class FinnhubNewsMapper {

    private FinnhubNewsMapper() { }

    /**
     * Converts a Finnhub JSON node to a {@link NewsItem}, applying formatting rules and safe defaults for missing or invalid values.
     */
    public static NewsItem map(JsonNode n) {
        String title = MappingUtils.validateTitle(n.path("headline").asString(""));
        String description = MappingUtils.parseTextField(n, "summary");
        URI url = MappingUtils.parseUri(n.path("url").asString(null));
        URI imageUrl = MappingUtils.parseUri(n.path("image").asString(null));

        long epoch = n.path("datetime").asLong(0);
        Instant publishedAt = MappingUtils.parseEpochSeconds(epoch);

        String source = MappingUtils.parseTextField(n, "source");

        List<String> tickers = MappingUtils.parseCsvToUppercaseList(n.path("related").asString(null));

        Map<String, String> extras = new LinkedHashMap<>();
        MappingUtils.putIfHasText(extras, "finnhub.category", n.path("category").asString(null));
        MappingUtils.putIfHasText(extras, "finnhub.image", n.path("image").asString(null));

        String idStr = n.hasNonNull("id") ? n.get("id").asString() : "null";
        NewsItem.ProviderRef providerRef = new NewsItem.ProviderRef("finnhub", idStr);

        return new NewsItem(
                title,
                description,
                url,
                imageUrl,
                publishedAt,
                source,
                tickers,
                Map.copyOf(extras),
                providerRef,
                null
        );
    }
}
