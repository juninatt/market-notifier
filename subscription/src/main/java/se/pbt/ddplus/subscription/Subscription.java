package se.pbt.ddplus.subscription;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Defines a user subscription with an ID, a provider, and a list of keywords.
 * Used to describe what news the user wants to receive.
 */
@Getter
@Setter
public class Subscription {

    private String id;
    private String provider;
    private List<String> keywords;

    @Override
    public String toString() {
        return "Subscription{" +
                "id='" + id + '\'' +
                ", provider='" + provider + '\'' +
                ", keywords=" + keywords +
                '}';
    }
}
