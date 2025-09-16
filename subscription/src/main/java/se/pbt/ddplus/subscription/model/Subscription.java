package se.pbt.ddplus.subscription.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import se.pbt.ddplus.core.schedule.SchedulePreset;

import java.util.TimeZone;

/**
 * Represents a subscription that defines how and when news should be retrieved and filtered.
 * <p>
 * A subscription contains a {@link SubscriptionFilter} for filtering rules
 * and a {@link SchedulePreset} that determines the delivery schedule.
 * It also holds metadata such as chat ID, timezone, and delivery settings.
 */
// TODO: Replace constructor with builder annotation
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Subscription {

    @EqualsAndHashCode.Include
    private String id;
    @Positive
    private long chatId;
    @NotNull
    private SchedulePreset schedule;
    @NotNull
    private TimeZone timezone;
    @NotNull
    @Valid
    private SubscriptionFilter filter;
    @Positive
    private int maxItems;
    private boolean enabled;

    @Override
    public String toString() {
        return "Subscription{" +
                "id='" + id + '\'' +
                ", chatId='" + chatId + '\'' +
                ", schedule=" + (schedule != null ? schedule.name() : null) +
                ", timezone=" + timezone +
                ", filter=" + filter +
                ", maxItems=" + maxItems +
                ", enabled=" + enabled +
                '}';
    }
}
