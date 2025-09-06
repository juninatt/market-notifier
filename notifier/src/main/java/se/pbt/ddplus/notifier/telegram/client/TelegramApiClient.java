package se.pbt.ddplus.notifier.telegram.client;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Simple client for sending messages via the Telegram Bot API.
 */
public final class TelegramApiClient {

    // TODO: Move to Constants
    private static final String SEND_MESSAGE_PATH = "/sendMessage";
    private static final String PARSE_MODE = "MarkdownV2";

    private final WebClient client;

    /**
     * Creates a WebClient configured to call the Telegram Bot API
     * using the provided base URL and bot token.
     */
    public TelegramApiClient(String baseUrl, String token) {
        this.client = WebClient.builder()
                .baseUrl(baseUrl + "/bot" + token)
                .build();
    }

    /**
     * Builds a JSON payload and sends it to Telegram's {@code /sendMessage} endpoint.
     */
    public Mono<Void> sendMessage(long chatId, String text) {
        String payload = "{\"chat_id\":" + chatId
                + ",\"text\":" + json(text)
                + ",\"parse_mode\":\"" + PARSE_MODE + "\"}";

        return client.post()
                .uri(SEND_MESSAGE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .then();
    }

    /**
     * Converts a String so all special characters are
     * represented in a JSON-safe way then wraps in quotes.
     */
    private static String json(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                + "\"";
    }
}
