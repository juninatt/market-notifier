package se.pbt.tvm.core.news;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Core domain model representing a normalized news article.
 * Carries normalized fields used by all notifiers.
 */
public record NewsItem(
        String title,
        String description,
        URI url,
        URI imageUrl,
        Instant publishedAt,
        String source,
        List<String> tickers,
        Map<String, String> extras,
        ProviderRef providerRef,
        String language
) {
    public record ProviderRef(
            String provider,
            String id
    ) {}
}
