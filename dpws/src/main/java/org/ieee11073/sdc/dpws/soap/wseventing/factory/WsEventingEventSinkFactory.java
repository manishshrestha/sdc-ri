package org.ieee11073.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.wseventing.EventSink;

import java.net.URI;

/**
 * Create {@link EventSink} instances.
 */
public interface WsEventingEventSinkFactory {
    /**
     * @param requestResponseClient Request response client where to send requests to (subscribe, renew, ...).
     * @param hostAddress      Address where to bind a notification sink server
     */
    EventSink createWsEventingEventSink(@Assisted RequestResponseClient requestResponseClient,
                                        @Assisted URI hostAddress);
}
