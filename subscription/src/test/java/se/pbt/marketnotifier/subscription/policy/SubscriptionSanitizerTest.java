package se.pbt.marketnotifier.subscription.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.core.subscription.SchedulePreset;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.model.SubscriptionFilter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscriptionSanitizer:")
class SubscriptionSanitizerTest {

    private final SubscriptionSanitizer sanitizer = new SubscriptionSanitizer();

    @Nested
    @DisplayName("normalizeKeywords:")
    class NormalizeKeywords {

        List<String> unsanitized = new ArrayList<>();

        @BeforeEach
        void setup() {
            unsanitized.clear();
        }

        @Test
        @DisplayName("Returns empty list when input is null")
        void returnsEmptyList_ForNullInput() {
            List<String> result = sanitizer.normalizeKeywords(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Removes null values from list")
        void normalizeKeywordsRemovesNulls() {
            unsanitized.addAll(List.of("bill gates", "kim kardashian"));
            // null has to be added separately
            unsanitized.add(null);
            List<String> result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("bill gates", "kim kardashian"), result);
        }

        @Test
        @DisplayName("Removes blank strings from list")
        void normalizeKeywordsRemovesBlanks() {
            unsanitized.addAll(List.of("elon musk", "space", "  "));
            List<String> result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("elon musk", "space"), result);
        }

        @Test
        @DisplayName("Converts all keywords to lowercase")
        void normalizeKeywordsMakesLowercase() {
            unsanitized.addAll(List.of("Volodymyr Zelenskyj", "Vladimir Putin", "Slava Ukraini"));
            List<String> result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("volodymyr zelenskyj", "vladimir putin", "slava ukraini"), result);
        }

