package org.somda.sdc.proto.discovery.consumer.helper;

import org.somda.protosdc.proto.model.discovery.DiscoveryMessages;
import org.somda.protosdc.proto.model.discovery.DiscoveryTypes;
import org.somda.protosdc.proto.model.discovery.Endpoint;
import org.somda.protosdc.proto.model.discovery.ResolveMatches;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class ResolveCallable implements Callable<Endpoint> {
    private final String expectedMessageId;
    private final Lock lock;
    private final long maxWaitInMillis;
    private final Condition condition;
    private final Supplier<Optional<ResolveMatches>> findMatch;

    public ResolveCallable(Duration maxWait,
                           String expectedMessageId,
                           Lock lock,
                           Condition condition,
                           Supplier<Optional<ResolveMatches>> findMatch) {
        this.maxWaitInMillis = maxWait.toMillis();
        this.expectedMessageId = expectedMessageId;
        this.lock = lock;
        this.condition = condition;
        this.findMatch = findMatch;
    }

    @Override
    public Endpoint call() throws Exception {
        try {
            lock.lock();
            var wait = maxWaitInMillis;
            while (wait > 0) {
                var tStartInMillis = System.currentTimeMillis();
                var msg = findMatch.get();
                if (msg.isPresent()) {
                    return msg.get().getEndpoint();
                }

                if (!condition.await(wait, TimeUnit.MILLISECONDS)) {
                    break;
                }

                msg = findMatch.get();
                wait -= System.currentTimeMillis() - tStartInMillis;
                if (msg.isPresent()) {
                    return msg.get().getEndpoint();
                }
            }
        } finally {
            lock.unlock();
        }

        throw new RuntimeException(String.format(
                "No ResolveMatches for Resolve message with ID %s received in %s milliseconds",
                expectedMessageId,
                maxWaitInMillis
        ));
    }
}