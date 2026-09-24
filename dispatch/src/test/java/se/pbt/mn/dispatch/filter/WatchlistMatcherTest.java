package se.pbt.mn.dispatch.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.mn.core.news.NewsItem;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WatchlistMatcher")
class WatchlistMatcherTest {

    private static NewsItem item(String title, String description, List<String> tickers) {
        return new NewsItem(
                title, description, URI.create("https://example.com/1"), null,
                Instant.EPOCH, "Example", tickers, Map.of(),
                new NewsItem.ProviderRef("test", "1"), null
        );
    }

    @Nested
    @DisplayName("Matching companies")
    class MatchingCompanies {

        @Test
        @DisplayName("Matches on ticker regardless of case")
        void matchesCompany_byTickerDifferentCase_matches() {
            var item = item("Market update", "desc", List.of("TSLA"));
            assertTrue(WatchlistMatcher.matchesCompany(item, "tsla"));
        }

        @Test
        @DisplayName("Matches on company name appearing in the title")
        void matchesCompany_byNameInTitle_matches() {
            var item = item("Tesla unveils new factory", "desc", List.of());
            assertTrue(WatchlistMatcher.matchesCompany(item, "Tesla"));
        }

        @Test
        @DisplayName("Matches on company name appearing in the description")
        void matchesCompany_byNameInDescription_matches() {
            var item = item("Factory opens", "Tesla announced today", List.of());
            assertTrue(WatchlistMatcher.matchesCompany(item, "Tesla"));
        }

        @Test
        @DisplayName("Does not match when neither ticker nor name appear")
        void matchesCompany_withNoOverlap_doesNotMatch() {
            var item = item("Weather update", "Rain expected", List.of("AAPL"));
            assertFalse(WatchlistMatcher.matchesCompany(item, "Tesla"));
        }

        @Test
        @DisplayName("matchesAnyCompany matches if any single company matches")
        void matchesAnyCompany_withOneOfSeveralMatching_matches() {
            var item = item("Tesla rallies", "desc", List.of());
            assertTrue(WatchlistMatcher.matchesAnyCompany(item, List.of("Apple", "Tesla")));
        }

        @Test
        @DisplayName("matchesAnyCompany returns false for an empty company list")
        void matchesAnyCompany_withEmptyList_doesNotMatch() {
            var item = item("Tesla rallies", "desc", List.of());
            assertFalse(WatchlistMatcher.matchesAnyCompany(item, List.of()));
        }
    }

    @Nested
    @DisplayName("Matching categories")
    class MatchingCategories {

        @Test
        @DisplayName("Matches category text case-insensitively")
        void matchesCategory_withDifferentCase_matches() {
            var item = item("Breakthrough in quantum computing", "desc", List.of());
            assertTrue(WatchlistMatcher.matchesCategory(item, "qUANTUM cOMPUTING"));
        }

        @Test
        @DisplayName("Does not match unrelated text")
        void matchesCategory_withNoOverlap_doesNotMatch() {
            var item = item("Tesla rallies", "desc", List.of());
            assertFalse(WatchlistMatcher.matchesCategory(item, "Space"));
        }
    }
}
