package se.pbt.ddplus.subscription;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionLoaderTest {

    private static final String TEST_SUBSCRIPTIONS_FILE = "test-subscriptions.yml";

    @Test
    void testLoadSubscriptionsFromTestFile() {
        SubscriptionLoader loader = new SubscriptionLoader();
        List<Subscription> subscriptions = loader.loadSubscriptions(TEST_SUBSCRIPTIONS_FILE);

        assertNotNull(subscriptions, "Subscriptions list should not be null");
        assertEquals(2, subscriptions.size(), "Should load two subscriptions");

        Subscription first = subscriptions.get(0);
        assertEquals("ai-alerts", first.getId());
        assertEquals("finnhub", first.getProvider());
        assertTrue(first.getKeywords().contains("AI"));
    }
}
