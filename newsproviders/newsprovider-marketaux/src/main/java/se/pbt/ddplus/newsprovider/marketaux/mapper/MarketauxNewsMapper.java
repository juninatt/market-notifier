package se.pbt.ddplus.newsprovider.marketaux.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import se.pbt.ddplus.core.news.NewsItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Maps a single Marketaux news JSON node to a {@link NewsItem}.
 * Handles provider-specific fields, normalization, and safe parsing.
 */
public final class MarketauxNewsMapper {

    private MarketauxNewsMapper() { }

    /**
     * Converts a Marketaux JSON node to a {@link NewsItem}, applying fallbacks for missing/invalid fields.
     */
    public static NewsItem map(JsonNode n) {
        String title = n.path("title").asText("");
        String description = emptyToNull(n.path("description").asText(null));
        URI url = safeUri(n.path("url").asText(null));
        URI imageUrl = safeUri(n.path("image_url").asText(null));
        Instant publishedAt = parseInstant(n.path("published_at").asText(null));
        String source = emptyToNull(n.path("source").asText(null));

        List<String> tickers = extractTickers(n.path("entities"));

        Map<String, String> extras = new LinkedHashMap<>();
        putIfHasText(extras, "marketaux.snippet", n.path("snippet").asText(null));
        putIfHasText(extras, "marketaux.uuid", n.path("uuid").asText(null));

        String language = emptyToNull(n.path("language").asText(null));

        return new NewsItem(
                title.isBlank() ? "(no title)" : title,
                description,
                url,
                imageUrl,
                publishedAt == null ? Instant.EPOCH : publishedAt,
                source,
                tickers,
                Map.copyOf(extras),
                null,
                language
        );
    }

    /**
     * Extracts ticker symbols from the Marketaux "entities" array, ensuring uniqueness and uppercase format.
     */
    private static List<String> extractTickers(JsonNode entitiesNode) {
        if (!(entitiesNode instanceof ArrayNode array) || array.isEmpty()) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (JsonNode e : array) {
            String sym = e.path("symbol").asText(null);
            if (sym != null) {
                String norm = sym.trim().toUpperCase(Locale.ROOT);
                if (!norm.isEmpty()) set.add(norm);
            }
        }
        return List.copyOf(set);
    }

    /**
     * Parses an ISO-8601 string into an Instant, returning null if blank or invalid.
     */
    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
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
