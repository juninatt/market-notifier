package se.pbt.mn.subscription.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import se.pbt.mn.core.subscription.SchedulePreset;

import java.util.TimeZone;

/**
 * Represents a subscription that defines how and when news should be retrieved and filtered.
 * <p>
 * A subscription contains a {@link SubscriptionFilter} for filtering rules
 * and a {@link SchedulePreset} that determines the delivery schedule.
 * It also holds metadata such as chat ID, timezone, and delivery settings.
 * <p>
 * {@code email} is optional -- when set, the subscription is also (or instead) delivered
 * via the email channel, in addition to Telegram via {@code chatId}.
 * <p>
 * {@code schedule} is also optional: a subscription with no schedule is never picked up by
 * the recurring {@code NewsDispatchScheduler}, but is still included when the application is
 * run in the {@code digest-now} profile, which sends every enabled subscription once and exits.
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
    @Email
    private String email;
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
                ", email='" + email + '\'' +
                ", schedule=" + (schedule != null ? schedule.name() : null) +
                ", timezone=" + timezone +
                ", filter=" + filter +
                ", maxItems=" + maxItems +
                ", enabled=" + enabled +
                '}';
    }
}
