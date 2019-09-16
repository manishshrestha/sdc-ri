package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ByeMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ProbeTimeoutMessage;

/**
 * Indicate class as a WS-Discovery Hello, Bye and ProbeMatches observer.
 *
 * Annotate method with {@link com.google.common.eventbus.Subscribe} to
 *
 * - {@link HelloMessage}
 * - {@link ByeMessage}
 * - {@link ProbeMatchesMessage}
 * - {@link ProbeTimeoutMessage}
 */
public interface HelloByeAndProbeMatchesObserver {
}
