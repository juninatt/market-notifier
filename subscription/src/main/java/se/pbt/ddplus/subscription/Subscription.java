package se.pbt.ddplus.subscription;

import lombok.Getter;
import lombok.Setter;
import java.util.TimeZone;

/**
 * Represents a subscription that controls
 * how and when news should be retrieved and filtered.
 */
@Getter
@Setter
public class Subscription {

    private String id;
    private String schedule;
    private TimeZone timezone;
    private SubscriptionFilter filter;
    private int maxItems;
    private int priority;
    private boolean enabled;

    @Override
    public String toString() {
        return "Subscription{" +
                "id='" + id + '\'' +
                ", schedule='" + schedule + '\'' +
                ", timezone=" + timezone +
                ", filter=" + filter +
                ", maxItems=" + maxItems +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
