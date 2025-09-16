package se.pbt.ddplus.subscription.model;

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
 * A filter specifies which items are included in a subscription based on
 * keywords, stock tickers, and the preferred language.
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

    @Override
    public String toString() {
        return "SubscriptionFilter{" +
                "keywords=" + keywords +
                ", tickers=" + tickers +
                ", language='" + language + '\'' +
                '}';
    }
}
