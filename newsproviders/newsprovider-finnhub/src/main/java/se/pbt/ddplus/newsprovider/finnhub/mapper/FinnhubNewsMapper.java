package se.pbt.ddplus.newsprovider.finnhub.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import se.pbt.ddplus.core.news.NewsItem;
import se.pbt.ddplus.newsprovider.common.MappingUtils;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Maps a Finnhub news JSON node to a {@link NewsItem} with safe parsing and fallbacks.
 */
public final class FinnhubNewsMapper {

    private FinnhubNewsMapper() { }

    /**
     * Converts a Finnhub JSON node to a {@link NewsItem}, applying formatting rules and safe defaults for missing or invalid values.
     */
    public static NewsItem map(JsonNode n) {
        String title = MappingUtils.validateTitle(n.path("headline").asText(""));
        String description = MappingUtils.parseTextField(n, "summary");
        URI url = MappingUtils.parseUri(n.path("url").asText(null));
        URI imageUrl = MappingUtils.parseUri(n.path("image").asText(null));

        long epoch = n.path("datetime").asLong(0);
        Instant publishedAt = MappingUtils.parseEpochSeconds(epoch);

        String source = MappingUtils.parseTextField(n, "source");

        List<String> tickers = MappingUtils.parseCsvToUppercaseList(n.path("related").asText(null));

        Map<String, String> extras = new LinkedHashMap<>();
        MappingUtils.putIfHasText(extras, "finnhub.category", n.path("category").asText(null));
        MappingUtils.putIfHasText(extras, "finnhub.image", n.path("image").asText(null));

        String idStr = n.hasNonNull("id") ? n.get("id").asText() : "null";
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
