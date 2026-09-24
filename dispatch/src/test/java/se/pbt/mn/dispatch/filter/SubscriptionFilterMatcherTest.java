package se.pbt.mn.dispatch.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SubscriptionFilterMatcher")
class SubscriptionFilterMatcherTest {

    private static NewsItem item(String title, String description, List<String> tickers, String language) {
        return new NewsItem(
                title, description, URI.create("https://example.com"), null,
                Instant.EPOCH, "Example", tickers, Map.of(),
                new NewsItem.ProviderRef("test", "1"), language
        );
    }

    private static SubscriptionFilter filter(List<String> keywords, List<String> tickers, String language) {
        var f = new SubscriptionFilter();
        f.setKeywords(keywords);
        f.setTickers(tickers);
        f.setLanguage(language);
        return f;
    }

    @Nested
    @DisplayName("Keyword matching")
    class Keywords {

        @Test
        @DisplayName("Matches when a keyword appears in the title (case-insensitive)")
        void matches_whenKeywordInTitle_returnsTrue() {
            var news = item("Tesla stock rallies", "desc", List.of(), null);
            var filter = filter(List.of("tesla"), List.of(), null);

            assertTrue(SubscriptionFilterMatcher.matches(news, filter));
        }

        @Test
        @DisplayName("Matches when a keyword appears only in the description")
        void matches_whenKeywordInDescription_returnsTrue() {
            var news = item("Markets update", "Nvidia beats earnings expectations", List.of(), null);
            var filter = filter(List.of("nvidia"), List.of(), null);

            assertTrue(SubscriptionFilterMatcher.matches(news, filter));
        }

        @Test
        @DisplayName("Does not match when no keyword is present")
        void matches_whenNoKeywordPresent_returnsFalse() {
            var news = item("Weather report", "Sunny skies ahead", List.of(), null);
            var filter = filter(List.of("tesla"), List.of(), null);

            assertFalse(SubscriptionFilterMatcher.matches(news, filter));
        }
    }

    @Nested
    @DisplayName("Ticker matching")
    class Tickers {

        @Test
        @DisplayName("Matches when an item ticker is in the subscription's ticker list")
        void matches_whenTickerOverlaps_returnsTrue() {
            var news = item("Update", "desc", List.of("AAPL", "MSFT"), null);
            var filter = filter(List.of(), List.of("aapl"), null);

            assertTrue(SubscriptionFilterMatcher.matches(news, filter));
        }

        @Test
        @DisplayName("Does not match when the item has no overlapping tickers")
        void matches_whenNoTickerOverlap_returnsFalse() {
            var news = item("Update", "desc", List.of("TSLA"), null);
            var filter = filter(List.of(), List.of("AAPL"), null);

            assertFalse(SubscriptionFilterMatcher.matches(news, filter));
        }

        @Test
        @DisplayName("Does not match when a ticker filter is set but the item has none")
        void matches_whenItemHasNoTickers_returnsFalse() {
            var news = item("Update", "desc", List.of(), null);
            var filter = filter(List.of(), List.of("AAPL"), null);

            assertFalse(SubscriptionFilterMatcher.matches(news, filter));
        }
    }

    @Nested
    @DisplayName("Language matching")
    class Language {

        @Test
        @DisplayName("Matches when languages are equal, case-insensitively")
        void matches_whenLanguageMatches_returnsTrue() {
            var news = item("Update", "desc", List.of(), "EN");
            var filter = filter(List.of(), List.of(), "en");

            assertTrue(SubscriptionFilterMatcher.matches(news, filter));
        }

        @Test
        @DisplayName("Does not match when languages differ")
        void matches_whenLanguageDiffers_returnsFalse() {
            var news = item("Update", "desc", List.of(), "sv");
            var filter = filter(List.of(), List.of(), "en");

            assertFalse(SubscriptionFilterMatcher.matches(news, filter));
        }

