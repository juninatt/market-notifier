package se.pbt.ddplus.newsprovider.finnhub.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import se.pbt.ddplus.core.news.NewsItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;

/**
 * Maps a single Finnhub news JSON node to a {@link NewsItem}.
 * Handles provider-specific fields, normalization, and safe parsing.
 */
public final class FinnhubNewsMapper {

    private FinnhubNewsMapper() { }

    /**
     * Converts a Finnhub JSON node to a {@link NewsItem}, applying fallbacks for missing/invalid fields.
     */
    public static NewsItem map(JsonNode n) {
        String title = n.path("headline").asText("");
        String description = emptyToNull(n.path("summary").asText(null));
        URI url = safeUri(n.path("url").asText(null));
        URI imageUrl = safeUri(n.path("image").asText(null));

        long epoch = n.path("datetime").asLong(0);
        Instant publishedAt = epoch > 0 ? Instant.ofEpochSecond(epoch) : Instant.EPOCH;

        String source = emptyToNull(n.path("source").asText(null));

        List<String> tickers = parseRelated(n.path("related").asText(null));

        Map<String, String> extras = new LinkedHashMap<>();
        putIfHasText(extras, "finnhub.category", n.path("category").asText(null));
        putIfHasText(extras, "finnhub.image", n.path("image").asText(null));

        String idStr = n.hasNonNull("id") ? n.get("id").asText() : "null";
        NewsItem.ProviderRef providerRef = new NewsItem.ProviderRef("finnhub", idStr);

        return new NewsItem(
                title.isBlank() ? "(no title)" : title,
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

    /**
     * Parses a comma-separated related string into a unique list of uppercase ticker symbols.
     */
    private static List<String> parseRelated(String related) {
        if (related == null || related.isBlank()) return List.of();
        String[] parts = related.split(",");
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String p : parts) {
            String norm = p.trim().toUpperCase(Locale.ROOT);
            if (!norm.isEmpty()) set.add(norm);
        }
        return List.copyOf(set);
    }

    /**
     * Parses a string into a URI, returning null if the value is blank or invalid.
     */
    private static URI safeUri(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new URI(raw);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Returns null if the string is null or blank, otherwise returns the string itself.
     */
    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * Adds a key-value pair to a map if the value is non-null and non-blank.
     */
    private static void putIfHasText(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}
