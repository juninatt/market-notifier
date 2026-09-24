package se.pbt.mn.subscription.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.pbt.mn.subscription.model.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * Validates that a subscription is complete and unique before it is saved.
 * <p>
 * Ensures that all required fields are present and that no identical
 * subscription already exists for the same chat.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionValidator {

    private final SubscriptionSanitizer sanitizer;

    /**
     * Performs structural and duplicate validation for a subscription before it is saved.
     */
    public Optional<String> validate(Subscription candidate, List<Subscription> existing) {
        String structuralError = validateStructure(candidate);
        if (structuralError != null) {
            return Optional.of(structuralError);
        }

        List<Subscription> safeExisting = Optional.ofNullable(existing).orElse(List.of());
        boolean duplicate = safeExisting.stream().anyMatch(existingSub ->
                sanitizer.isSameRecipient(existingSub, candidate)
                        && sanitizer.usesSameLanguage(existingSub, candidate)
                        && sanitizer.containsSameKeywords(existingSub, candidate)
        );

        return duplicate
                ? Optional.of("A subscription with the same keywords and language already exists for this recipient.")
                : Optional.empty();
    }

    /**
     * Checks that the subscription contains all mandatory fields before additional validation.
     */
    private String validateStructure(Subscription candidate) {
        if (candidate == null) return "Subscription cannot be null.";
        if (candidate.getFilter() == null) return "Subscription filter cannot be null.";

        if (isEmpty(candidate.getFilter().getKeywords())
                && isEmpty(candidate.getFilter().getTickers())
                && isEmpty(candidate.getFilter().getCompanies())
                && isEmpty(candidate.getFilter().getCategories())) {
            return "At least one of keywords, tickers, companies, or categories must be specified.";
        }
        if (candidate.getFilter().getLanguage() == null || candidate.getFilter().getLanguage().isBlank()) {
            return "Language must be specified.";
        }
        return null;
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }
}
