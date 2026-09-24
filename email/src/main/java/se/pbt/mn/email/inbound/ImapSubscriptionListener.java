package se.pbt.mn.email.inbound;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.subscription.ParsedSubscribeCommand;
import se.pbt.mn.core.subscription.SubscribeCommand;
import se.pbt.mn.core.subscription.SubscribeCommandParser;
import se.pbt.mn.email.config.ImapProperties;
import se.pbt.mn.subscription.mapper.SubscribeCommandMapper;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.util.List;
import java.util.Properties;

/**
 * Polls an IMAP inbox for unread emails and creates a {@link Subscription} from each one,
 * mirroring how {@code TelegramLongPollingRunner} polls Telegram for inbound commands.
 * <p>
 * A message's body is parsed with the same {@link SubscribeCommandParser} used for Telegram's
 * /subscribe command. The sender's own address is used as the delivery email unless the body
 * explicitly specifies a different trailing address.
 * <p>
 * Excluded under the {@code digest-now} profile, which sends every enabled subscription
 * once and exits without listening for inbound subscribe emails.
 */
@Component
@Profile("!digest-now")
public class ImapSubscriptionListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ImapSubscriptionListener.class);
    private static final long THREAD_JOIN_TIMEOUT_MS = 5_000;
    private static final long SLEEP_CHUNK_MS = 500;

    private final ImapProperties properties;
    private final SubscriptionService subscriptionService;
    private final SubscribeCommandMapper mapper;

    private volatile boolean running = false;
    private Thread pollThread;

    public ImapSubscriptionListener(
            ImapProperties properties,
            SubscriptionService subscriptionService,
            SubscribeCommandMapper mapper
    ) {
        this.properties = properties;
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
    }

    // SmartLifecycle

    @Override
    public synchronized void start() {
        if (running) return;
        if (!properties.isEnabled()) {
            log.info("IMAP subscription listener: disabled by config.");
            return;
        }

        running = true;
        pollThread = new Thread(this::pollLoop, "imap-subscription-poll");
        pollThread.start();
        log.info("IMAP subscription listener: started (folder={}, interval={}s).",
                properties.getFolder(), properties.getPollIntervalSeconds());
    }

    @Override
    public synchronized void stop() {
        if (!running) return;
        log.info("IMAP subscription listener: stopping...");
        running = false;
        joinQuietly(pollThread, THREAD_JOIN_TIMEOUT_MS);
        log.info("IMAP subscription listener: stopped.");
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    // Worker loop

    private void pollLoop() {
        long intervalMs = Math.max(1, properties.getPollIntervalSeconds()) * 1000L;

        while (running) {
            try {
                pollOnce();
            } catch (Exception e) {
                log.warn("IMAP poll error: {}", e.toString());
            }
            sleepInChunks(intervalMs);
        }
    }

    /**
     * Sleeps in short chunks, re-checking {@code running} between each one, so
     * {@link #stop()} doesn't have to wait out a full poll interval to take effect.
     */
    private void sleepInChunks(long totalMs) {
        long remaining = totalMs;
        while (running && remaining > 0) {
            long toSleep = Math.min(SLEEP_CHUNK_MS, remaining);
            sleep(toSleep);
            remaining -= toSleep;
        }
    }

    /**
     * Connects, processes every unseen message in the configured folder, and disconnects.
     * Reconnecting each cycle is simpler and more robust than holding a long-lived
     * connection open across the poll interval.
     */
    void pollOnce() throws MessagingException {
        Properties sessionProps = new Properties();
        String protocol = properties.isSsl() ? "imaps" : "imap";
        sessionProps.put("mail.store.protocol", protocol);

        Session session = Session.getInstance(sessionProps);

        try (Store store = session.getStore(protocol)) {
            store.connect(properties.getHost(), properties.getPort(), properties.getUsername(), properties.getPassword());

            Folder folder = store.getFolder(properties.getFolder());
            folder.open(Folder.READ_WRITE);
            try {
                Message[] unseen = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                for (Message message : unseen) {
                    processMessageSafely(message);
                }
            } finally {
                folder.close(false);
            }
        }
    }

    private void processMessageSafely(Message message) {
        String body = null;
        try {
            String from = extractFromAddress(message);
            body = extractText(message);
            processMessage(from, body);
        } catch (Exception e) {
            log.warn("Failed to process inbound subscription email: {} (raw body: '{}')", e, body);
        } finally {
            try {
                message.setFlag(Flags.Flag.SEEN, true);
            } catch (MessagingException e) {
                log.warn("Failed to mark inbound email as read, it may be reprocessed: {}", e.toString());
            }
        }
    }

    /**
     * Parses and saves a subscription from an inbound email's sender address and body text.
     * Package-private so it can be unit tested without a real IMAP connection.
     */
    void processMessage(String fromAddress, String bodyText) {
        ParsedSubscribeCommand parsed = SubscribeCommandParser.parse(bodyText);
        String email = parsed.email() != null ? parsed.email() : fromAddress;

        SubscribeCommand command = new SubscribeCommand(
                0, parsed.language(), parsed.maxItems(), parsed.keywords(), parsed.schedule(), email);

        List<String> normalizedKeywords = command.keywords().stream()
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();

        Subscription subscription = mapper.map(command, normalizedKeywords);
        SubscriptionService.SaveResult result = subscriptionService.save(subscription);

        if (result.success()) {
            log.info("New subscription created via email for {}", email);
        } else {
            log.warn("Rejected inbound subscription email for {}: {}", email, result.message());
        }
    }

    private static String extractFromAddress(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) {
            throw new MessagingException("Message has no From address");
        }
        return from[0] instanceof InternetAddress ia ? ia.getAddress() : from[0].toString();
    }

    private static String extractText(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    sb.append(part.getContent());
                }
            }
            return sb.toString();
        }
        return "";
    }

    private void joinQuietly(Thread thread, long ms) {
        if (thread == null || !thread.isAlive()) {
            return;
        }
        try {
            thread.join(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