        @Test
        @DisplayName("Removes duplicates lower case")
        void removesDuplicatesSameCase() {
            unsanitized.addAll(List.of("ai", "energy", "energy"));
            List<String> result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("ai", "energy"), result);
        }

        @Test
        @DisplayName("Removes duplicates case-insensitively")
        void removesDuplicatesDifferentCase() {
            unsanitized.addAll(List.of("AI", "ai", "AI ", " ai"));
            List<String> result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("ai"), result);
        }
    }

    @Nested
    @DisplayName("containsSameKeywords:")
    class ContainsSameKeywords {

        @Test
        @DisplayName("Returns true when keywords have different case and order")
        void returnsTrue_ForDifferentCaseWordsAndOrder() {
            Subscription s1 = newSubscription(List.of("AI", "ML"), "en");
            Subscription s2 = newSubscription(List.of("ml", "ai"), "en");
            assertTrue(sanitizer.containsSameKeywords(s1, s2));
        }

        @Test
        @DisplayName("Returns true when keywords are in different order")
        void returnsTrue_ForDifferentOrder() {
            Subscription s1 = newSubscription(List.of("ai", "ml"), "en");
            Subscription s2 = newSubscription(List.of("ml", "ai"), "en");
            assertTrue(sanitizer.containsSameKeywords(s1, s2));
        }

        @Test
        @DisplayName("Returns true when language is the same but with different case")
        void returnsTrue_ForDifferentCaseLanguage() {
            Subscription s1 = newSubscription(List.of("AI"), "EN");
            Subscription s2 = newSubscription(List.of("AI"), "en");
            assertTrue(sanitizer.usesSameLanguage(s1, s2));
        }
    }

    @Nested
    @DisplayName("usesSameLanguage:")
    class UsesSameLanguage {

        @Test
        @DisplayName("Returns false when languages differ")
        void returnsFalse_ForDifferentLanguages() {
            Subscription s1 = newSubscription(List.of("AI"), "en");
            Subscription s2 = newSubscription(List.of("AI"), "sv");
            assertFalse(sanitizer.usesSameLanguage(s1, s2));
        }

        @Test
        @DisplayName("Returns false when one language is null")
        void returnsFalse_WhenOneLanguageIsNull() {
            Subscription s1 = newSubscription(List.of("AI"), null);
            Subscription s2 = newSubscription(List.of("AI"), "en");
            assertFalse(sanitizer.usesSameLanguage(s1, s2));
        }

        @Test
        @DisplayName("Returns true when both languages are null")
        void returnsTrue_WhenBothLanguagesAreNull() {
            Subscription s1 = newSubscription(List.of("AI"), null);
            Subscription s2 = newSubscription(List.of("AI"), null);
            assertTrue(sanitizer.usesSameLanguage(s1, s2));
        }
    }

    @Nested
    @DisplayName("special characters:")
    class SpecialCharacters {

        @Test
        @DisplayName("Preserves plus/hash and lowercases consistently (C++, C#)")
        void preservesPlusAndHash() {
            List<String> input = new ArrayList<>(List.of("C++", "C#", "c++"));
            List<String> result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("c++", "c#"), result);
        }

        @Test
        @DisplayName("Preserves accents/diacritics (Göteborg, naïve-bayes)")
        void preservesAccentsAndDiacritics() {
            List<String> input = new ArrayList<>(List.of("Göteborg", "naïve-bayes"));
            List<String> result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("göteborg", "naïve-bayes"), result);
        }

        @Test
        @DisplayName("Preserves punctuation and symbols (?, -, _)")
        void preservesPunctuationAndSymbols() {
            List<String> input = new ArrayList<>(List.of("elon-musk?", "_quant", "  A/B  "));
            List<String> result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("elon-musk?", "_quant", "a/b"), result);
        }

        @Test
        @DisplayName("Preserves emojis")
        void preservesEmojis() {
            List<String> input = new ArrayList<>(List.of("AI 🤖", "🚀 SpaceX"));
            List<String> result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("ai 🤖", "🚀 spacex"), result);
        }

        @Test
        @DisplayName("Trims whitespace around tokens but keeps inner symbols")
        void trimsWhitespaceButKeepsInnerSymbols() {
            List<String> input = new ArrayList<>(List.of("  C#  ", "  Node.js "));
            List<String> result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("c#", "node.js"), result);
        }


        @Nested
        @DisplayName("usesSameSchedule:")
        class UsesSameSchedule {

            @Test
            @DisplayName("Returns true when both subscriptions use the same schedule preset")
            void returnsTrue_ForSameSchedule() {
                Subscription s1 = newSubscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING_EVENING);
                Subscription s2 = newSubscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING_EVENING);
                assertTrue(sanitizer.usesSameSchedule(s1, s2));
            }

            @Test
            @DisplayName("Returns false when schedules differ")
            void returnsFalse_ForDifferentSchedules() {
                Subscription s1 = newSubscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING);
                Subscription s2 = newSubscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.EVENING);
                assertFalse(sanitizer.usesSameSchedule(s1, s2));
            }

            @Test
            @DisplayName("Returns false when one schedule is null")
            void returnsFalse_WhenOneScheduleIsNull() {
                Subscription s1 = newSubscriptionWithSchedule(List.of("AI"), "en", null);
                Subscription s2 = newSubscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING);
                assertFalse(sanitizer.usesSameSchedule(s1, s2));
            }

            @Test
            @DisplayName("Returns true when both schedules are null")
            void returnsTrue_WhenBothSchedulesAreNull() {
                Subscription s1 = newSubscriptionWithSchedule(List.of("AI"), "en", null);
                Subscription s2 = newSubscriptionWithSchedule(List.of("AI"), "en", null);
                assertTrue(sanitizer.usesSameSchedule(s1, s2));
            }
        }

    }

    // Helpers

    private Subscription newSubscription(List<String> keywords, String language) {
            Subscription sub = new Subscription();
            SubscriptionFilter f = new SubscriptionFilter();
            f.setKeywords(keywords);
            f.setLanguage(language);
            sub.setFilter(f);
            return sub;
        }

    private Subscription newSubscriptionWithSchedule(List<String> keywords, String language, SchedulePreset schedule) {
        Subscription sub = newSubscription(keywords, language);
        sub.setSchedule(schedule);
        return sub;
    }

}
