package se.pbt.mn.dispatch.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.subscription.model.Watchlist;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WatchlistDigestBuilder")
class WatchlistDigestBuilderTest {

    private static NewsItem item(String id, String title, List<String> tickers, Instant publishedAt) {
        return new NewsItem(
                title, "desc", URI.create("https://example.com/" + id), null,
                publishedAt, "Example", tickers, Map.of(),
                new NewsItem.ProviderRef("test", id), null
        );
    }

    private static NewsGroup group(NewsItem item) {
        return new NewsGroup(List.of(item));
    }

    private static Watchlist watchlist(List<String> companies, List<String> categories) {
        var watchlist = new Watchlist();
        watchlist.setEmail("you@example.com");
        watchlist.setCompanies(companies);
        watchlist.setCategories(categories);
        return watchlist;
    }

    @Nested
    @DisplayName("No matches")
    class NoMatches {

        @Test
        @DisplayName("Returns empty when nothing in the watchlist matches any group")
        void build_withNoMatches_returnsEmpty() {
            var groups = List.of(group(item("1", "Weather report", List.of(), Instant.EPOCH)));
            var watchlist = watchlist(List.of("Tesla"), List.of("Space"));

            var result = WatchlistDigestBuilder.build(groups, watchlist, 10);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Company matches")
    class CompanyMatches {

        @Test
        @DisplayName("Includes every matching group with no upper limit")
        void build_withManyCompanyMatches_includesAll() {
            Instant now = Instant.now();
            var groups = List.of(
                    group(item("1", "Tesla rallies", List.of("TSLA"), now)),
                    group(item("2", "Tesla opens new plant", List.of("TSLA"), now.plus(1, ChronoUnit.DAYS))),
                    group(item("3", "Tesla recalls vehicles", List.of("TSLA"), now.plus(2, ChronoUnit.DAYS)))
            );
            var watchlist = watchlist(List.of("Tesla"), List.of());

            var result = WatchlistDigestBuilder.build(groups, watchlist, 10);

            assertTrue(result.isPresent());
            String body = result.get().body();
            assertTrue(body.contains("Tesla rallies"));
            assertTrue(body.contains("Tesla opens new plant"));
            assertTrue(body.contains("Tesla recalls vehicles"));
        }
    }

    @Nested
    @DisplayName("Category matches")
    class CategoryMatches {

        @Test
        @DisplayName("Caps the number of items per category to the given limit")
        void build_withMoreCategoryMatchesThanLimit_truncatesToLimit() {
            Instant now = Instant.now();
            var groups = List.of(
                    group(item("1", "AI model released", List.of(), now)),
                    group(item("2", "AI breakthrough announced", List.of(), now.plus(1, ChronoUnit.DAYS))),
                    group(item("3", "AI startup raises funding", List.of(), now.plus(2, ChronoUnit.DAYS)))
            );
            var watchlist = watchlist(List.of(), List.of("AI"));

            var result = WatchlistDigestBuilder.build(groups, watchlist, 2);

            assertTrue(result.isPresent());
            String body = result.get().body();
            assertTrue(body.contains("AI startup raises funding"));
            assertTrue(body.contains("AI breakthrough announced"));
            assertFalse(body.contains("AI model released"));
        }

        @Test
        @DisplayName("Matches categories case-insensitively")
        void build_withDifferentCaseCategory_stillMatches() {
            var groups = List.of(group(item("1", "Quantum computing milestone reached", List.of(), Instant.now())));
            var watchlist = watchlist(List.of(), List.of("qUANTUM cOMPUTING"));

            var result = WatchlistDigestBuilder.build(groups, watchlist, 10);

            assertTrue(result.isPresent());
            assertTrue(result.get().body().contains("Quantum computing milestone reached"));
        }
    }
}
