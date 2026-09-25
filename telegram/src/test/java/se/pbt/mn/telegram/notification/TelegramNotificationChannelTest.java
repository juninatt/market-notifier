package se.pbt.mn.telegram.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.telegram.client.TelegramApiClient;
import se.pbt.mn.telegram.format.TelegramMessageSplitter;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("TelegramNotificationChannel")
class TelegramNotificationChannelTest {

    private static final long CHAT_ID = 123L;
    private static final String RECIPIENT = String.valueOf(CHAT_ID);

    private TelegramApiClient apiClient;
    private TelegramNotificationChannel channel;

    @BeforeEach
    void setUp() {
        apiClient = mock(TelegramApiClient.class);
        channel = new TelegramNotificationChannel(apiClient);
        when(apiClient.sendFormattedMessage(anyLong(), anyString())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("id() returns 'telegram'")
    void id_returnsTelegram() {
        assertEquals("telegram", channel.id());
    }

    @Test
    @DisplayName("Sends via sendFormattedMessage, not the escaping sendMessage")
    void send_usesFormattedSendPath() {
        channel.send(RECIPIENT, new Notification("Title", "Body", null, null, null, null));

        verify(apiClient).sendFormattedMessage(eq(CHAT_ID), anyString());
        verify(apiClient, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    @DisplayName("Skips sending when the recipient address is not a valid chat id")
    void send_withNonNumericRecipient_doesNotCallApiClient() {
        channel.send("not-a-chat-id", new Notification("Title", "Body", null, null, null, null));

        verifyNoInteractions(apiClient);
    }

    @Nested
    @DisplayName("Message formatting")
    class Formatting {

        @Test
        @DisplayName("Renders the title in bold with a leading emoji")
        void format_withTitle_rendersBold() {
            channel.send(RECIPIENT, new Notification("Tesla rallies", null, null, null, null, null));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID), eq("📰 *Tesla rallies*"));
        }

        @Test
        @DisplayName("Escapes MarkdownV2 special characters in the title")
        void format_withMarkdownSpecialsInTitle_escapesThem() {
            channel.send(RECIPIENT, new Notification("Q3 report: +10% (beats est.)", null, null, null, null, null));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID),
                    eq("📰 *Q3 report: \\+10% \\(beats est\\.\\)*"));
        }

        @Test
        @DisplayName("Includes source and published date joined with a middle dot")
        void format_withSourceAndDate_joinsWithMiddleDot() {
            Instant publishedAt = Instant.parse("2026-08-08T12:00:00Z");

            channel.send(RECIPIENT, new Notification("Title", null, null, "MarketWatch", publishedAt, null));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID), contains("📌 MarketWatch · "));
        }

        @Test
        @DisplayName("Omits the meta line entirely when source and date are both missing")
        void format_withoutSourceOrDate_omitsMetaLine() {
            channel.send(RECIPIENT, new Notification("Title", null, null, null, null, null));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID), argThatDoesNotContain("📌"));
        }

        @Test
        @DisplayName("Renders tickers joined by commas with a tag emoji")
        void format_withTickers_joinsWithCommas() {
            channel.send(RECIPIENT, new Notification("Title", null, null, null, null, List.of("TSLA", "AAPL")));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID), contains("🏷 TSLA, AAPL"));
        }

        @Test
        @DisplayName("Omits the ticker line when the list is empty")
        void format_withEmptyTickers_omitsTickerLine() {
            channel.send(RECIPIENT, new Notification("Title", null, null, null, null, List.of()));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID), argThatDoesNotContain("🏷"));
        }

        @Test
        @DisplayName("Renders the url as a Markdown link, escaping ')' inside it")
        void format_withUrl_rendersAsMarkdownLink() {
            channel.send(RECIPIENT, new Notification("Title", null, "https://example.com/a(b)", null, null, null));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID),
                    contains("🔗 [Läs mer](https://example.com/a(b\\))"));
        }

        @Test
        @DisplayName("Separates present sections with a blank line")
        void format_withMultipleSections_separatesWithBlankLine() {
            channel.send(RECIPIENT, new Notification("Title", "Body", "https://example.com", null, null, null));

            verify(apiClient).sendFormattedMessage(eq(CHAT_ID),
                    eq("📰 *Title*\n\nBody\n\n🔗 [Läs mer](https://example.com)"));
        }
    }

    @Test
    @DisplayName("Sends a notification over Telegram's length limit as several messages, in order")
    void send_withBodyOverLengthLimit_sendsSeveralMessagesInOrder() {
        List<String> entries = IntStream.range(0, 40)
                .mapToObj(i -> "Entry " + i + " " + "x".repeat(150))
                .toList();
        String body = String.join("\n\n", entries);

        channel.send(RECIPIENT, new Notification("Your subscription digest", body, null, null, null, List.of()));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(apiClient, atLeast(2)).sendFormattedMessage(eq(CHAT_ID), captor.capture());
        List<String> parts = captor.getAllValues();
        parts.forEach(part -> assertTrue(part.length() <= TelegramMessageSplitter.MAX_MESSAGE_LENGTH));
        assertTrue(parts.get(0).startsWith("📰 *Your subscription digest*"));

        String joined = String.join("\n\n", parts);
        int previous = -1;
        for (String entry : entries) {
            int index = joined.indexOf(entry);
            assertTrue(index > previous, "missing or out of order: " + entry);
            previous = index;
        }
    }

    @Test
    @DisplayName("When the API call fails, the exception does not propagate")
    void send_withApiFailure_doesNotPropagateException() {
        when(apiClient.sendFormattedMessage(anyLong(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("API down")));

        assertDoesNotThrow(() -> channel.send(RECIPIENT, new Notification("Title", "Body", null, null, null, null)));
    }

    private static String argThatDoesNotContain(String needle) {
        return argThat(actual -> actual != null && !actual.contains(needle));
    }
}
