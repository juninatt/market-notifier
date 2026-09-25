package se.pbt.mn.telegram.format;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits an already MarkdownV2-formatted message into parts that each fit within
 * Telegram's per-message length limit.
 * <p>
 * Splits prefer paragraph boundaries ({@code "\n\n"}), then line boundaries, and only cut
 * inside a line as a last resort -- never directly after an escaping backslash or between
 * the two halves of a surrogate pair, either of which would make Telegram reject the part.
 * The limit is applied to the escaped text, which is always at least as long as what
 * Telegram counts after parsing, so a part never ends up over the real limit.
 */
public final class TelegramMessageSplitter {

    /**
     * Telegram's maximum length for a single message's text.
     * See: https://core.telegram.org/bots/api#sendmessage
     */
    public static final int MAX_MESSAGE_LENGTH = 4096;

    private static final String PARAGRAPH = "\n\n";
    private static final String LINE = "\n";

    private TelegramMessageSplitter() {}

    public static List<String> split(String text) {
        return split(text, MAX_MESSAGE_LENGTH);
    }

    static List<String> split(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return List.of(text);
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split(PARAGRAPH)) {
            for (String piece : fit(paragraph, maxLength)) {
                if (!current.isEmpty() && current.length() + PARAGRAPH.length() + piece.length() > maxLength) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                if (!current.isEmpty()) {
                    current.append(PARAGRAPH);
                }
                current.append(piece);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }

    /**
     * Breaks a single paragraph that is itself too long into pieces of at most
     * {@code maxLength}, by line first and by character only when a line still doesn't fit.
     */
    private static List<String> fit(String paragraph, int maxLength) {
        if (paragraph.length() <= maxLength) {
            return List.of(paragraph);
        }

        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : paragraph.split(LINE)) {
            for (String chunk : cut(line, maxLength)) {
                if (!current.isEmpty() && current.length() + LINE.length() + chunk.length() > maxLength) {
                    pieces.add(current.toString());
                    current.setLength(0);
                }
                if (!current.isEmpty()) {
                    current.append(LINE);
                }
                current.append(chunk);
            }
        }
        if (!current.isEmpty()) {
            pieces.add(current.toString());
        }
        return pieces;
    }

    private static List<String> cut(String line, int maxLength) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (line.length() - start > maxLength) {
            int end = start + maxLength;
            while (end > start + 1 && (line.charAt(end - 1) == '\\' || Character.isHighSurrogate(line.charAt(end - 1)))) {
                end--;
            }
            chunks.add(line.substring(start, end));
            start = end;
        }
        chunks.add(line.substring(start));
        return chunks;
    }
}
