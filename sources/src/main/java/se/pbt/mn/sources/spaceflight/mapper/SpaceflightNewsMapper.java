package se.pbt.mn.sources.spaceflight.mapper;

import tools.jackson.databind.JsonNode;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.sources.common.MappingUtils;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a Spaceflight News API article JSON node to a {@link NewsItem} with safe parsing and fallbacks.
 */
public final class SpaceflightNewsMapper {

    private SpaceflightNewsMapper() { }

    /**
     * Converts a Spaceflight News API JSON node to a {@link NewsItem}, applying formatting rules and safe defaults for missing or invalid values.
     */
    public static NewsItem map(JsonNode n) {
        String title = MappingUtils.validateTitle(n.path("title").asString(""));
        String description = MappingUtils.parseTextField(n, "summary");
        URI url = MappingUtils.parseUri(n.path("url").asString(null));
        URI imageUrl = MappingUtils.parseUri(n.path("image_url").asString(null));

        Instant publishedAt = MappingUtils.parseInstant(n.path("published_at").asString(null));
        if (publishedAt == null) {
            publishedAt = Instant.EPOCH;
        }

        String source = MappingUtils.parseTextField(n, "news_site");

        Map<String, String> extras = new LinkedHashMap<>();
        JsonNode authors = n.path("authors");
        if (authors.isArray() && !authors.isEmpty()) {
            MappingUtils.putIfHasText(extras, "spaceflightnews.author", MappingUtils.parseTextField(authors.get(0), "name"));
        }

        String idStr = n.hasNonNull("id") ? n.get("id").asString() : "null";
        NewsItem.ProviderRef providerRef = new NewsItem.ProviderRef("spaceflightnews", idStr);

        return new NewsItem(
                title,
                description,
                url,
                imageUrl,
                publishedAt,
                source,
                List.of(),
                Map.copyOf(extras),
                providerRef,
                null
        );
    }
}
