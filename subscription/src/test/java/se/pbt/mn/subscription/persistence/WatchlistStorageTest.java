package se.pbt.mn.subscription.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WatchlistStorage")
class WatchlistStorageTest {

    private static final String TEST_WATCHLIST_FILE_PATH = "src/test/resources/test-watchlist.yml";

    private final WatchlistStorage storage = new WatchlistStorage();

    @Nested
    @DisplayName("Load operations")
    class LoadOperation {

        @Test
        @DisplayName("Loads a watchlist from YAML file and maps fields correctly")
        void load_fromValidYaml_mapsFieldsCorrectly() {
            var result = storage.loadWatchlist(TEST_WATCHLIST_FILE_PATH);

            assertTrue(result.isPresent());
            var watchlist = result.get();
            assertEquals("you@example.com", watchlist.getEmail());
            assertEquals(List.of("Tesla", "AAPL"), watchlist.getCompanies());
            assertEquals(List.of("AI", "Space"), watchlist.getCategories());
        }

        @Test
        @DisplayName("Returns empty result when file does not exist")
        void load_whenFileMissing_returnsEmpty() {
            var result = storage.loadWatchlist("nonexistent.yml");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty result when YAML is invalid")
        void load_withInvalidYaml_returnsEmpty() {
            var result = storage.loadWatchlist("src/test/resources/invalid-watchlist.yml");
            assertTrue(result.isEmpty());
        }
    }
}
