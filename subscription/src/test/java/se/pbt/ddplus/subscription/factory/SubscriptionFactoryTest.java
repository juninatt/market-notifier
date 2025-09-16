package se.pbt.ddplus.subscription.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.ddplus.core.schedule.SchedulePreset;
import se.pbt.ddplus.core.subscription.SubscribeCommand;
import se.pbt.ddplus.subscription.model.Subscription;
import se.pbt.ddplus.subscription.model.SubscriptionFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscriptionFactory")
class SubscriptionFactoryTest {

    private static final long DEFAULT_CHAT_ID = 123L;
    private static final String DEFAULT_LANGUAGE = "en";
    private static final int DEFAULT_MAX_ITEMS = 5;
    private static final List<String> IGNORED_KEYWORD_LIST = List.of("Ignored");
    private static final SchedulePreset DEFAULT_SCHEDULE = SchedulePreset.MORNING;

    SubscribeCommand defaultCommand;

    private final SubscriptionFactory factory = new SubscriptionFactory();

    @BeforeEach
    void setup() {
        defaultCommand = new SubscribeCommand(
                DEFAULT_CHAT_ID,
                DEFAULT_LANGUAGE,
                DEFAULT_MAX_ITEMS,
                IGNORED_KEYWORD_LIST,
                DEFAULT_SCHEDULE
        );
    }


    @Nested
    @DisplayName("Happy path:")
    class HappyPath {

        @Test
        @DisplayName("Builds a subscription with defaults and provided values")
        void buildsSubscriptionWithDefaults() {
            List<String> normalizedKeywords = List.of("ai", "ml");

            Subscription subscription = factory.from(defaultCommand, normalizedKeywords);

            assertEquals(DEFAULT_CHAT_ID, subscription.getChatId());
            assertEquals(SchedulePreset.MORNING, subscription.getSchedule());
            assertEquals(DEFAULT_MAX_ITEMS, subscription.getMaxItems());
            assertTrue(subscription.isEnabled());

            SubscriptionFilter filter = subscription.getFilter();
            assertNotNull(filter);
            assertEquals(List.of("ai", "ml"), filter.getKeywords());
            assertEquals(List.of(), filter.getTickers());
            assertEquals("en", filter.getLanguage());

            TimeZone timeZone = subscription.getTimezone();
            assertNotNull(timeZone);
            assertEquals("Europe/Stockholm", timeZone.getID());
        }

        @Test
        @DisplayName("Applies default schedule when schedule is null")
        void appliesDefaultScheduleWhenNull() {
            SubscribeCommand command = createCommandWithSchedule(null);
            Subscription subscription = factory.from(command, List.of("ai"));

            assertEquals(SchedulePreset.MORNING_EVENING, subscription.getSchedule());
        }


        @Test
        @DisplayName("Preserves order of normalized keywords")
        void preservesKeywordsOrder() {
            List<String> normalizedKeywords = List.of("first", "second", "third");
            Subscription subscription = factory.from(defaultCommand, normalizedKeywords);

            assertEquals(List.of("first", "second", "third"), subscription.getFilter().getKeywords());
        }
    }

    @Nested
    @DisplayName("Validation:")
    class Validation {

        @Test
        @DisplayName("Throws when command is null")
        void throwsWhenCommandIsNull() {
            assertThrows(NullPointerException.class,
                    () -> factory.from(null, List.of("ai")));
        }

