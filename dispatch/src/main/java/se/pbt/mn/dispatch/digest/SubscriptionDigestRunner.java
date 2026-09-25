package se.pbt.mn.dispatch.digest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.dispatch.grouping.NewsGrouper;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.util.List;

/**
 * Sends every enabled subscription its digest exactly once, then exits the application --
 * activated by running with the {@code digest-now} Spring profile
 * (e.g. {@code mvn spring-boot:run -pl app-runner -Dspring-boot.run.profiles=digest-now}),
 * which also excludes the recurring scheduler and the Telegram/IMAP listeners so nothing
 * else starts in the background.
 * <p>
 * Delivery goes through the same {@link SubscriptionDigestSender} as the recurring
 * {@code NewsDispatchScheduler}, so a subscription gets the same digest either way -- this
 * just sends it now, for every enabled subscription, regardless of schedule.
 */
@Component
@Profile("digest-now")
public class SubscriptionDigestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDigestRunner.class);

    private final SubscriptionService subscriptionService;
    private final NewsFetcher newsFetcher;
    private final SubscriptionDigestSender digestSender;
    private final ConfigurableApplicationContext context;

    public SubscriptionDigestRunner(
            SubscriptionService subscriptionService,
            NewsFetcher newsFetcher,
            SubscriptionDigestSender digestSender,
            ConfigurableApplicationContext context
    ) {
        this.subscriptionService = subscriptionService;
        this.newsFetcher = newsFetcher;
        this.digestSender = digestSender;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        int result;
        try {
            result = sendAllDigests();
        } catch (Exception e) {
            log.error("Digest run failed: {}", e.toString());
            result = 1;
        }
        exit(result);
    }

    /**
     * Terminates the JVM with the given exit code now that the digest run has completed.
     * Package-private and non-final purely so a test can override it instead of letting it
     * kill the test process.
     */
    void exit(int exitCode) {
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    private int sendAllDigests() {
        List<Subscription> subscriptions = subscriptionService.findAllEnabled();
        if (subscriptions.isEmpty()) {
            log.info("No enabled subscriptions found, nothing to send.");
            return 0;
        }

        List<NewsGroup> allGroups = NewsGrouper.group(newsFetcher.fetchAll());
        if (allGroups.isEmpty()) {
            log.info("No news fetched, nothing to send.");
            return 0;
        }

        for (Subscription subscription : subscriptions) {
            digestSender.send(subscription, allGroups);
        }
        return 0;
    }
}
