package se.pbt.marketnotifier.subscription.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.testutil.SubscriptionTestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SubscriptionValidator:")
class SubscriptionValidatorTest {

    private final SubscriptionSanitizer sanitizer = mock(SubscriptionSanitizer.class);
    private final SubscriptionValidator validator = new SubscriptionValidator(sanitizer);

    @Nested
    @DisplayName("Structural validation:")
    class StructuralValidation {

        @Test
        @DisplayName("Returns error when subscription is null")
        void returns_error_when_subscription_is_null() {
            Optional<String> result = validator.validate(null, List.of());
            assertTrue(result.isPresent());
            assertEquals("Subscription cannot be null.", result.get());
        }

        @Test
        @DisplayName("Returns error when filter is missing")
        void returns_error_when_filter_is_missing() {
            Subscription sub = SubscriptionTestFactory.subscriptionWithIdOnly("sub-1");

            Optional<String> result = validator.validate(sub, List.of());

            assertTrue(result.isPresent());
            assertEquals("Subscription filter cannot be null.", result.get());
        }

        @Test
        @DisplayName("Returns error when keywords list is empty")
        void returns_error_when_keywords_are_empty() {
            var filter = SubscriptionTestFactory.filter(List.of(), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-2", filter, true);

            Optional<String> result = validator.validate(sub, List.of());

            assertTrue(result.isPresent());
            assertEquals("At least one keyword must be specified.", result.get());
        }

        @Test
        @DisplayName("Returns error when language is missing")
        void returns_error_when_language_is_missing() {
            var filter = SubscriptionTestFactory.filter(List.of("Stock"), List.of("AAPL"), null);
            var sub = SubscriptionTestFactory.subscription("sub-3", filter, true);

            Optional<String> result = validator.validate(sub, List.of());

            assertTrue(result.isPresent());
            assertEquals("Language must be specified.", result.get());
        }
    }

    @Nested
    @DisplayName("Duplicate detection:")
    class DuplicateDetection {

        @Test
        @DisplayName("Returns error when an identical subscription already exists")
        void returns_error_when_duplicate_exists() {
            var existingFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("TSLA"), "en");
            var existing = SubscriptionTestFactory.subscription("sub-1", existingFilter, true);
            existing.setChatId(100);

            var newFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("AAPL"), "en");
            var candidate = SubscriptionTestFactory.subscription("sub-2", newFilter, true);
            candidate.setChatId(100);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(true);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(true);

            Optional<String> result = validator.validate(candidate, List.of(existing));

            assertTrue(result.isPresent());
            assertEquals(
                    "A subscription with the same keywords and language already exists for this chat.",
                    result.get()
            );
        }

        @Test
        @DisplayName("Returns empty result when subscription is unique")
        void returns_empty_result_when_unique() {
            var existingFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("TSLA"), "en");
            var existing = SubscriptionTestFactory.subscription("sub-1", existingFilter, true);
            existing.setChatId(200);

            var candidateFilter = SubscriptionTestFactory.filter(List.of("Economy"), List.of("SP500"), "sv");
            var candidate = SubscriptionTestFactory.subscription("sub-2", candidateFilter, true);
            candidate.setChatId(200);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(false);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(false);

            Optional<String> result = validator.validate(candidate, List.of(existing));

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Handles null list of existing subscriptions safely")
        void handles_null_existing_list_safely() {
            var filter = SubscriptionTestFactory.filter(List.of("Finance"), List.of("SP500"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-3", filter, true);

            Optional<String> result = validator.validate(sub, null);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Handles empty list of existing subscriptions safely")
        void handles_empty_existing_list_safely() {
            var filter = SubscriptionTestFactory.filter(List.of("Tech"), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-4", filter, true);

            Optional<String> result = validator.validate(sub, new ArrayList<>());

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Treats subscriptions with same keywords in different case as duplicates")
        void treats_case_insensitive_keywords_as_duplicates() {
            var existingFilter = SubscriptionTestFactory.filter(List.of("AI"), List.of("TSLA"), "EN");
            var existing = SubscriptionTestFactory.subscription("sub-1", existingFilter, true);
            existing.setChatId(1);

            var candidateFilter = SubscriptionTestFactory.filter(List.of("ai"), List.of("AAPL"), "en");
            var candidate = SubscriptionTestFactory.subscription("sub-2", candidateFilter, true);
            candidate.setChatId(1);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(true);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(true);

            Optional<String> result = validator.validate(candidate, List.of(existing));

            assertTrue(result.isPresent());
            assertEquals(
                    "A subscription with the same keywords and language already exists for this chat.",
                    result.get()
            );
        }

        @Test
        @DisplayName("Detects duplicate even when multiple existing subscriptions are present")
        void detects_duplicate_among_multiple_existing_subscriptions() {
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

            Optional<String> result = validator.validate(candidate, List.of(s1, s2));

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Allows same keywords if language differs")
        void allows_same_keywords_if_language_differs() {
            var existing = SubscriptionTestFactory.subscription("sub-1",
                    SubscriptionTestFactory.filter(List.of("Market"), List.of("AAPL"), "en"), true);
            existing.setChatId(42);

            var candidate = SubscriptionTestFactory.subscription("sub-2",
                    SubscriptionTestFactory.filter(List.of("Market"), List.of("AAPL"), "sv"), true);
            candidate.setChatId(42);

            when(sanitizer.usesSameLanguage(existing, candidate)).thenReturn(false);
            when(sanitizer.containsSameKeywords(existing, candidate)).thenReturn(true);

            Optional<String> result = validator.validate(candidate, List.of(existing));

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Propagates sanitizer exception if it occurs during validation")
        void propagates_sanitizer_exception_if_it_occurs() {
            var filter = SubscriptionTestFactory.filter(List.of("Tech"), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-1", filter, true);
            sub.setChatId(1);

            when(sanitizer.usesSameLanguage(any(), any()))
                    .thenThrow(new RuntimeException("Sanitizer failed"));

            assertThrows(RuntimeException.class,
                    () -> validator.validate(sub, List.of(sub)));
        }
    }
}
