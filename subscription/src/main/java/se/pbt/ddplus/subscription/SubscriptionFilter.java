package se.pbt.ddplus.subscription;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Holds filtering rules for a {@link Subscription},
 * such as keywords, tickers, and preferred language.
 */
@Getter
@Setter
public class SubscriptionFilter {

    private List<String> keywords;
    private List<String> tickers;
    private String language;

    @Override
    public String toString() {
        return "SubscriptionFilter{" +
                "keywords=" + keywords +
                ", tickers=" + tickers +
                ", language='" + language + '\'' +
                '}';
    }
}
