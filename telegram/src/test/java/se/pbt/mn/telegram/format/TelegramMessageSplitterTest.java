package se.pbt.mn.telegram.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TelegramMessageSplitter")
class TelegramMessageSplitterTest {

    @Test
    @DisplayName("Leaves a message within the limit as a single part")
    void split_withShortText_returnsSinglePart() {
        assertEquals(List.of("short"), TelegramMessageSplitter.split("short", 10));
    }

    @Test
    @DisplayName("Splits on paragraph boundaries and keeps every paragraph intact")
    void split_withManyParagraphs_splitsOnParagraphBoundaries() {
        String text = "aaaa\n\nbbbb\n\ncccc";

        List<String> parts = TelegramMessageSplitter.split(text, 10);

        assertEquals(List.of("aaaa\n\nbbbb", "cccc"), parts);
    }

    @Test
    @DisplayName("Splits an over-long paragraph on line boundaries")
    void split_withLongParagraph_splitsOnLines() {
        String text = "aaaa\nbbbb\ncccc";

        List<String> parts = TelegramMessageSplitter.split(text, 9);

        assertEquals(List.of("aaaa\nbbbb", "cccc"), parts);
    }

    @Test
    @DisplayName("Never ends a part on an escaping backslash")
    void split_withEscapeAtCutPoint_keepsEscapeWithItsCharacter() {
        String text = "abcd\\.efgh";

        List<String> parts = TelegramMessageSplitter.split(text, 5);

        parts.forEach(part -> assertFalse(part.endsWith("\\"), part));
        assertEquals(text, String.join("", parts));
    }

    @Test
    @DisplayName("Keeps every part within the limit and loses no content for a real-sized message")
    void split_withMessageOverTelegramLimit_keepsPartsWithinLimit() {
        String paragraph = "x".repeat(200);
        String text = String.join("\n\n", Collections.nCopies(50, paragraph));

        List<String> parts = TelegramMessageSplitter.split(text);

        assertTrue(parts.size() > 1);
        parts.forEach(part -> assertTrue(part.length() <= TelegramMessageSplitter.MAX_MESSAGE_LENGTH));
        assertEquals(text, String.join("\n\n", parts));
    }
}