        @Test
        @DisplayName("Does not exclude an item with an unknown (null) language")
        void matches_whenItemLanguageUnknown_doesNotExclude() {
            var news = item("Update", "desc", List.of(), null);
            var filter = filter(List.of(), List.of(), "en");

            assertTrue(SubscriptionFilterMatcher.matches(news, filter));
        }
    }

    @Test
    @DisplayName("An empty filter matches everything")
    void matches_withEmptyFilter_returnsTrue() {
        var news = item("Anything", "goes", List.of("XYZ"), "fr");
        var filter = filter(List.of(), List.of(), null);

        assertTrue(SubscriptionFilterMatcher.matches(news, filter));
    }

    @Test
    @DisplayName("All specified categories must match (AND across categories)")
    void matches_requiresAllCategoriesToMatch() {
        var news = item("Tesla news", "desc", List.of("MSFT"), null);
        var filter = filter(List.of("tesla"), List.of("AAPL"), null);

        assertFalse(SubscriptionFilterMatcher.matches(news, filter));
    }

    @Nested
    @DisplayName("Company matching")
    class Companies {

        @Test
        @DisplayName("Matches on ticker regardless of case")
        void matchesAnyCompany_byTickerDifferentCase_matches() {
            var news = item("Market update", "desc", List.of("TSLA"), null);
            assertTrue(SubscriptionFilterMatcher.matchesAnyCompany(news, List.of("tsla")));
        }

        @Test
        @DisplayName("Matches on company name appearing in the title or description")
        void matchesAnyCompany_byName_matches() {
            var news = item("Tesla unveils new factory", "desc", List.of(), null);
            assertTrue(SubscriptionFilterMatcher.matchesAnyCompany(news, List.of("Tesla")));
        }

        @Test
        @DisplayName("Does not match when neither ticker nor name appear")
        void matchesAnyCompany_withNoOverlap_doesNotMatch() {
            var news = item("Weather update", "Rain expected", List.of("AAPL"), null);
            assertFalse(SubscriptionFilterMatcher.matchesAnyCompany(news, List.of("Tesla")));
        }

        @Test
        @DisplayName("Returns false for an empty or null company list")
        void matchesAnyCompany_withEmptyList_doesNotMatch() {
            var news = item("Tesla rallies", "desc", List.of(), null);
            assertFalse(SubscriptionFilterMatcher.matchesAnyCompany(news, List.of()));
            assertFalse(SubscriptionFilterMatcher.matchesAnyCompany(news, null));
        }

        @Test
        @DisplayName("Does not match on a blank company entry")
        void matchesAnyCompany_withBlankEntry_doesNotMatch() {
            var news = item("Tesla rallies", "desc", List.of("TSLA"), null);
            assertFalse(SubscriptionFilterMatcher.matchesAnyCompany(news, List.of("", " ")));
        }
    }

    @Nested
    @DisplayName("Category matching")
    class Categories {

        @Test
        @DisplayName("Matches category text case-insensitively")
        void matchesCategory_withDifferentCase_matches() {
            var news = item("Breakthrough in quantum computing", "desc", List.of(), null);
            assertTrue(SubscriptionFilterMatcher.matchesCategory(news, "qUANTUM cOMPUTING"));
        }

        @Test
        @DisplayName("Does not match unrelated text")
        void matchesCategory_withNoOverlap_doesNotMatch() {
            var news = item("Tesla rallies", "desc", List.of(), null);
            assertFalse(SubscriptionFilterMatcher.matchesCategory(news, "Space"));
        }

        @Test
        @DisplayName("Does not match on a null or blank category")
        void matchesCategory_withNullOrBlank_doesNotMatch() {
            var news = item("Tesla rallies", "desc", List.of(), null);
            assertFalse(SubscriptionFilterMatcher.matchesCategory(news, null));
            assertFalse(SubscriptionFilterMatcher.matchesCategory(news, " "));
        }
    }
}
