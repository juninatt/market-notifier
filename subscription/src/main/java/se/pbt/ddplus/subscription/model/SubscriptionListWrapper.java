package se.pbt.ddplus.subscription.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Wrapper class used for deserializing a list of subscriptions from a YAML file.
 * The root YAML object should contain a "subscriptions" key.
 */
@Getter
@Setter
public class SubscriptionListWrapper {

    private List<Subscription> subscriptions;

    @Override
    public String toString() {
        return "SubscriptionListWrapper{" +
                "subscriptions=" + subscriptions +
                '}';
    }
}
