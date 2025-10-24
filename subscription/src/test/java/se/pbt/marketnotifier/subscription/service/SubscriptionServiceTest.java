package se.pbt.marketnotifier.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.subscription.format.SubscriptionFormatter;
import se.pbt.marketnotifier.subscription.persistence.SubscriptionStorage;
import se.pbt.marketnotifier.subscription.policy.SubscriptionIdGenerator;
import se.pbt.marketnotifier.subscription.policy.SubscriptionSanitizer;
import se.pbt.marketnotifier.subscription.policy.SubscriptionValidator;
import se.pbt.marketnotifier.subscription.testutil.SubscriptionTestFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    private SubscriptionStorage storage;
    private SubscriptionIdGenerator idGenerator;
    private SubscriptionValidator validator;
    private SubscriptionFormatter formatter;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        storage = mock(SubscriptionStorage.class);
        idGenerator = new SubscriptionIdGenerator();
        validator = new SubscriptionValidator(new SubscriptionSanitizer());
        formatter = new SubscriptionFormatter();

        service = new SubscriptionService(storage, idGenerator, validator, formatter);
    }

    @Nested
    @DisplayName("Save operations:")
    class SaveOperation {

        @Test
        @DisplayName("Saves a valid subscription successfully")
        void save_withValidSubscription_returnsSuccess() {
            var filter = SubscriptionTestFactory.filter(List.of("Tech"), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-1", filter, true);

            when(storage.loadSubscriptions(anyString())).thenReturn(new ArrayList<>());

            var result = service.save(sub, "subscriptions.yml");

            assertTrue(result.success());
            assertTrue(result.message().contains("Subscription created with id:"));
            verify(storage).saveSubscriptions(anyList(), anyString());
        }

        @Test
        @DisplayName("Fails when subscription is null")
        void save_withNullSubscription_returnsFailure() {
            var result = service.save(null, "subscriptions.yml");

            assertFalse(result.success());
            assertEquals("Subscription cannot be null.", result.message());
            verifyNoInteractions(storage);
        }

        @Test
        @DisplayName("Fails when validator reports invalid data")
        void save_withInvalidSubscription_returnsValidationError() {
            var invalid = SubscriptionTestFactory.subscription("x", null, true);
            when(storage.loadSubscriptions(anyString())).thenReturn(List.of());

            var result = service.save(invalid, "subscriptions.yml");

            assertFalse(result.success());
            assertTrue(result.message().contains("filter cannot be null"));
            verify(storage, never()).saveSubscriptions(anyList(), anyString());
        }

        @Test
        @DisplayName("Handles storage failure gracefully")
        void save_whenStorageThrows_returnsFailure() {
            var filter = SubscriptionTestFactory.filter(List.of("AI"), List.of("GOOG"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-2", filter, true);

            when(storage.loadSubscriptions(anyString())).thenThrow(new RuntimeException("Disk error"));

            var result = service.save(sub, "subscriptions.yml");

            assertFalse(result.success());
            assertTrue(result.message().contains("Failed to save subscription"));
        }
    }

    @Nested
    @DisplayName("Listing subscriptions:")
    class Listing {

        @Test
        @DisplayName("Returns formatted subscriptions for given chat ID")
        void list_withMatchingChatId_returnsFormattedResults() {
            var filter = SubscriptionTestFactory.filter(List.of("Finance"), List.of("AAPL"), "en");
            var sub1 = SubscriptionTestFactory.subscription("id-1", filter, true);
            sub1.setChatId(100);

            var sub2 = SubscriptionTestFactory.subscription("id-2", filter, false);
            sub2.setChatId(200);

            when(storage.loadSubscriptions(anyString())).thenReturn(List.of(sub1, sub2));

            var result = service.listByChatId(100);

            assertEquals(1, result.size());
            assertTrue(result.get(0).contains("ID: id-1"));
            assertTrue(result.get(0).contains("Finance"));
        }

        @Test
        @DisplayName("Returns empty list when no subscriptions exist")
        void list_withNoSubscriptions_returnsEmptyList() {
            when(storage.loadSubscriptions(anyString())).thenReturn(List.of());

            var result = service.listByChatId(123);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Throws runtime exception on storage error")
        void list_whenStorageFails_throwsRuntimeException() {
            when(storage.loadSubscriptions(anyString())).thenThrow(new RuntimeException("Corrupted file"));

            var ex = assertThrows(RuntimeException.class, () -> service.listByChatId(1));
            assertTrue(ex.getMessage().contains("Failed to list subscriptions"));
        }
    }

    @Nested
    @DisplayName("Remove operations:")
    class RemoveOperation {

        @Test
        @DisplayName("Removes subscription by matching ID")
        void remove_withMatchingId_removesSubscription() {
            var filter = SubscriptionTestFactory.filter(List.of("Tech"), List.of("TSLA"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-1", filter, true);
            sub.setChatId(1);

            when(storage.loadSubscriptions(anyString())).thenReturn(List.of(sub));

            boolean result = service.removeByIdOrKeyword(1, "sub-1");

            assertTrue(result);
            verify(storage).saveSubscriptions(anyList(), anyString());
        }

        @Test
        @DisplayName("Removes subscription by matching keyword (case-insensitive)")
        void remove_withMatchingKeyword_removesSubscription() {
            var filter = SubscriptionTestFactory.filter(List.of("Market"), List.of("MSFT"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-2", filter, true);
            sub.setChatId(99);

            when(storage.loadSubscriptions(anyString())).thenReturn(List.of(sub));

            boolean result = service.removeByIdOrKeyword(99, "market");

            assertTrue(result);
            verify(storage).saveSubscriptions(anyList(), anyString());
        }

        @Test
        @DisplayName("Does not remove when no matching ID or keyword")
        void remove_withNoMatch_returnsFalse() {
            var filter = SubscriptionTestFactory.filter(List.of("Stocks"), List.of("AAPL"), "en");
            var sub = SubscriptionTestFactory.subscription("sub-3", filter, true);
            sub.setChatId(5);

            when(storage.loadSubscriptions(anyString())).thenReturn(List.of(sub));

            boolean result = service.removeByIdOrKeyword(5, "Crypto");

            assertFalse(result);
            verify(storage, never()).saveSubscriptions(anyList(), anyString());
        }

        @Test
        @DisplayName("Returns false when argument is blank")
        void remove_withBlankArgument_returnsFalse() {
            boolean result = service.removeByIdOrKeyword(10, "  ");
            assertFalse(result);
            verifyNoInteractions(storage);
        }

        @Test
        @DisplayName("Throws runtime exception if storage fails")
        void remove_whenStorageFails_throwsRuntimeException() {
            when(storage.loadSubscriptions(anyString())).thenThrow(new RuntimeException("Read error"));

            assertThrows(RuntimeException.class, () -> service.removeByIdOrKeyword(1, "Tech"));
        }
    }
}
