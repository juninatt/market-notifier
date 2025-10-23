package se.pbt.marketnotifier.notifier.telegram.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.marketnotifier.core.subscription.SchedulePreset;
import se.pbt.marketnotifier.notifier.telegram.model.TelegramCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelegramCommandParser")
class TelegramInputParserTest {

    private final TelegramInputParser parser = new TelegramInputParser();

    @Nested
    @DisplayName("Command structure:")
    class CommandStructure {

        @Test
        @DisplayName("Throws on too few arguments")
        void failsOnTooFewArgs() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe en 5")));
        }

        @Test
        @DisplayName("Accepts leading/trailing/multiple spaces")
        void acceptsExtraWhitespace() {
            var r = parser.parseSubscribeCommand(
                    new TelegramCommand(1L, "   /subscribe   AI   en   5   "));

            assertEquals(List.of("AI"), r.keywords());
            assertEquals("en", r.language());
            assertEquals(5, r.maxItems());
        }

        @Test
        @DisplayName("Fails when schedule appears after maxItems")
        void failsWhenScheduleAfterMaxItems() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe AI en 5 me")));
        }

        @Test
        @DisplayName("Fails when command token is not /subscribe")
        void failsOnUnknownCommand() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parseSubscribeCommand(new TelegramCommand(1L, "/start AI en 5")));
        }
    }

    @Nested
    @DisplayName("Language token:")
    class Language {

        @Test
        @DisplayName("Parses single keyword without quotes")
        void parseSingleKeyword() {
            var result = parser.parseSubscribeCommand(new TelegramCommand(123L, "/subscribe Tesla en 5"));

            assertEquals(123L, result.chatId());
            assertEquals(List.of("Tesla"), result.keywords());
            assertEquals("en", result.language());
            assertEquals(5, result.maxItems());
        }

        @Test
        @DisplayName("Throws if language is not 2 letters")
        void failsOnInvalidLanguage() {
            assertThrows(IllegalArgumentException.class, () ->
                    parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe Tesla english 5"))
            );
        }
    }

    @Nested
    @DisplayName("Keywords:")
    class Keyword {

        @Test
        @DisplayName("Throws if no keywords given")
        void failIfNoKeywords() {
            assertThrows(IllegalArgumentException.class, () ->
                    parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe en 5"))
            );
        }

        @Test
        @DisplayName("Parses single quoted multi-word keyword in swedish")
        void parseSingleKeywordsInSwedish() {
            var result = parser.parseSubscribeCommand(
                    new TelegramCommand(123L, "/subscribe \"Skåne Mejerier\" sv 5")
            );

            assertEquals(123L, result.chatId());
            assertEquals(List.of("Skåne Mejerier"), result.keywords());
            assertEquals("sv", result.language());
            assertEquals(5, result.maxItems());
        }

        @Test
        @DisplayName("Parses single quoted multi-word keyword in english")
        void parseSingleMultiwordKeyword() {
            var result = parser.parseSubscribeCommand(
                    new TelegramCommand(123L, "/subscribe \"Silicon Valley\" sv 3"));

            assertEquals(List.of("Silicon Valley"), result.keywords());
            assertEquals("sv", result.language());
            assertEquals(3, result.maxItems());
        }

        @Test
        @DisplayName("Parses multiple quoted multi-word keyword")
        void parseSeveralMultiwordKeyword() {
            var result = parser.parseSubscribeCommand(
                    new TelegramCommand(
                            123L,
                            "/subscribe \"VanEck Space Innovations UCITS ETF\" \"" +
                                    "Meta Space Fund\" \"Silicon Valley\" sv 3")
            );

            assertEquals(List.of(
                    "VanEck Space Innovations UCITS ETF", "Meta Space Fund", "Silicon Valley"), result.keywords()
            );

            assertEquals("sv", result.language());
            assertEquals(3, result.maxItems());
        }

        @Test
        @DisplayName("Parses mixed quoted and unquoted keywords")
        void parseMixedKeywords() {
            var result = parser.parseSubscribeCommand(
                    new TelegramCommand(123L, "/subscribe Tesla \"Silicon Valley\" en 2"));

            assertEquals(List.of("Tesla", "Silicon Valley"), result.keywords());
        }

        @Test
        @DisplayName("Treats hyphenated word as single keyword")
        void parsesHyphenatedKeywordAsSingle() {
            var result = parser.parseSubscribeCommand(
                    new TelegramCommand(123L, "/subscribe Tesla Silicon-Valley en 2"));

            assertEquals(List.of("Tesla", "Silicon-Valley"), result.keywords());
        }
    }

    @Nested
    @DisplayName(("Max items:"))
    class MaxItem {

        @Test
        @DisplayName("Throws on invalid maxItems")
        void failsOnInvalidMaxItems() {
            Exception e = assertThrows(IllegalArgumentException.class,
                    () -> parser.parseSubscribeCommand(
                            new TelegramCommand(1L, "/subscribe Tesla en notanumber"))
            );

            assertTrue(e.getMessage().contains("maxItems"));
        }

        @Test
        @DisplayName("Throws if maxItems is not a number")
        void failsOnInvalidMaxItem() {
            assertThrows(IllegalArgumentException.class, () ->
                    parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe Tesla en five"))
            );
        }
    }

    @Nested
    @DisplayName("Schedule")
    class Schedule {

        @Test
        @DisplayName("Parses with explicit schedule alias 'me' for morning_evening")
        void parsesScheduleAliasMorningEvening() {
            var result = parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe AI en me 10"));

            assertEquals(SchedulePreset.MORNING_EVENING, result.schedule());
            assertEquals(List.of("AI"), result.keywords());
            assertEquals("en", result.language());
        }

        @Test
        @DisplayName("Parses without schedule -> leaves schedule null for factory default")
        void parsesWithoutSchedule() {
            var result = parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe AI en 5")
            );

            assertNull(result.schedule()); // factory will apply default
            assertEquals("en", result.language());
            assertEquals(List.of("AI"), result.keywords());
        }

        @Test
        @DisplayName("Throws if unknown schedule alias is used")
        void failsOnUnknownScheduleAlias() {
            Exception e = assertThrows(IllegalArgumentException.class,
                    () -> parser.parseSubscribeCommand(
                            new TelegramCommand(1L, "/subscribe AI en nonsense 5")));

            assertTrue(e.getMessage().toLowerCase().contains("language"));
        }

        @Test
        @DisplayName("Parses with explicit schedule alias 'm' for morning")
        void parsesScheduleAliasMorning() {
            var result = parser.parseSubscribeCommand(new TelegramCommand(1L, "/subscribe Tesla en m 5"));

            assertEquals(SchedulePreset.MORNING, result.schedule());
            assertEquals("en", result.language());
            assertEquals(5, result.maxItems());
        }

        @Test
        @DisplayName("Parses with explicit schedule full name 'evening'")
        void parsesScheduleFullNameEvening() {
            var result = parser.parseSubscribeCommand(
                    new TelegramCommand(1L, "/subscribe \"Silicon Valley\" en evening 2")
            );

            assertEquals(SchedulePreset.EVENING, result.schedule());
            assertEquals(List.of("Silicon Valley"), result.keywords());
            assertEquals("en", result.language());
        }
    }
}
