package org.ieee11073.sdc.dpws.soap.wsdiscovery;

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
