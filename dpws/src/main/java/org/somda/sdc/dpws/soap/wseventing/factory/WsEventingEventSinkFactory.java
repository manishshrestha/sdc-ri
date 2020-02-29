package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.wseventing.EventSink;

/**
 * Creates {@link EventSink} instances.
 */
public interface WsEventingEventSinkFactory {
    /**
     * Creates a new WS-Eventing event sink.
     *
     * @param requestResponseClient request response client where to send requests to (subscribe, renew, ...).
     * @param hostAddress           address where to bind a notification sink server.
     * @return a new {@link EventSink} instance.
     */
    EventSink createWsEventingEventSink(@Assisted RequestResponseClient requestResponseClient,
                                        @Assisted String hostAddress);
}
