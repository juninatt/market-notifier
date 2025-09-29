package se.pbt.ddplus.notifier.telegram.polling;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import se.pbt.ddplus.notifier.telegram.client.TelegramApiClient;
import se.pbt.ddplus.notifier.telegram.config.TelegramProperties;
import se.pbt.ddplus.notifier.telegram.model.TelegramCommand;
import se.pbt.ddplus.notifier.telegram.service.TelegramService;

import java.util.Optional;

/**
 * Runs a Telegram long-poll worker and binds its lifecycle to Spring.
 * <p>
 * Keeps the application responsive to inbound Telegram updates and translates
 * them into {@link TelegramCommand} objects for downstream handling.
 */
// TODO: Centralize hardcoded values
@Component
public class TelegramLongPollingRunner implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelegramLongPollingRunner.class);

    private final TelegramProperties props;
    private final TelegramService telegramService;
    private final TelegramApiClient apiClient;

    private volatile boolean running = false;
    private Thread pollThread;
    private volatile long offset;

    private static final long SLEEP_SHORT_MS = 150;
    private static final long SLEEP_ON_ERROR_MS = 1500;
    private static final long THREAD_JOIN_TIMEOUT_MS = 2000;


    /**
     * Builds the runner with configuration, service, and API client.
     * <p>
     * Initializes the polling offset so restarts continue from a known point.
     */
    public TelegramLongPollingRunner(
            TelegramProperties props,
            TelegramService telegramService,
            TelegramApiClient apiClient
    ) {
        this.props = props;
        this.telegramService = telegramService;
        this.apiClient = apiClient;
        this.offset = props.getInitialOffset();
    }

    // SmartLifecycle

    /**
     * Starts the background worker thread.
     * <p>
     * Begins consuming Telegram updates as soon as the application context is ready.
     */
    @Override
    public synchronized void start() {
        if (running) return;
        if (!shouldStart()) return;

        running = true;
        startWorkerThread();
        log.info("Telegram long-poll: started (initialOffset={}).", offset);
    }

    /**
     * Stops the background worker thread.
     * <p>
     * Ensures a clean shutdown without leaving dangling threads.
     */
    @Override
    public synchronized void stop() {
        if (!running) return;
        log.info("Telegram long-poll: stopping...");
        running = false;
        joinQuietly(pollThread, THREAD_JOIN_TIMEOUT_MS);
        log.info("Telegram long-poll: stopped.");
    }

    /**
     * Signals to Spring that shutdown has completed.
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    /**
     * Reports whether the worker is currently running.
     */
    @Override public boolean isRunning() {
        return running;
    }

    /**
     * Enables automatic startup on context refresh.
     */
    @Override public boolean isAutoStartup() {
        return true;
    }

    /**
     * Sets lifecycle phase for coordinated start/stop.
     */
    @Override public int getPhase() {
        return 0;
    }

    // Worker loop

    /**
     * Performs the background polling cycle and dispatches messages.
     * <p>
     * Continuously fetches, parses, and hands off Telegram updates while applying
     * short delays to avoid tight loops and tolerate transient failures.
     */
    private void pollLoop() {
        final int timeout = Math.max(1, props.getLongPollTimeoutSeconds());

        while (running) {
            try {
                JsonNode root = safeGetUpdates(timeout);
                if (root == null) {
                    sleep(SLEEP_SHORT_MS);
                    continue;
                }
                if (!root.path("ok").asBoolean(false))  {
                    sleep(SLEEP_SHORT_MS);
                    continue;
                }

                JsonNode result = root.path("result");
                if (!result.isArray() || result.isEmpty()) {
                    sleep(SLEEP_SHORT_MS);
                    continue;
                }

                for (JsonNode upd : result) {
                    advanceOffset(upd);
                    toCommand(upd).ifPresent(telegramService::handleTelegramCommand);
                }
                sleep(SLEEP_SHORT_MS);
            } catch (Exception e) {
                log.warn("pollLoop error: {}", e.toString());
                sleep(SLEEP_ON_ERROR_MS);
            }
        }
    }

    // Helpers

    /**
     * Validates startup conditions and logs if startup is skipped.
     */
    private boolean shouldStart() {
        if (!props.isEnabled()) {
            log.info("Telegram long-poll: disabled by config.");
            return false;
        }
        if (props.getBotToken() == null || props.getBotToken().isBlank()) {
            log.error("Telegram long-poll: missing botToken");
            return false;
        }
        return true;
    }

    /**
     * Creates and starts the dedicated worker thread.
     */
    private void startWorkerThread() {
        pollThread = new Thread(this::pollLoop, "telegram-long-poll");
        pollThread.start();
    }

    /**
     * Waits for a thread to finish, restoring interrupt state if interrupted.
     */
    private void joinQuietly(Thread t, long ms) {
        if (t == null || !t.isAlive())
            return;
        try {
            t.join(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Calls Telegram getUpdates and converts failures into a neutral result.
     */
    private JsonNode safeGetUpdates(int timeout) {
        try {
            return apiClient.getUpdates(timeout, offset)
                    .onErrorResume(ex -> {
                        log.warn("getUpdates error: {}", ex.toString());
                        sleep(SLEEP_ON_ERROR_MS);
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception ex) {
            log.warn("getUpdates unexpected error: {}", ex.toString());
            sleep(SLEEP_ON_ERROR_MS);
            return null;
        }
    }

    /**
     * Advances the update cursor to {@code update_id + 1}.
     * <p>
     * Prevents re-processing old updates and keeps polling progress consistent.
     */
    private void advanceOffset(JsonNode upd) {
        long id = upd.path("update_id").asLong(0);
        if (id > 0) offset = id + 1;
    }

    /**
     * Extracts a text message into a {@link TelegramCommand}.
     */
    private Optional<TelegramCommand> toCommand(JsonNode upd) {
        JsonNode message = upd.path("message");
        if (message.isMissingNode())
            return Optional.empty();

        long chatId = message.path("chat").path("id").asLong(0);
        if (chatId <= 0)
            return Optional.empty();

        JsonNode textNode = message.get("text");
        if (textNode == null || textNode.isNull())
            return Optional.empty();

        String text = textNode.asText();
        if (text == null || text.isBlank())
            return Optional.empty();

        return Optional.of(new TelegramCommand(chatId, text));
    }

    /**
     * Pauses the current thread for a short interval.
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
