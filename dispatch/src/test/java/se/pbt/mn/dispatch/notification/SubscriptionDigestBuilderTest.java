package se.pbt.mn.dispatch.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SubscriptionDigestBuilder")
class SubscriptionDigestBuilderTest {

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

    private static Subscription subscription(List<String> keywords, List<String> tickers,
                                              List<String> companies, List<String> categories, int maxItems) {
        var filter = new SubscriptionFilter();
        filter.setKeywords(keywords);
        filter.setTickers(tickers);
        filter.setCompanies(companies);
        filter.setCategories(categories);

        var subscription = new Subscription();
        subscription.setId("sub-1");
        subscription.setEmail("you@example.com");
        subscription.setMaxItems(maxItems);
        subscription.setFilter(filter);
        return subscription;
    }

    @Nested
    @DisplayName("No matches")
    class NoMatches {

        @Test
        @DisplayName("Returns empty when nothing matches")
        void build_withNoMatches_returnsEmpty() {
            var groups = List.of(group(item("1", "Weather report", List.of(), Instant.EPOCH)));
            var subscription = subscription(List.of("Tesla"), List.of(), List.of("Apple"), List.of("Space"), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Does not treat an empty keywords/tickers filter as matching everything")
        void build_withEmptyKeywordsAndTickers_doesNotDumpEveryItemIntoMatches() {
            var groups = List.of(group(item("1", "Completely unrelated item", List.of(), Instant.EPOCH)));
            var subscription = subscription(List.of(), List.of(), List.of("Tesla"), List.of(), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Treats a null categories list as empty")
        void build_withNullCategories_returnsEmpty() {
            var groups = List.of(group(item("1", "Weather report", List.of(), Instant.EPOCH)));
            var subscription = subscription(List.of("Tesla"), List.of(), List.of(), null, 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Regular keyword/ticker matches")
    class RegularMatches {

        @Test
        @DisplayName("Includes matches capped to maxItems, same as the recurring dispatcher")
        void build_withKeywordMatches_capsToMaxItems() {
            Instant now = Instant.now();
            var groups = List.of(
                    group(item("1", "Tesla rallies", List.of(), now)),
                    group(item("2", "Tesla opens plant", List.of(), now.plus(1, ChronoUnit.DAYS)))
            );
            var subscription = subscription(List.of("Tesla"), List.of(), List.of(), List.of(), 1);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

            assertTrue(result.isPresent());
            String body = result.get().body();
            assertTrue(body.contains("MATCHES"));
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
            var subscription = subscription(List.of(), List.of(), List.of("Tesla"), List.of(), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

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
            var subscription = subscription(List.of(), List.of(), List.of(), List.of("AI"), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 2);

            assertTrue(result.isPresent());
            String body = result.get().body();
            assertTrue(body.contains("AI startup raises funding"));
            assertTrue(body.contains("AI breakthrough announced"));
            assertFalse(body.contains("AI model released"));
        }
    }

    @Nested
    @DisplayName("Combined sections")
    class CombinedSections {

        @Test
        @DisplayName("Includes matches, companies, and categories together in one digest")
        void build_withAllThreeKinds_includesAllSections() {
            Instant now = Instant.now();
            var groups = List.of(
                    group(item("1", "Volvo unveils new model", List.of(), now)),
                    group(item("2", "Tesla rallies", List.of("TSLA"), now)),
                    group(item("3", "Space telescope launched", List.of(), now))
            );
            var subscription = subscription(List.of("Volvo"), List.of(), List.of("Tesla"), List.of("Space"), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

            assertTrue(result.isPresent());
            String body = result.get().body();
            assertTrue(body.contains("Volvo unveils new model"));
            assertTrue(body.contains("Tesla rallies"));
            assertTrue(body.contains("Space telescope launched"));
        }

        @Test
        @DisplayName("Lists a group once, in the first section it matches, even if later ones match too")
        void build_withGroupMatchingSeveralSections_listsItOnce() {
            var groups = List.of(group(item("1", "Tesla rallies", List.of("TSLA"), Instant.EPOCH)));
            var subscription = subscription(List.of("Tesla"), List.of(), List.of("TSLA"), List.of("rallies"), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

            assertTrue(result.isPresent());
            String body = result.get().body();
            assertEquals("MATCHES\nTesla rallies\nhttps://example.com/1\nSources: Example", body);
        }

        @Test
        @DisplayName("Fills a category's limit from groups not already listed in an earlier section")
        void build_withCategoryOverlappingCompanies_fillsLimitFromUnlistedGroups() {
            Instant now = Instant.now();
            var groups = List.of(
                    group(item("1", "Tesla AI chip unveiled", List.of("TSLA"), now)),
                    group(item("2", "AI startup raises funding", List.of(), now.minus(1, ChronoUnit.DAYS)))
            );
            var subscription = subscription(List.of(), List.of(), List.of("TSLA"), List.of("AI"), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 1);

            assertTrue(result.isPresent());
            String body = result.get().body();
            assertTrue(body.contains("COMPANIES\nTesla AI chip unveiled"));
            assertTrue(body.contains("AI\nAI startup raises funding"));
        }
    }

    @Nested
    @DisplayName("Item presentation")
    class Presentation {

        @Test
        @DisplayName("Lists every distinct source in a group after its title and link")
        void build_withGroupFromSeveralSources_listsEverySource() {
            Instant now = Instant.now();
            var reuters = new NewsItem(
                    "Tesla rallies", "desc", URI.create("https://example.com/1"), null,
                    now, "Reuters", List.of("TSLA"), Map.of(), new NewsItem.ProviderRef("test", "1"), null);
            var marketWatch = new NewsItem(
                    "Tesla stock jumps", "desc", URI.create("https://example.com/2"), null,
                    now.plus(1, ChronoUnit.HOURS), "MarketWatch", List.of("TSLA"), Map.of(),
                    new NewsItem.ProviderRef("test", "2"), null);
            var groups = List.of(new NewsGroup(List.of(reuters, marketWatch)));
            var subscription = subscription(List.of("Tesla"), List.of(), List.of(), List.of(), 10);

            var result = SubscriptionDigestBuilder.build(groups, subscription, 10);

            assertTrue(result.isPresent());
            assertEquals("MATCHES\nTesla rallies\nhttps://example.com/1\nSources: Reuters, MarketWatch", result.get().body());
        }
    }
}
