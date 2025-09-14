package se.pbt.ddplus.subscription.model;

import lombok.Getter;
import lombok.Setter;
import se.pbt.ddplus.core.schedule.SchedulePreset;

import java.util.TimeZone;

/**
 * Represents a subscription that controls
 * how and when news should be retrieved and filtered.
 */
@Getter
@Setter
public class Subscription {

    private String id;
    private long chatId;
    private SchedulePreset schedule;
    private TimeZone timezone;
    private SubscriptionFilter filter;
    private int maxItems;
    private boolean enabled;

    @Override
    public String toString() {
        return "Subscription{" +
                "id='" + id + '\'' +
                "chatId='" + chatId + '\'' +
                ", schedule=" + (schedule != null ? schedule.name() : null) +
                ", timezone=" + timezone +
                ", filter=" + filter +
                ", maxItems=" + maxItems +
                ", enabled=" + enabled +
                '}';
    }
}
