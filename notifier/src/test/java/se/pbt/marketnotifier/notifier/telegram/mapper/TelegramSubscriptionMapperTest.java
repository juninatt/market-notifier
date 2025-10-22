package se.pbt.marketnotifier.notifier.telegram.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.core.subscription.SchedulePreset;
import se.pbt.marketnotifier.core.subscription.TelegramSubscribeCommand;
import se.pbt.marketnotifier.subscription.model.Subscription;
import se.pbt.marketnotifier.subscription.model.SubscriptionFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelegramSubscriptionMapper")
class TelegramSubscriptionMapperTest {

    private static final long DEFAULT_CHAT_ID = 123L;
    private static final String DEFAULT_LANGUAGE = "en";
    private static final int DEFAULT_MAX_ITEMS = 5;
    private static final List<String> IGNORED_KEYWORD_LIST = List.of("Ignored");
    private static final SchedulePreset DEFAULT_SCHEDULE = SchedulePreset.MORNING;

    private TelegramSubscribeCommand defaultCommand;
    private TelegramSubscriptionMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new TelegramSubscriptionMapper();
        defaultCommand = new TelegramSubscribeCommand(
                DEFAULT_CHAT_ID,
                DEFAULT_LANGUAGE,
                DEFAULT_MAX_ITEMS,
                IGNORED_KEYWORD_LIST,
                DEFAULT_SCHEDULE
        );
    }


    @Nested
    @DisplayName("Mapping behaviour:")
    class MappingBehaviour {

        @Test
        @DisplayName("Builds a subscription with correct values and defaults")
        void buildsSubscriptionCorrectly() {
            List<String> normalizedKeywords = List.of("ai", "ml");

            Subscription subscription = mapper.map(defaultCommand, normalizedKeywords);

            assertEquals(DEFAULT_CHAT_ID, subscription.getChatId());
            assertEquals(DEFAULT_SCHEDULE, subscription.getSchedule());
            assertEquals(DEFAULT_MAX_ITEMS, subscription.getMaxItems());
            assertTrue(subscription.isEnabled());

            SubscriptionFilter filter = subscription.getFilter();
            assertNotNull(filter);
            assertEquals(List.of("ai", "ml"), filter.getKeywords());
            assertEquals("en", filter.getLanguage());

            TimeZone timeZone = subscription.getTimezone();
            assertNotNull(timeZone);
            assertEquals("Europe/Stockholm", timeZone.getID());
        }

        @Test
        @DisplayName("Applies default schedule when schedule is null")
        void appliesDefaultScheduleWhenNull() {
            TelegramSubscribeCommand command = createCommandWithSchedule(null);
            Subscription subscription = mapper.map(command, List.of("ai"));

            assertEquals(SchedulePreset.MORNING_EVENING, subscription.getSchedule());
        }

        @Test
        @DisplayName("Preserves order of normalized keywords")
        void preservesKeywordOrder() {
            List<String> normalizedKeywords = List.of("first", "second", "third");
            Subscription subscription = mapper.map(defaultCommand, normalizedKeywords);

            assertEquals(List.of("first", "second", "third"), subscription.getFilter().getKeywords());
        }
    }


    @Nested
    @DisplayName("Input validation:")
    class InputValidation {

        @Test
        @DisplayName("Throws when command is null")
        void throwsWhenCommandIsNull() {
            assertThrows(NullPointerException.class,
                    () -> mapper.map(null, List.of("ai")));
        }

        @Test
        @DisplayName("Throws when normalized keywords is null")
        void throwsWhenKeywordsIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> mapper.map(defaultCommand, null));
        }

        @Test
        @DisplayName("Throws when normalized keywords is empty")
        void throwsWhenKeywordsIsEmpty() {
            assertThrows(IllegalArgumentException.class,
                    () -> mapper.map(defaultCommand, List.of()));
        }

        @Test
        @DisplayName("Throws when first normalized keyword is null")
        void throwsWhenFirstKeywordIsNull() {
            ArrayList<String> normalized = new ArrayList<>();
            normalized.add(null);

            assertThrows(IllegalArgumentException.class,
                    () -> mapper.map(defaultCommand, normalized));
        }

        @Test
        @DisplayName("Throws when first normalized keyword is blank")
        void throwsWhenFirstKeywordIsBlank() {
            assertThrows(IllegalArgumentException.class,
                    () -> mapper.map(defaultCommand, List.of("   ", "ai")));
        }

        @Test
        @DisplayName("Throws when first normalized keyword is only whitespace chars")
        void throwsWhenFirstKeywordIsOnlyWhitespaceChars() {
            List<String> normalized = List.of("\t  \n  ");

            assertThrows(IllegalArgumentException.class,
                    () -> mapper.map(defaultCommand, normalized));
        }
    }


    @Nested
    @DisplayName("Immutability:")
    class Immutability {

        @Test
        @DisplayName("Keywords list in Subscription is unmodifiable")
        void keywordsAreUnmodifiable() {
            Subscription subscription = mapper.map(defaultCommand, List.of("ai"));

            assertThrows(UnsupportedOperationException.class,
                    () -> subscription.getFilter().getKeywords().add("X"));
        }

        @Test
        @DisplayName("Filter object is unique per subscription instance")
        void filterObjectIsUniquePerSubscription() {
            Subscription sub1 = mapper.map(defaultCommand, List.of("ai"));
            Subscription sub2 = mapper.map(defaultCommand, List.of("ai"));

            assertNotSame(sub1.getFilter(), sub2.getFilter(),
                    "Each subscription should have its own filter instance");
        }
    }

    @Nested
    @DisplayName("Language normalization:")
    class LanguageNormalization {

        @Test
        @DisplayName("Trims whitespace around language string")
        void trimsLanguage() {
            TelegramSubscribeCommand command = createCommandWithLanguage("   sv   ");

            Subscription subscription = mapper.map(command, List.of("ai"));

            assertEquals("sv", subscription.getFilter().getLanguage());
        }

        @Test
        @DisplayName("Sets language to null if blank")
        void setsLanguageToNullIfBlank() {
            TelegramSubscribeCommand command = createCommandWithLanguage("   ");

            Subscription subscription = mapper.map(command, List.of("ai"));

            assertNull(subscription.getFilter().getLanguage());
        }

        @Test
        @DisplayName("Preserves valid language code as-is")
        void preservesValidLanguage() {
            TelegramSubscribeCommand command = createCommandWithLanguage("en");

            Subscription subscription = mapper.map(command, List.of("ai"));

            assertEquals("en", subscription.getFilter().getLanguage());
        }
    }

    //  Helpers

    private TelegramSubscribeCommand createCommandWithLanguage(String language) {
        return new TelegramSubscribeCommand(
                DEFAULT_CHAT_ID,
                language,
                DEFAULT_MAX_ITEMS,
                IGNORED_KEYWORD_LIST,
                DEFAULT_SCHEDULE
        );
    }

    private TelegramSubscribeCommand createCommandWithSchedule(SchedulePreset schedule) {
        return new TelegramSubscribeCommand(
                DEFAULT_CHAT_ID,
                DEFAULT_LANGUAGE,
                DEFAULT_MAX_ITEMS,
                IGNORED_KEYWORD_LIST,
                schedule
        );
    }
}
