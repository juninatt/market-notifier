package se.pbt.mn.subscription.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * A personal watchlist of companies and topic categories, hand-maintained in a standalone
 * YAML file rather than created through the Telegram/email subscribe flow.
 * <p>
 * Unlike {@link Subscription}, a watchlist carries no schedule or chat id -- it exists only
 * to drive the one-off digest sent when the application starts up.
 */
@Getter
@Setter
@NoArgsConstructor
public class Watchlist {

    /**
     * Address the startup digest is delivered to.
     */
    private String email;

    /**
     * Tickers or company names to follow. Every news item matching any of these -- by
     * ticker or by name appearing in the title/description -- is included in the digest,
     * with no upper limit.
     */
    private List<String> companies = List.of();

    /**
     * Topic categories to follow (e.g. "Space", "AI"), matched case-insensitively against
     * article titles and descriptions. Only the most recently published items per category
     * are included in the digest, up to the configured limit.
     */
    private List<String> categories = List.of();

    @Override
    public String toString() {
        return "Watchlist{" +
                "email='" + email + '\'' +
                ", companies=" + companies +
                ", categories=" + categories +
                '}';
    }
}
