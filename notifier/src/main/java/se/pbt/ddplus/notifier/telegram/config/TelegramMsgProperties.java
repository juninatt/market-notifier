package se.pbt.ddplus.notifier.telegram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centralized user-facing texts for the Telegram bot.
 * <p>
 * Binds to telegram message properties in {@code application-telegram.yml}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "telegram.messages")
public class TelegramMsgProperties {

    /** General help/start text shown when the user types /start or /help. */
    private String help;

    /** Fallback text when a command or message is not recognized. */
    private String unknownCommand;

    private SubscriptionMessage subscriptionMessage = new SubscriptionMessage();
    private ManagementMessage managementMessage = new ManagementMessage();
    private ErrorMessage error = new ErrorMessage();
    private Button button = new Button();


    /**
     * Texts related to creating and confirming subscriptions.
     * Placeholders are used for dynamic values such as keywords, language, schedule, and max items.
     */
    @Getter @Setter
    public static class SubscriptionMessage {
        private String confirm;
        private String saved;
        private String cancelled;
        private String invalidFormat;
    }

    /**
     * Texts related to managing existing subscriptions,
     * including listing them, showing when none exist, and removing by ID or keyword.
     */
    @Getter @Setter
    public static class ManagementMessage {
        private String list;
        private String none;
        private String removed;
        private String notFound;
    }

    /**
     * Error texts that are shown when something goes wrong
     * or when external services are temporarily unavailable.
     */
    @Getter @Setter
    public static class ErrorMessage {
        private String unexpected;
        private String unavailable;
    }

    /**
     * Labels for inline buttons shown to the user,
     * for example confirmation, cancellation, or navigation.
     */
    @Getter @Setter
    public static class Button {
        private String confirm;
        private String cancel;
        private String back;
    }
}