        @Test
        @DisplayName("Throws when normalized keywords is null")
        void throwsWhenKeywordsIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> factory.from(defaultCommand, null));
        }

        @Test
        @DisplayName("Throws when normalized keywords is empty")
        void throwsWhenKeywordsIsEmpty() {
            assertThrows(IllegalArgumentException.class,
                    () -> factory.from(defaultCommand, List.of()));
        }

        @Test
        @DisplayName("Throws when first normalized keyword is null")
        void throwsWhenFirstKeywordIsNull() {
            ArrayList<String> normalized = new ArrayList<>();
            normalized.add(null);

            assertThrows(IllegalArgumentException.class,
                    () -> factory.from(defaultCommand, normalized));
        }

        @Test
        @DisplayName("Throws when first normalized keyword is blank")
        void throwsWhenFirstKeywordIsBlank() {
            assertThrows(IllegalArgumentException.class,
                    () -> factory.from(defaultCommand, List.of("   ", "ai")));
        }

        @Test
        @DisplayName("Throws when first normalized keyword is only whitespace chars")
        void throwsWhenFirstKeywordIsOnlyWhitespaceChars() {
            List<String> normalized = List.of("\t  \n  ");

            assertThrows(IllegalArgumentException.class,
                    () -> factory.from(defaultCommand, normalized));
        }
    }

    @Nested
    @DisplayName("Immutability & defensive copies:")
    class Immutability {

        @Test
        @DisplayName("Keywords list in Subscription is unmodifiable")
        void keywordsAreUnmodifiable() {
            Subscription subscription = factory.from(defaultCommand, List.of("ai"));

            assertThrows(UnsupportedOperationException.class,
                    () -> subscription.getFilter().getKeywords().add("X"));
        }

        @Test
        @DisplayName("Tickers list in Subscription is unmodifiable")
        void tickersAreUnmodifiable() {
            Subscription subscription = factory.from(defaultCommand, List.of("ai"));

            assertThrows(UnsupportedOperationException.class,
                    () -> subscription.getFilter().getTickers().add("TSLA"));
        }

        @Test
        @DisplayName("Filter is non-null and unique per subscription")
        void filterObjectIsNonNullAndOwned() {
            Subscription sub1 = factory.from(defaultCommand, List.of("ai"));
            Subscription sub2 = factory.from(defaultCommand, List.of("ai"));

            assertNotNull(sub1.getFilter());
            assertNotNull(sub2.getFilter());
            assertNotSame(sub1.getFilter(), sub2.getFilter(), "Each subscription should own its filter instance");
        }
    }

    @Nested
    @DisplayName("Pass-through & consistency:")
    class PassThroughConsistency {

        @Test
        @DisplayName("Enabled is set to true by default")
        void enabledDefaultsToTrue() {
            Subscription subscription = factory.from(defaultCommand, List.of("ai"));

            assertTrue(subscription.isEnabled());
        }

        @Test
        @DisplayName("Timezone defaults to Europe/Stockholm")
        void timezoneDefaultsToStockholm() {
            Subscription subscription = factory.from(defaultCommand, List.of("ai"));

            assertEquals("Europe/Stockholm", subscription.getTimezone().getID());
        }

        @Test
        @DisplayName("Trims whitespace around language string")
        void trimsLanguageString() {
            SubscribeCommand command = createCommandWithLanguage("   sv   ");

            Subscription subscription = factory.from(command, List.of("ai"));

            assertEquals("sv", subscription.getFilter().getLanguage());
        }

        @Test
        @DisplayName("Uses normalizedKeywords instead of command.keywords()")
        void usesNormalizedKeywordsInsteadOfCommandKeywords() {
            List<String> normalized = List.of("right1", "right2");
            Subscription subscription = factory.from(defaultCommand, normalized);

            assertEquals(List.of("right1", "right2"), subscription.getFilter().getKeywords());
        }

        @Test
        @DisplayName("Preserves emojis in normalized keywords")
        void preservesEmojisInKeywords() {
            List<String> normalizedKeywords = List.of("🚀", "ai🤖", "stocks📈");

            Subscription subscription = factory.from(defaultCommand, normalizedKeywords);

            assertEquals(List.of("🚀", "ai🤖", "stocks📈"), subscription.getFilter().getKeywords());
        }
    }

    // Helpers

    SubscribeCommand createCommandWithLanguage(String language) {
        return new SubscribeCommand(
                DEFAULT_CHAT_ID,
                language,
                DEFAULT_MAX_ITEMS,
                IGNORED_KEYWORD_LIST,
                DEFAULT_SCHEDULE
        );
    }

    SubscribeCommand createCommandWithSchedule(SchedulePreset schedule) {
        return new SubscribeCommand(
                DEFAULT_CHAT_ID,
                DEFAULT_LANGUAGE,
                DEFAULT_MAX_ITEMS,
                IGNORED_KEYWORD_LIST,
                schedule
        );
    }
}
