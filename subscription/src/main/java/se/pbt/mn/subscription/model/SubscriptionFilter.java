package se.pbt.mn.subscription.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Defines the filtering rules of a {@link Subscription}.
 * <p>
 * {@code keywords}, {@code tickers}, and {@code language} are matched together: each
 * non-empty one must match for an item to be included (see {@code SubscriptionFilterMatcher}).
 * {@code companies} and {@code categories} are independent of that and of each other:
 * {@code companies} matches an item on ticker OR name with no upper limit on results, and
 * {@code categories} matches free-text topics, capped to the most recently published items
 * per category. Both are meant to be hand-edited directly in the subscriptions file rather
 * than set through the Telegram/email subscribe commands.
 */
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionFilter {

    @NotNull
    @NotEmpty
    private List<@NotBlank String> keywords;
    @NotNull
    private List<@NotBlank String> tickers;
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be ISO code like 'en' or 'sv-SE'")
    private String language;
    private List<String> companies = List.of();
    private List<String> categories = List.of();

    @Override
    public String toString() {
        return "SubscriptionFilter{" +
                "keywords=" + keywords +
                ", tickers=" + tickers +
                ", language='" + language + '\'' +
                ", companies=" + companies +
                ", categories=" + categories +
                '}';
    }
}
