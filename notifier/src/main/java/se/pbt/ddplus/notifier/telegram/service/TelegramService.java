package se.pbt.ddplus.notifier.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import se.pbt.ddplus.core.subscription.TelegramSubscribeCommand;
import se.pbt.ddplus.notifier.telegram.client.TelegramApiClient;
import se.pbt.ddplus.notifier.telegram.config.TelegramMsgProperties;
import se.pbt.ddplus.notifier.telegram.config.TelegramStorageProperties;
import se.pbt.ddplus.notifier.telegram.format.TelegramInputParser;
import se.pbt.ddplus.notifier.telegram.model.TelegramCommand;

import se.pbt.ddplus.subscription.model.Subscription;
import se.pbt.ddplus.subscription.contract.SubscriptionMapper;
import se.pbt.ddplus.subscription.service.SubscriptionService;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Service layer that processes incoming Telegram commands.
 * <p>
 * Decides which command to execute (/subscribe, /list, /unsubscribe, etc.)
 * and delegates to the correct handler. All user-facing messages come from {@link TelegramMsgProperties}.
 */
@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final TelegramApiClient apiClient;
    private final TelegramInputParser inputParser;
    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper<TelegramSubscribeCommand> mapper;
    private final TelegramMsgProperties messages;
    private final TelegramStorageProperties storage;

    private final Map<String, Consumer<TelegramCommand>> commandHandlers = new HashMap<>();

    public TelegramService(
            TelegramApiClient apiClient,
            TelegramInputParser inputParser,
            SubscriptionService subscriptionService,
            SubscriptionMapper<TelegramSubscribeCommand> mapper,
            TelegramMsgProperties messages,
            TelegramStorageProperties storage
    ) {
        this.apiClient = apiClient;
        this.inputParser = inputParser;
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
        this.messages = messages;
        this.storage = storage;

        commandHandlers.put("/help", this::handleHelp);
        commandHandlers.put("/start", this::handleHelp);
        commandHandlers.put("/subscribe", this::handleSubscribe);
        commandHandlers.put("/list", this::handleList);
        commandHandlers.put("/unsubscribe", this::handleUnsubscribe);
    }

    /**
     * Entry point for handling all incoming Telegram commands.
     * <p>
     * Decides which command to execute (subscribe, list, unsubscribe, etc.) and delegates to the correct handler.
     * Unknown commands are answered with a standard fallback message.
     */
    public void handleTelegramCommand(TelegramCommand command) {
        String text = command.message() == null ? "" : command.message().trim();
        String base = extractBaseCommand(text);

        Consumer<TelegramCommand> handler =
                commandHandlers.getOrDefault(base, this::handleUnknown);
        handler.accept(command);
    }

    /**
     * Extracts the base command (e.g. "/subscribe") from the raw text.
     */
    private String extractBaseCommand(String text) {
        if (text.isBlank()) return "";
        return text.trim().split("\\s+")[0];
    }

    // Handlers

    /**
     * Handles /help and /start commands.
     * <p>
     * Sends the standard help text to guide the user on how to interact with the bot.
     */
    private void handleHelp(TelegramCommand cmd) {
        log.debug("Help requested by chatId={}", cmd.chatId());
        reply(cmd, messages.getHelp()).subscribe();
    }

    /**
     * Handles the {@code /subscribe} command received from a Telegram user.
     * <p>
     * The command is parsed into a {@link TelegramSubscribeCommand}, mapped into a {@link Subscription} domain object
     * and then persisted through {@link SubscriptionService}.
     * <p>
     * Responds to the user with a confirmation message or an error message if
     * validation fails or an exception occurs during processing.
     */
    private void handleSubscribe(TelegramCommand cmd) {
        try {
            var subscribeCommand = inputParser.parseSubscribeCommand(cmd);

            var normalizedKeywords = subscribeCommand.keywords().stream()
                    .map(String::trim)
                    .filter(k -> !k.isBlank())
                    .toList();

            var subscription = mapper.map(subscribeCommand, normalizedKeywords);

            subscriptionService.save(subscription, storage.getSubscriptions());

            log.info("New subscription saved for chatId={}", cmd.chatId());
            reply(cmd, messages.getSubscriptionMessage().getSaved()).subscribe();
        } catch (IllegalArgumentException e) {
            log.debug("Invalid subscribe format for chatId={}", cmd.chatId(), e);
            reply(cmd, messages.getSubscriptionMessage().getInvalidFormat()).subscribe();
            reply(cmd, messages.getHelp()).subscribe();
        } catch (Exception e) {
            log.error("Unexpected error while handling /subscribe for chatId={}", cmd.chatId(), e);
            reply(cmd, messages.getError().getUnexpected()).subscribe();
        }
    }



    /**
     * Handles /list commands.
     * <p>
     * Retrieves all subscriptions for a chat and renders them into a human-readable list.
     */
    private void handleList(TelegramCommand cmd) {
        safeRun(cmd, () -> {
            List<String> subs = subscriptionService.listByChatId(cmd.chatId());
            if (subs == null || subs.isEmpty()) {
                reply(cmd, messages.getManagementMessage().getNone()).subscribe();
            } else {
                String rendered = renderSubscriptionList(subs);
                reply(cmd, MessageFormat.format(messages.getManagementMessage().getList(), rendered))
                        .subscribe();
            }
            log.debug("Listed subscriptions for chatId={} (count={})",
                    cmd.chatId(), subs == null ? 0 : subs.size());
        });
    }

    /**
     * Handles /unsubscribe commands.
     * <p>
     * Removes a subscription by ID or keyword and confirms the result with the user.
     */
    private void handleUnsubscribe(TelegramCommand cmd) {
        String arg = cmd.message().replaceFirst("^/unsubscribe\\s*", "").trim();
        if (arg.isEmpty()) {
            reply(cmd, messages.getManagementMessage().getNotFound()).subscribe();
            return;
        }
        safeRun(cmd, () -> {
            boolean removed = subscriptionService.removeByIdOrKeyword(cmd.chatId(), arg);
            reply(cmd, removed
                    ? messages.getManagementMessage().getRemoved()
                    : messages.getManagementMessage().getNotFound()).subscribe();
            log.info("Unsubscribe requested for chatId={} arg={} removed={}", cmd.chatId(), arg, removed);
        });
    }

    /**
     * Handles unrecognized commands or text.
     * <p>
     * Replies with a standard fallback message so the user knows the command was not understood.
     */
    private void handleUnknown(TelegramCommand cmd) {
        log.warn("Unknown command from chatId={} message={}", cmd.chatId(), cmd.message());
        reply(cmd, messages.getUnknownCommand()).subscribe();
    }

    //  Helpers

    /**
     * Convenience wrapper for sending a reply bound to a specific command.
     * <p>
     * Escapes MarkdownV2 syntax before sending to ensure Telegram renders
     * text correctly, and handles errors gracefully.
     */
    private Mono<Void> reply(TelegramCommand cmd, String text) {
        log.debug("Sending message to chatId={} text='{}'", cmd.chatId(), text);
        return apiClient.sendMessage(cmd.chatId(), text)
                .onErrorResume(ex -> {
                    log.error("Failed to send message to chatId={} text='{}'", cmd.chatId(), text, ex);
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Runs a command handler and catches unexpected errors.
     * <p>
     * Ensures the bot always replies with a fallback error message
     * instead of crashing silently.
     */
    private void safeRun(TelegramCommand cmd, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Error while handling command for chatId={}", cmd.chatId(), e);
            reply(cmd, messages.getError().getUnexpected()).subscribe();
        }
    }

    /**
     * Renders a numbered subscription list for display.
     */
    private String renderSubscriptionList(List<?> subs) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Object s : subs) {
            sb.append(i++).append(". ").append(s).append("\n");
        }
        return sb.toString().trim();
    }
}
