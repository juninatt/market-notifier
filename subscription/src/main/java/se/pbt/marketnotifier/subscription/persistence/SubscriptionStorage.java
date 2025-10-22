package se.pbt.marketnotifier.subscription.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.model.SubscriptionListWrapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Handles loading and saving subscriptions to and from a YAML file.
 * Used as simple file-based storage for the user's subscriptions.
 */
@Component
public class SubscriptionStorage {

    // TODO: Move to a shared constants file if reused
    private static final String DEFAULT_FILE = "subscriptions.yml";

    /**
     * Loads subscriptions from the default YAML file.
     */
    public List<Subscription> loadSubscriptions() {
        return loadSubscriptions(DEFAULT_FILE);
    }

    /**
     * Loads subscriptions from a YAML file or classpath resource.
     * <p>
     * Returns an empty list if the file is missing or cannot be parsed.
     */
    public List<Subscription> loadSubscriptions(String path) {
        try (InputStream input = tryLoadInputStream(path)) {
            if (input == null) {
                System.err.println("File not found: " + path);
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

    /**
     * Attempts to open an {@link InputStream} for the given path.
     * <p>
     * First checks the file system; if no file is found, falls back to
     * loading from the classpath resources.
     */
    private InputStream tryLoadInputStream(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return new FileInputStream(file);
        } else {
            return getClass().getClassLoader().getResourceAsStream(path); // fallback
        }
    }


    /**
     * Saves a list of subscriptions to the default YAML file.
     * Overwrites any existing content.
     */
    public void saveSubscriptions(List<Subscription> subscriptions) {
        saveSubscriptions(subscriptions, DEFAULT_FILE);
    }

    /**
     * Saves subscriptions to the given file path.
     */
    public void saveSubscriptions(List<Subscription> subscriptions, String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            SubscriptionListWrapper wrapper = new SubscriptionListWrapper();
            wrapper.setSubscriptions(subscriptions);

            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            Files.writeString(Paths.get(filePath), mapper.writeValueAsString(wrapper));

        } catch (IOException e) {
            System.err.println("Failed to save subscriptions: " + e.getMessage());
        }
    }
}
