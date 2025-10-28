package se.pbt.tvm.subscription.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.tvm.core.subscription.SchedulePreset;
import se.pbt.tvm.subscription.testutil.SubscriptionTestFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscriptionSanitizer")
class SubscriptionSanitizerTest {

    private final SubscriptionSanitizer sanitizer = new SubscriptionSanitizer();

    @Nested
    @DisplayName("normalizeKeywords")
    class NormalizeKeywords {

        List<String> unsanitized = new ArrayList<>();

        @BeforeEach
        void setup() {
            unsanitized.clear();
        }

        @Test
        @DisplayName("Returns empty list when input is null")
        void normalize_withNullInput_returnsEmptyList() {
            var result = sanitizer.normalizeKeywords(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Removes null values from list")
        void normalize_removesNullValues() {
            unsanitized.addAll(List.of("bill gates", "kim kardashian"));
            // null has to be added separately
            unsanitized.add(null);
            var result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("bill gates", "kim kardashian"), result);
        }

        @Test
        @DisplayName("Removes blank strings from list")
        void normalize_removesBlankStrings() {
            unsanitized.addAll(List.of("elon musk", "space", "  "));
            var result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("elon musk", "space"), result);
        }

        @Test
        @DisplayName("Converts all keywords to lowercase")
        void normalize_convertsToLowercase() {
            unsanitized.addAll(List.of("Volodymyr Zelenskyj", "Vladimir Putin", "Slava Ukraini"));
            var result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("volodymyr zelenskyj", "vladimir putin", "slava ukraini"), result);
        }

        @Test
        @DisplayName("Removes duplicates same case")
        void normalize_removesDuplicateSameCase() {
            unsanitized.addAll(List.of("ai", "energy", "energy"));
            var result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("ai", "energy"), result);
        }

        @Test
        @DisplayName("Removes duplicates case-insensitively")
        void normalize_removesDuplicateDifferentCase() {
            unsanitized.addAll(List.of("AI", "ai", "AI ", " ai"));
            var result = sanitizer.normalizeKeywords(unsanitized);
            assertEquals(List.of("ai"), result);
        }
    }

    @Nested
    @DisplayName("containsSameKeywords")
    class ContainsSameKeywords {

        @Test
        @DisplayName("Returns true when keywords have different case and order")
        void containsSameKeywords_withDifferentCaseAndOrder_returnsTrue() {
            var s1 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI", "ML"), "en");
            var s2 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("ml", "ai"), "en");
            assertTrue(sanitizer.containsSameKeywords(s1, s2));
        }

        @Test
        @DisplayName("Returns true when keywords are in different order")
        void containsSameKeywords_withDifferentOrder_returnsTrue() {
            var s1 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("ai", "ml"), "en");
            var s2 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("ml", "ai"), "en");
            assertTrue(sanitizer.containsSameKeywords(s1, s2));
        }

        @Test
        @DisplayName("Returns true when language is the same but with different case")
        void usesSameLanguage_withDifferentCase_returnsTrue() {
            var s1 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), "EN");
            var s2 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), "en");
            assertTrue(sanitizer.usesSameLanguage(s1, s2));
        }
    }

    @Nested
    @DisplayName("usesSameLanguage")
    class UsesSameLanguage {

        @Test
        @DisplayName("Returns false when languages differ")
        void usesSameLanguage_withDifferentLanguages_returnsFalse() {
            var s1 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), "en");
            var s2 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), "sv");
            assertFalse(sanitizer.usesSameLanguage(s1, s2));
        }

        @Test
        @DisplayName("Returns false when one language is null")
        void usesSameLanguage_withOneNull_returnsFalse() {
            var s1 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), null);
            var s2 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), "en");
            assertFalse(sanitizer.usesSameLanguage(s1, s2));
        }

        @Test
        @DisplayName("Returns true when both languages are null")
        void usesSameLanguage_withBothNull_returnsTrue() {
            var s1 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), null);
            var s2 = SubscriptionTestFactory.subscriptionWithKeywords(List.of("AI"), null);
            assertTrue(sanitizer.usesSameLanguage(s1, s2));
        }
    }

    @Nested
    @DisplayName("special characters")
    class SpecialCharacters {

        @Test
        @DisplayName("Preserves plus/hash and lowercases consistently (C++, C#)")
        void normalize_preservesPlusAndHash() {
            var input = new ArrayList<>(List.of("C++", "C#", "c++"));
            var result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("c++", "c#"), result);
        }

        @Test
        @DisplayName("Preserves accents/diacritics (Göteborg, naïve-bayes)")
        void normalize_preservesAccentsAndDiacritics() {
            var input = new ArrayList<>(List.of("Göteborg", "naïve-bayes"));
            var result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("göteborg", "naïve-bayes"), result);
        }

        @Test
        @DisplayName("Preserves punctuation and symbols (?, -, _)")
        void normalize_preservesPunctuationAndSymbols() {
            var input = new ArrayList<>(List.of("elon-musk?", "_quant", "  A/B  "));
            var result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("elon-musk?", "_quant", "a/b"), result);
        }

        @Test
        @DisplayName("Preserves emojis")
        void normalize_preservesEmojis() {
            var input = new ArrayList<>(List.of("AI 🤖", "🚀 SpaceX"));
            var result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("ai 🤖", "🚀 spacex"), result);
        }

        @Test
        @DisplayName("Trims whitespace around tokens but keeps inner symbols")
        void normalize_trimsWhitespaceButKeepsInnerSymbols() {
            var input = new ArrayList<>(List.of("  C#  ", "  Node.js "));
            var result = sanitizer.normalizeKeywords(input);
            assertEquals(List.of("c#", "node.js"), result);
        }

        @Nested
        @DisplayName("usesSameSchedule")
        class UsesSameSchedule {

            @Test
            @DisplayName("Returns true when both subscriptions use the same schedule preset")
            void usesSameSchedule_withSamePreset_returnsTrue() {
                var s1 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING_EVENING);
                var s2 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING_EVENING);
                assertTrue(sanitizer.usesSameSchedule(s1, s2));
            }

            @Test
            @DisplayName("Returns false when schedules differ")
            void usesSameSchedule_withDifferentPresets_returnsFalse() {
                var s1 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING);
                var s2 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.EVENING);
                assertFalse(sanitizer.usesSameSchedule(s1, s2));
            }

            @Test
            @DisplayName("Returns false when one schedule is null")
            void usesSameSchedule_withOneNull_returnsFalse() {
                var s1 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", null);
                var s2 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", SchedulePreset.MORNING);
                assertFalse(sanitizer.usesSameSchedule(s1, s2));
            }

            @Test
            @DisplayName("Returns true when both schedules are null")
            void usesSameSchedule_withBothNull_returnsTrue() {
                var s1 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", null);
                var s2 = SubscriptionTestFactory.subscriptionWithSchedule(List.of("AI"), "en", null);
                assertTrue(sanitizer.usesSameSchedule(s1, s2));
            }
        }
    }
}
