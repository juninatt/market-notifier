package se.pbt.ddplus.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Loads subscriptions from a YAML file located in the resources' directory.
 * By default, it reads from "subscriptions.yml", but other paths can be specified.
 */
public class SubscriptionLoader {

    // TODO: Move to a shared constants file
    private static final String DEFAULT_FILE = "subscriptions.yml";

    /**
     * Loads subscriptions from the default YAML file.
     */
    public List<Subscription> loadSubscriptions() {
        return loadSubscriptions(DEFAULT_FILE);
    }

    /**
     * Loads subscriptions from a specified YAML file.
     */
    public List<Subscription> loadSubscriptions(String resourcePath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                System.err.println("File not found: " + resourcePath);
                return Collections.emptyList();
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            SubscriptionListWrapper wrapper = mapper.readValue(input, SubscriptionListWrapper.class);
            return wrapper.getSubscriptions();
        } catch (Exception e) {
            System.err.println("Failed to load subscriptions: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
