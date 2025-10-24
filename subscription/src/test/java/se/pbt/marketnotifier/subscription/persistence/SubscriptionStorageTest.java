package se.pbt.marketnotifier.subscription.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.core.subscription.SchedulePreset;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.model.SubscriptionFilter;
import se.pbt.marketnotifier.subscription.testutil.SubscriptionTestFactory;

import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscriptionStorage")
class SubscriptionStorageTest {

    private static final String TEST_SUBSCRIPTION_FILE_PATH = "src/test/resources/test-subscriptions.yml";

    @Nested
    @DisplayName("Save operations")
    class SaveOperation {

        @Test
        @DisplayName("Saves subscriptions to YAML file without throwing")
        void save_withValidSubscriptions_succeeds() {
            var storage = new SubscriptionStorage();
            var subs = SampleSubscriptions.twoSubscriptions();
            assertDoesNotThrow(() -> storage.saveSubscriptions(subs, TEST_SUBSCRIPTION_FILE_PATH));
        }

        @Test
        @DisplayName("Saves subscriptions to default file without throwing")
        void save_withDefaultFile_succeeds() {
            var storage = new SubscriptionStorage();
            var subs = SubscriptionTestFactory.subscriptionList(2);
            assertDoesNotThrow(() -> storage.saveSubscriptions(subs));
        }

        @Test
        @DisplayName("Handles invalid path gracefully when saving")
        void save_withInvalidPath_doesNotThrow() {
            var storage = new SubscriptionStorage();
            var subs = SubscriptionTestFactory.subscriptionList(2);
            assertDoesNotThrow(() -> storage.saveSubscriptions(subs, "Z:/invalid/folder/test.yml"));
        }

        @Test
        @DisplayName("Handles empty list gracefully when saving")
        void save_withEmptyList_doesNotThrow() {
            var storage = new SubscriptionStorage();
            assertDoesNotThrow(() -> storage.saveSubscriptions(List.of(), TEST_SUBSCRIPTION_FILE_PATH));
        }
    }

    @Nested
    @DisplayName("Load operations")
    class LoadOperation {

        @Test
        @DisplayName("Loads subscriptions from YAML file and maps fields correctly")
        void load_fromValidYaml_mapsFieldsCorrectly() {
            var storage = new SubscriptionStorage();
            var subscriptions = storage.loadSubscriptions(TEST_SUBSCRIPTION_FILE_PATH);

            assertNotNull(subscriptions, "Should not return null");
            assertEquals(2, subscriptions.size(), "Should load two subscriptions");

            var first = subscriptions.get(0);
            assertEquals("ai-alerts", first.getId());
            assertEquals(123456789L, first.getChatId());
            assertEquals("Europe/Stockholm", first.getTimezone().getID());
            assertTrue(first.isEnabled());
            assertEquals(List.of("AI", "machine learning"), first.getFilter().getKeywords());
        }

        @Test
        @DisplayName("Returns empty list when file does not exist")
        void load_whenFileMissing_returnsEmptyList() {
            var storage = new SubscriptionStorage();
            var result = storage.loadSubscriptions("nonexistent.yml");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when YAML is invalid")
        void load_withInvalidYaml_returnsEmptyList() {
            var storage = new SubscriptionStorage();
            var result = storage.loadSubscriptions("src/test/resources/invalid-subscriptions.yml");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    /**
     * Used only for testing actual YAML mapping.
     * Must match the file structure exactly.
     */
    private static final class SampleSubscriptions {

        private static List<Subscription> twoSubscriptions() {
            var aiAlerts = new Subscription();
            aiAlerts.setId("ai-alerts");
            aiAlerts.setChatId(123456789L);
            aiAlerts.setSchedule(SchedulePreset.MORNING);
            aiAlerts.setTimezone(TimeZone.getTimeZone("Europe/Stockholm"));
            aiAlerts.setMaxItems(5);
            aiAlerts.setEnabled(true);

            var aiFilter = new SubscriptionFilter();
            aiFilter.setKeywords(List.of("AI", "machine learning"));
            aiFilter.setTickers(List.of());
            aiFilter.setLanguage("en");
            aiAlerts.setFilter(aiFilter);

            var greenNews = new Subscription();
            greenNews.setId("green-news");
            greenNews.setSchedule(SchedulePreset.MORNING_EVENING);
            greenNews.setTimezone(TimeZone.getTimeZone("Europe/Stockholm"));
            greenNews.setMaxItems(5);
            greenNews.setEnabled(true);

            var greenFilter = new SubscriptionFilter();
            greenFilter.setKeywords(List.of("solar", "wind"));
            greenFilter.setTickers(List.of());
            greenFilter.setLanguage("en");
            greenNews.setFilter(greenFilter);

            return List.of(aiAlerts, greenNews);
        }

        private SampleSubscriptions() {}
    }
}
