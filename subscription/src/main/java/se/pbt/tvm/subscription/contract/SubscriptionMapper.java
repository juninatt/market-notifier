package se.pbt.tvm.subscription.contract;

import se.pbt.tvm.subscription.model.Subscription;
import java.util.List;

/**
 * Defines a contract for mapping external subscription sources into
 * {@link Subscription} domain objects.
 *
 * @param <S> The type of the external source object (e.g. TelegramSubscribeCommand)
 */
public interface SubscriptionMapper<S> {

    /**
     * Maps the given source object into a {@link Subscription}.
     *
     * @param source the source object containing raw subscription data
     * @param normalizedKeywords preprocessed keywords used for filtering
     * @return a mapped {@link Subscription} instance
     */
    Subscription map(S source, List<String> normalizedKeywords);
}
