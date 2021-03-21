package org.somda.sdc.proto.discovery.consumer.helper;

import com.google.common.eventbus.EventBus;
import org.somda.sdc.proto.discovery.consumer.event.DeviceProbeTimeoutMessage;
import org.somda.sdc.proto.discovery.consumer.event.ProbedDeviceFoundMessage;
import org.somda.protosdc.proto.model.discovery.DiscoveryMessages;
import org.somda.protosdc.proto.model.discovery.DiscoveryTypes;
import org.somda.protosdc.proto.model.discovery.Endpoint;
import org.somda.protosdc.proto.model.discovery.ProbeMatches;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class ProbeCallable implements Callable<List<Endpoint>> {
    private final EventBus helloByeProbeEvents;
    private final Lock lock;
    private final String probeId;
    private final Integer maxResults;
    private final long maxWaitInMillis;
    private final Condition condition;
    private final Supplier<Optional<ProbeMatches>> findMatch;
    private final List<Endpoint> endpoints;

    public ProbeCallable(String probeId,
                         Integer maxResults,
                         Duration maxWait,
                         Lock lock,
                         Condition condition,
                         EventBus helloByeProbeEvents,
                         Supplier<Optional<ProbeMatches>> findMatch) {
        this.probeId = probeId;
        this.maxResults = maxResults;
        this.maxWaitInMillis = maxWait.toMillis();
        this.findMatch = findMatch;
        this.lock = lock;
        this.condition = condition;
        this.helloByeProbeEvents = helloByeProbeEvents;
        this.endpoints = new ArrayList<>();
    }

    @Override
    public List<Endpoint> call() throws Exception {
        Integer probeMatchesCount = 0;
        long wait = maxWaitInMillis;
        try {
            lock.lock();
            while (wait > 0) {
                long tStartInMillis = System.currentTimeMillis();
                probeMatchesCount = fetchData(probeMatchesCount);
                if (probeMatchesCount.equals(maxResults)) {
                    break;
                }

                if (!condition.await(wait, TimeUnit.MILLISECONDS)) {
                    break;
                }

                wait -= System.currentTimeMillis() - tStartInMillis;
                probeMatchesCount = fetchData(probeMatchesCount);
                if (probeMatchesCount.equals(maxResults)) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }

        helloByeProbeEvents.post(new DeviceProbeTimeoutMessage(probeMatchesCount, probeId));
        return endpoints;
    }

    private Integer fetchData(Integer probeMatchesCount) {
        var msg = findMatch.get();
        if (msg.isPresent()) {
            endpoints.addAll(msg.get().getEndpointList());
            helloByeProbeEvents.post(new ProbedDeviceFoundMessage(msg.get().getEndpointList(), probeId));
            probeMatchesCount++;
        }
        return probeMatchesCount;
    }
}
