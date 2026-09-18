package se.pbt.mn.telegram.client;

import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import se.pbt.mn.telegram.format.TelegramOutputFormatter;

/**
 * Lightweight client for interacting with the Telegram Bot API.
 * <p>
 * Handles sending messages and retrieving updates using {@link WebClient}.
 * All outgoing text is automatically escaped for Telegram's MarkdownV2 syntax
 * via {@link TelegramOutputFormatter}.
 */
public final class TelegramApiClient {

    // TODO: Move to Constants
    private static final String SEND_MESSAGE_PATH = "/sendMessage";
    private static final String GET_UPDATES_PATH  = "/getUpdates";
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
     * Text is escaped to ensure valid MarkdownV2 formatting.
     */
    public Mono<Void> sendMessage(long chatId, String text) {
        return sendRaw(chatId, TelegramOutputFormatter.escapeMarkdown(text));
    }

    /**
     * Sends text that is already formatted for MarkdownV2 -- unlike {@link #sendMessage},
     * this does not blanket-escape the text first. Use this when the caller has already
     * escaped its own dynamic content and deliberately left Markdown syntax (bold, links)
     * intact around it.
     */
    public Mono<Void> sendFormattedMessage(long chatId, String preFormattedMarkdown) {
        return sendRaw(chatId, preFormattedMarkdown);
    }

    private Mono<Void> sendRaw(long chatId, String safeText) {
        String payload = "{\"chat_id\":" + chatId
                + ",\"text\":" + TelegramOutputFormatter.json(safeText)
                + ",\"parse_mode\":\"" + PARSE_MODE + "\"}";

        return client.post()
                .uri(SEND_MESSAGE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> apiError("sendMessage", resp.statusCode(), body))
                )
                .toBodilessEntity()
                .then();
    }

    /**
     * Retrieves incoming updates from Telegram using long polling.
     * <p>
     * This method is used by a background worker to continuously
     * fetch user commands or messages sent to the bot by the user.
     */
    public Mono<JsonNode> getUpdates(int timeoutSeconds, long offset) {
        return client.get()
                .uri(uri -> uri
                        .path(GET_UPDATES_PATH)
                        .queryParam("timeout", Math.max(1, timeoutSeconds))
                        .queryParam("offset", offset)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> apiError("getUpdates", resp.statusCode(), body))
                )
                .bodyToMono(JsonNode.class);
    }

    /**
     * Creates a standardized {@link RuntimeException} wrapped in a {@link Mono}.
     * <p>
     * Used for consistent error handling when the Telegram API responds
     * with a non-successful HTTP status.
     */
    private static <T> Mono<T> apiError(String action, HttpStatusCode status, String body) {
        return Mono.error(new RuntimeException(
                "Telegram API " + action + " failed: status=" + status.value() + ", body=" + body
        ));
    }
}
