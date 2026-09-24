package se.pbt.mn.subscription.persistence;

import tools.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.pbt.mn.subscription.model.Watchlist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Loads a user-maintained {@link Watchlist} from a YAML file.
 * <p>
 * Read-only, unlike {@link SubscriptionStorage} -- the watchlist is meant to be hand-edited
 * directly, not managed through the application.
 */
@Component
public class WatchlistStorage {

    private static final Logger log = LoggerFactory.getLogger(WatchlistStorage.class);

    /**
     * Loads the watchlist from a YAML file or classpath resource.
     * <p>
     * Returns an empty {@link Optional} if no file exists at the path yet, or if its
     * content cannot be parsed -- a missing or malformed watchlist should never prevent
     * the application from starting.
     */
    public Optional<Watchlist> loadWatchlist(String path) {
        try (InputStream input = tryLoadInputStream(path)) {
            if (input == null) {
                log.debug("No watchlist file found at '{}' -- skipping the startup digest", path);
                return Optional.empty();
            }

            YAMLMapper mapper = new YAMLMapper();
            return Optional.ofNullable(mapper.readValue(input, Watchlist.class));
        } catch (Exception e) {
            log.warn("Failed to load watchlist from '{}': {}", path, e.getMessage());
            return Optional.empty();
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
}
