package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ByeMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.event.ProbeTimeoutMessage;

/**
 * Designates a deriving class as a WS-Discovery Hello, Bye and ProbeMatches observer.
 * <p>
 * Annotate methods with {@link com.google.common.eventbus.Subscribe} to catch
 * <ul>
 * <li>{@link HelloMessage}
 * <li>{@link ByeMessage}
 * <li>{@link ProbeMatchesMessage}
 * <li>{@link ProbeTimeoutMessage}
 * </ul>
 */
public interface HelloByeAndProbeMatchesObserver {
}
