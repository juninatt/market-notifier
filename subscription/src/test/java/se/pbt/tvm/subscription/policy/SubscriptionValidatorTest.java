package se.pbt.tvm.subscription.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.tvm.subscription.testutil.SubscriptionTestFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SubscriptionValidator")
class SubscriptionValidatorTest {

    private final SubscriptionSanitizer sanitizer = mock(SubscriptionSanitizer.class);
    private final SubscriptionValidator validator = new SubscriptionValidator(sanitizer);

    @Nested
    @DisplayName("Structural validation")
    class StructuralValidation {

        @Test
        @DisplayName("Returns error when subscription is null")
        void validate_withNullSubscription_returnsError() {
            var result = validator.validate(null, List.of());
            assertTrue(result.isPresent());
            assertEquals("Subscription cannot be null.", result.get());
        }

        @Test
        @DisplayName("Returns error when filter is missing")
        void validate_withMissingFilter_returnsError() {
            var sub = SubscriptionTestFactory.subscriptionWithIdOnly("sub-1");
            var result = validator.validate(sub, List.of());
            assertTrue(result.isPresent());
            assertEquals("Subscription filter cannot be null.", result.get());
        }

        @Test
        @DisplayName("Returns error when keywords list is empty")
        void validate_withEmptyKeywords_returnsError() {
            var filter = SubscriptionTestFactory.filter(List.of(), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-2", filter, true);
            var result = validator.validate(sub, List.of());
            assertTrue(result.isPresent());
            assertEquals("At least one keyword must be specified.", result.get());
        }

        @Test
        @DisplayName("Returns error when language is missing")
        void validate_withMissingLanguage_returnsError() {
            var filter = SubscriptionTestFactory.filter(List.of("Stock"), List.of("AAPL"), null);
            var sub = SubscriptionTestFactory.subscription("sub-3", filter, true);
            var result = validator.validate(sub, List.of());
            assertTrue(result.isPresent());
            assertEquals("Language must be specified.", result.get());
        }
    }

    @Nested
    @DisplayName("Duplicate detection")
    class DuplicateDetection {

        @Test
        @DisplayName("Returns error when an identical subscription already exists")
        void validate_withDuplicateSubscription_returnsError() {
            var existingFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("TSLA"), "en");
            var existing = SubscriptionTestFactory.subscription("sub-1", existingFilter, true);
            existing.setChatId(100);

            var newFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("AAPL"), "en");
            var candidate = SubscriptionTestFactory.subscription("sub-2", newFilter, true);
            candidate.setChatId(100);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(true);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(true);

            var result = validator.validate(candidate, List.of(existing));

            assertTrue(result.isPresent());
            assertEquals(
                    "A subscription with the same keywords and language already exists for this chat.",
                    result.get()
            );
        }

        @Test
        @DisplayName("Returns empty result when subscription is unique")
        void validate_withUniqueSubscription_returnsEmptyResult() {
            var existingFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("TSLA"), "en");
            var existing = SubscriptionTestFactory.subscription("sub-1", existingFilter, true);
            existing.setChatId(200);

            var candidateFilter = SubscriptionTestFactory.filter(List.of("Economy"), List.of("SP500"), "sv");
            var candidate = SubscriptionTestFactory.subscription("sub-2", candidateFilter, true);
            candidate.setChatId(200);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(false);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(false);

            var result = validator.validate(candidate, List.of(existing));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Handles null list of existing subscriptions safely")
        void validate_withNullExistingList_returnsEmptyResult() {
            var filter = SubscriptionTestFactory.filter(List.of("Finance"), List.of("SP500"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-3", filter, true);
            var result = validator.validate(sub, null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Handles empty list of existing subscriptions safely")
        void validate_withEmptyExistingList_returnsEmptyResult() {
            var filter = SubscriptionTestFactory.filter(List.of("Tech"), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-4", filter, true);
            var result = validator.validate(sub, new ArrayList<>());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Treats subscriptions with same keywords in different case as duplicates")
        void validate_withDifferentCaseKeywords_detectsDuplicate() {
            var existingFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("TSLA"), "EN");
            var existing = SubscriptionTestFactory.subscription("sub-1", existingFilter, true);
            existing.setChatId(1);

            var candidateFilter = SubscriptionTestFactory.filter(List.of("ai"), List.of("AAPL"), "en");
            var candidate = SubscriptionTestFactory.subscription("sub-2", candidateFilter, true);
            candidate.setChatId(1);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(true);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(true);

            var result = validator.validate(candidate, List.of(existing));

            assertTrue(result.isPresent());
            assertEquals(
                    "A subscription with the same keywords and language already exists for this chat.",
                    result.get()
            );
        }

        @Test
        @DisplayName("Detects duplicate even when multiple existing subscriptions are present")
        void validate_withMultipleExisting_detectsDuplicate() {
            var s1 = SubscriptionTestFactory.subscription("sub-1",
                    SubscriptionTestFactory.filter(List.of("Stock"), List.of("AAPL"), "en"), true);
            s1.setChatId(10);

            var s2 = SubscriptionTestFactory.subscription("sub-2",
                    SubscriptionTestFactory.filter(List.of("Crypto"), List.of("BTC"), "en"), true);
            s2.setChatId(10);

            var candidate = SubscriptionTestFactory.subscription("sub-3",
                    SubscriptionTestFactory.filter(List.of("Crypto"), List.of("ETH"), "en"), true);
            candidate.setChatId(10);

            when(sanitizer.usesSameLanguage(s2, candidate)).thenReturn(true);
            when(sanitizer.containsSameKeywords(s2, candidate)).thenReturn(true);

            var result = validator.validate(candidate, List.of(s1, s2));
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Allows same keywords if language differs")
        void validate_withSameKeywordsDifferentLanguage_returnsEmptyResult() {
            var existing = SubscriptionTestFactory.subscription("sub-1",
                    SubscriptionTestFactory.filter(List.of("Market"), List.of("AAPL"), "en"), true);
            existing.setChatId(42);

            var candidate = SubscriptionTestFactory.subscription("sub-2",
                    SubscriptionTestFactory.filter(List.of("Market"), List.of("AAPL"), "sv"), true);
            candidate.setChatId(42);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(false);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(true);

            var result = validator.validate(candidate, List.of(existing));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Propagates sanitizer exception if it occurs during validation")
        void validate_whenSanitizerThrows_propagatesException() {
            var filter = SubscriptionTestFactory.filter(List.of("Tech"), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-1", filter, true);
            sub.setChatId(1);

            when(sanitizer.usesSameLanguage(any(), any()))
                    .thenThrow(new RuntimeException("Sanitizer failed"));

            assertThrows(RuntimeException.class, () -> validator.validate(sub, List.of(sub)));
        }
    }
}
