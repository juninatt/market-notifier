package se.pbt.mn.subscription.persistence;

import tools.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionListWrapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles loading and saving subscriptions to and from a YAML file.
 * Used as simple file-based storage for the user's subscriptions.
 */
@Component
public class SubscriptionStorage {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionStorage.class);

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
                log.debug("No subscriptions file found at '{}' yet -- treating as empty", path);
                return new ArrayList<>();
            }

            YAMLMapper mapper = new YAMLMapper();
            SubscriptionListWrapper wrapper = mapper.readValue(input, SubscriptionListWrapper.class);
            return wrapper.getSubscriptions();
        } catch (Exception e) {
            log.warn("Failed to load subscriptions from '{}': {}", path, e.getMessage());
            return new ArrayList<>();
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
            YAMLMapper mapper = new YAMLMapper();
            SubscriptionListWrapper wrapper = new SubscriptionListWrapper();
            wrapper.setSubscriptions(subscriptions);

            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            Files.writeString(Paths.get(filePath), mapper.writeValueAsString(wrapper));

        } catch (IOException e) {
            log.warn("Failed to save subscriptions to '{}': {}", filePath, e.getMessage());
        }
    }
}
