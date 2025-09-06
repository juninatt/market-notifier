package se.pbt.ddplus.notifier.telegram.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelegramCommandParser")
class TelegramCommandParserTest {

    private final TelegramCommandParser parser = new TelegramCommandParser();

    @Test
    @DisplayName("Parses single keyword without quotes")
    void parseSingleKeyword() {
        var result = parser.parseSubscribeCommand(123L, "/subscribe Tesla en 5");
        assertEquals(123L, result.chatId());
        assertEquals(List.of("Tesla"), result.keywords());
        assertEquals("en", result.language());
        assertEquals(5, result.maxItems());
    }

    @Test
    @DisplayName("Parses single quoted multi-word keyword in swedish")
    void parseSingleKeywordsInSwedish() {
        var result = parser.parseSubscribeCommand(123L, "/subscribe \"Skåne Mejerier\" sv 5");
        assertEquals(123L, result.chatId());
        assertEquals(List.of("Skåne Mejerier"), result.keywords());
        assertEquals("sv", result.language());
        assertEquals(5, result.maxItems());
    }

    @Test
    @DisplayName("Parses single quoted multi-word keyword in english")
    void parseSingleMultiwordKeyword() {
        var result = parser.parseSubscribeCommand(123L, "/subscribe \"Silicon Valley\" sv 3");
        assertEquals(List.of("Silicon Valley"), result.keywords());
        assertEquals("sv", result.language());
        assertEquals(3, result.maxItems());
    }

    @Test
    @DisplayName("Parses multiple quoted multi-word keyword")
    void parseSeveralMultiwordKeyword() {
        var result = parser.parseSubscribeCommand(123L, "/subscribe \"VanEck Space Innovations UCITS ETF\" \"Meta Space Fund\" \"Silicon Valley\" sv 3");
        assertEquals(List.of("VanEck Space Innovations UCITS ETF", "Meta Space Fund", "Silicon Valley"), result.keywords());
        assertEquals("sv", result.language());
        assertEquals(3, result.maxItems());
    }

    @Test
    @DisplayName("Parses mixed quoted and unquoted keywords")
    void parseMixedKeywords() {
        var result = parser.parseSubscribeCommand(123L, "/subscribe Tesla \"Silicon Valley\" en 2");
        assertEquals(List.of("Tesla", "Silicon Valley"), result.keywords());
    }

    @Test
    @DisplayName("Treats hyphenated word as single keyword")
    void parsesHyphenatedKeywordAsSingle() {
        var result = parser.parseSubscribeCommand(123L, "/subscribe Tesla Silicon-Valley en 2");
        assertEquals(List.of("Tesla", "Silicon-Valley"), result.keywords());
    }

    @Test
    @DisplayName("Throws on invalid maxItems")
    void failsOnInvalidMaxItems() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> parser.parseSubscribeCommand(1L, "/subscribe Tesla en notanumber"));
        assertTrue(e.getMessage().contains("maxItems"));
    }

    @Test
    @DisplayName("Throws on too few arguments")
    void failsOnTooFewArgs() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseSubscribeCommand(1L, "/subscribe en 5"));
    }

    @Test
    @DisplayName("Throws if language is not 2 letters")
    void failsOnInvalidLanguage() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parseSubscribeCommand(1L, "/subscribe Tesla english 5")
        );
    }

    @Test
    @DisplayName("Throws if language is a number")
    void failsIfLanguageIsANumber() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parseSubscribeCommand(1L, "/subscribe Tesla 2 5")
        );
    }

    @Test
    @DisplayName("Throws if maxItems is not a number")
    void failsOnInvalidMaxItem() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parseSubscribeCommand(1L, "/subscribe Tesla en five")
        );
    }

    @Test
    @DisplayName("Throws if no keywords given")
    void failIfNoKeywords() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parseSubscribeCommand(1L, "/subscribe en 5")
        );
    }
}
