package se.pbt.ddplus.subscription.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.ddplus.subscription.model.Subscription;
import se.pbt.ddplus.subscription.model.SubscriptionFilter;
import se.pbt.ddplus.subscription.persestence.SubscriptionStorage;

import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionStorageTest {

    private static final String TEST_SUBSCRIPTION_FILE_PATH = "src/test/resources/test-subscriptions.yml";

    @Test
    @DisplayName("Saves subscriptions to YAML file without throwing")
    void savesSubscriptionsToYamlSuccessfully() {
        SubscriptionStorage storage = new SubscriptionStorage();

        List<Subscription> subs = SampleSubscriptions.twoSubscriptions();

        assertDoesNotThrow(() -> storage.saveSubscriptions(subs, TEST_SUBSCRIPTION_FILE_PATH));
    }

    @Test
    @DisplayName("Loads subscriptions from YAML file and maps fields correctly")
    void loadsSubscriptionsCorrectlyFromYamlFile() {
        SubscriptionStorage storage = new SubscriptionStorage();
        List<Subscription> subscriptions = storage.loadSubscriptions(TEST_SUBSCRIPTION_FILE_PATH);

        assertNotNull(subscriptions, "Should not return null");
        assertEquals(2, subscriptions.size(), "Should load two subscriptions");

        Subscription first = subscriptions.get(0);
        assertEquals("ai-alerts", first.getId());
        assertEquals(123456789L, first.getChatId(), "chatId should be mapped");
        assertEquals("Europe/Stockholm", first.getTimezone().getID());
        assertTrue(first.isEnabled());
        assertEquals(List.of("AI", "machine learning"), first.getFilter().getKeywords());
    }

    /**
     * Private fixture builder for sample subscriptions used in tests.
     * Matches the provided YAML structure exactly.
     */
    private static final class SampleSubscriptions {

        private static List<Subscription> twoSubscriptions() {
            Subscription aiAlerts = new Subscription();
            aiAlerts.setId("ai-alerts");
            aiAlerts.setChatId(123456789L);
            aiAlerts.setSchedule("0 0 * * * *"); // Every hour
            aiAlerts.setTimezone(TimeZone.getTimeZone("Europe/Stockholm"));
            aiAlerts.setMaxItems(5);
            aiAlerts.setPriority(1);
            aiAlerts.setEnabled(true);

            SubscriptionFilter aiFilter = new SubscriptionFilter();
            aiFilter.setKeywords(List.of("AI", "machine learning"));
            aiFilter.setTickers(List.of());
            aiFilter.setLanguage("en");
            aiAlerts.setFilter(aiFilter);

            Subscription greenNews = new Subscription();
            greenNews.setId("green-news");
            greenNews.setSchedule("0 30 * * * *"); // Every hour at 30 minutes past
            greenNews.setTimezone(TimeZone.getTimeZone("Europe/Stockholm"));
            greenNews.setMaxItems(5);
            greenNews.setPriority(2);
            greenNews.setEnabled(true);

            SubscriptionFilter greenFilter = new SubscriptionFilter();
            greenFilter.setKeywords(List.of("solar", "wind"));
            greenFilter.setTickers(List.of());
            greenFilter.setLanguage("en");
            greenNews.setFilter(greenFilter);

            return List.of(aiAlerts, greenNews);
        }

        private SampleSubscriptions() {}
    }
}
