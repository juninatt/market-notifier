package se.pbt.marketnotifier.subscription.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.testutil.SubscriptionTestFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscriptionFormatter")
class SubscriptionFormatterTest {

    private final SubscriptionFormatter formatter = new SubscriptionFormatter();

    @Nested
    @DisplayName("Basic formatting")
    class BasicFormatting {

        @Test
        @DisplayName("Formats subscription with all fields correctly")
        void formatsSubscription_whenAllFieldsArePresent_returnsExpectedText() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("Stock", "Market"),
                    List.of("AAPL"),
                    "en"
            );
            var sub = SubscriptionTestFactory.subscription("sub-1", filter, true);

            String result = formatter.format(sub);

            assertAll(
                    () -> assertTrue(result.contains("ID: sub-1")),
                    () -> assertTrue(result.contains("Keywords: Stock, Market")),
                    () -> assertTrue(result.contains("Lang: en")),
                    () -> assertTrue(result.contains("Enabled: true"))
            );
        }

        @Test
        @DisplayName("Includes 'Enabled: false' when subscription is disabled")
        void formatsSubscription_whenDisabled_includesFalseFlag() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("Economy"),
                    List.of("MSFT"),
                    "sv"
            );
            var sub = SubscriptionTestFactory.subscription("sub-2", filter, false);

            String result = formatter.format(sub);

            assertTrue(result.contains("Enabled: false"));
        }
    }

    @Nested
    @DisplayName("Null safety and fallbacks")
    class NullSafety {

        @Test
        @DisplayName("Handles null filter safely")
        void formatsSubscription_whenFilterIsNull_usesFallbackValues() {
            Subscription sub = SubscriptionTestFactory.subscriptionWithIdOnly("sub-3");

            String result = formatter.format(sub);

            assertTrue(result.contains("(no keywords)"));
            assertTrue(result.contains("(unknown)"));
        }

        @Test
        @DisplayName("Returns placeholder when subscription is null")
        void formatsSubscription_whenNull_returnsPlaceholder() {
            String result = formatter.format(null);
            assertEquals("(invalid subscription)", result);
        }

        @Test
        @DisplayName("Replaces missing ID with '(no id)'")
        void formatsSubscription_whenIdIsMissing_insertsPlaceholder() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("AI"),
                    List.of("GOOG"),
                    "en"
            );
            var sub = SubscriptionTestFactory.subscription(null, filter, true);

            String result = formatter.format(sub);

            assertTrue(result.contains("ID: (no id)"));
        }

        @Test
        @DisplayName("Displays '(no keywords)' when keyword list is empty")
        void formatsSubscription_whenKeywordsEmpty_displaysPlaceholder() {
            var filter = SubscriptionTestFactory.filter(List.of(), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-4", filter, false);

            String result = formatter.format(sub);

            assertTrue(result.contains("(no keywords)"));
        }

        @Test
        @DisplayName("Displays '(unknown)' when language is missing")
        void formatsSubscription_whenLanguageMissing_displaysUnknownTag() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("Finance"),
                    List.of("SP500"),
                    null
            );
            var sub = SubscriptionTestFactory.subscription("sub-5", filter, false);

            String result = formatter.format(sub);

            assertTrue(result.contains("Lang: (unknown)"));
        }

        @Test
        @DisplayName("Handles null tickers list without errors")
        void formatsSubscription_whenTickersNull_handlesGracefully() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("Tech"),
                    null,
                    "en"
            );
            var sub = SubscriptionTestFactory.subscription("sub-10", filter, true);

            String result = formatter.format(sub);

            assertTrue(result.contains("Tech"));
        }
    }

    @Nested
    @DisplayName("Keyword formatting")
    class KeywordFormatting {

        @Test
        @DisplayName("Joins multiple keywords with comma and space")
        void formatsSubscription_whenMultipleKeywords_joinsWithComma() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("One", "Two", "Three"),
                    List.of("AAPL"),
                    "en"
            );
            var sub = SubscriptionTestFactory.subscription("sub-6", filter, true);

            String result = formatter.format(sub);

            assertTrue(result.contains("Keywords: One, Two, Three"));
        }

        @Test
        @DisplayName("Shows '(no keywords)' when keyword list is empty")
        void formatsSubscription_whenNoKeywords_showsPlaceholder() {
            var filter = SubscriptionTestFactory.filter(List.of(), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription(filter);

            String result = formatter.format(sub);

            assertTrue(result.contains("Keywords: (no keywords)"));
        }

        @Test
        @DisplayName("Does not trim or reorder keywords; preserves original strings")
        void formatsSubscription_whenKeywordsContainSpaces_preservesOriginalFormatting() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("  Apple ", " Microsoft  ", "   Tesla"),
                    List.of("AAPL"),
                    "en"
            );
            var sub = SubscriptionTestFactory.subscription("sub-7", filter, true);

            String result = formatter.format(sub);

            assertTrue(result.contains("Keywords:   Apple ,  Microsoft  ,    Tesla"));
        }

        @Test
        @DisplayName("Handles non-Latin or special characters in keywords")
        void formatsSubscription_whenKeywordsContainSpecialCharacters_displaysThemCorrectly() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("börs", "株式", "ação"),
                    List.of("TSLA"),
                    "sv"
            );
            var sub = SubscriptionTestFactory.subscription("sub-8", filter, true);

            String result = formatter.format(sub);

            assertTrue(result.contains("börs"));
            assertTrue(result.contains("株式"));
            assertTrue(result.contains("ação"));
        }

        @Test
        @DisplayName("Preserves original case of keywords and language")
        void formatsSubscription_whenUppercaseKeywords_preservesOriginalCase() {
            var filter = SubscriptionTestFactory.filter(
                    List.of("Tesla", "AI"),
                    List.of("TSLA"),
                    "EN"
            );
            var sub = SubscriptionTestFactory.subscription("sub-9", filter, true);

            String result = formatter.format(sub);

            assertTrue(result.contains("Keywords: Tesla, AI"));
            assertTrue(result.contains("Lang: EN"));
        }

        @Test
        @DisplayName("Formats long keyword lists correctly")
        void formatsSubscription_whenManyKeywords_formatsAllCorrectly() {
            var longList = List.of("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight");
            var filter = SubscriptionTestFactory.filter(longList, List.of("AAPL"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-11", filter, true);

            String result = formatter.format(sub);

            assertTrue(result.contains("Keywords: One, Two, Three, Four, Five, Six, Seven, Eight"));
        }
    }
}
