package se.pbt.mn.telegram.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.telegram.client.TelegramApiClient;
import se.pbt.mn.telegram.format.TelegramMessageSplitter;
import se.pbt.mn.telegram.format.TelegramOutputFormatter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link NotificationChannel} implementation that delivers notifications as
 * Telegram messages via the existing {@link TelegramApiClient}.
 * <p>
 * A notification longer than Telegram's per-message limit is sent as several consecutive
 * messages (see {@link TelegramMessageSplitter}) rather than being rejected or truncated.
 */
@Component
public class TelegramNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationChannel.class);

    private static final DateTimeFormatter PUBLISHED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final TelegramApiClient apiClient;

    public TelegramNotificationChannel(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String id() {
        return "telegram";
    }

    @Override
    public void send(String recipientAddress, Notification notification) {
        long chatId;
        try {
            chatId = Long.parseLong(recipientAddress);
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("Skipping Telegram notification: '{}' is not a valid chat id", recipientAddress);
            return;
        }

        try {
            for (String part : TelegramMessageSplitter.split(format(notification))) {
                apiClient.sendFormattedMessage(chatId, part).block();
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram notification to chatId={}", chatId, e);
        }
    }

    /**
     * Builds a MarkdownV2-formatted message. Dynamic text (title, body, source, tickers) is
     * escaped individually via {@link TelegramOutputFormatter#escapeMarkdown}; the
     * surrounding Markdown syntax (bold, link) is left raw so Telegram renders it as
     * formatting instead of literal characters.
     */
    private String format(Notification notification) {
        StringBuilder sb = new StringBuilder();

        if (hasText(notification.title())) {
            sb.append("📰 *").append(TelegramOutputFormatter.escapeMarkdown(notification.title())).append("*");
        }
        if (hasText(notification.body())) {
            appendSection(sb, TelegramOutputFormatter.escapeMarkdown(notification.body()));
        }

        String meta = formatMeta(notification);
        if (meta != null) {
            appendSection(sb, "📌 " + meta);
        }

        String tags = formatTickers(notification.tickers());
        if (tags != null) {
            appendSection(sb, "🏷 " + tags);
        }

        if (hasText(notification.url())) {
            appendSection(sb, "🔗 [Läs mer](" + TelegramOutputFormatter.escapeLinkUrl(notification.url()) + ")");
        }

        return sb.toString();
    }

    private String formatMeta(Notification notification) {
        String source = hasText(notification.source()) ? TelegramOutputFormatter.escapeMarkdown(notification.source()) : null;
        String published = notification.publishedAt() != null
                ? TelegramOutputFormatter.escapeMarkdown(PUBLISHED_AT_FORMAT.format(notification.publishedAt()))
                : null;

        if (source != null && published != null) return source + " · " + published;
        return source != null ? source : published;
    }

    private String formatTickers(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return null;
        }
        return tickers.stream()
                .map(TelegramOutputFormatter::escapeMarkdown)
                .collect(Collectors.joining(", "));
    }

    private void appendSection(StringBuilder sb, String section) {
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(section);
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
