package se.pbt.ddplus.subscription.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import se.pbt.ddplus.subscription.persistence.SubscriptionStorage;

import java.util.List;

/**
 * Wrapper class used for deserializing a list of {@link Subscription} objects from a YAML file.
 * <p>
 * The root YAML object is expected to contain a {@code subscriptions} key
 * pointing to the list of subscriptions.
 * <p>
 * This class is primarily used by {@link SubscriptionStorage}
 * when loading and saving subscriptions.
 */
@Getter
@Setter
public class SubscriptionListWrapper {

    @NotNull
    @NotEmpty
    @Valid
    private List<Subscription> subscriptions;

    @Override
    public String toString() {
        return "SubscriptionListWrapper{" +
                "subscriptions=" + subscriptions +
                '}';
    }
}
