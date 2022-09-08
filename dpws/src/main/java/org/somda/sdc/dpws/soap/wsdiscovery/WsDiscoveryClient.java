package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.util.concurrent.ListenableFuture;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * Ws-Discovery Client interface.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231815"
 * >Conceptual Message Content</a>
 */
public interface WsDiscoveryClient extends Interceptor {

    /**
     * Sends a probe message using the given parameters.
     * <p>
     * ProbeMatches messages are handled by the observers.
     *
     * @param probeId of the probe
     * @param types   to probe for
     * @param scopes  to probe for
     * @return Future containing the number of matches within the given timeout.
     * @throws MarshallingException on marshalling issues in the outgoing message
     * @throws TransportException   on transport issues in the outgoing message
     * @throws InterceptorException on preprocessing issues with the outgoing message
     * @see #registerHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver)
     */
    ListenableFuture<Integer> sendProbe(String probeId, Collection<QName> types, Collection<String> scopes)
            throws MarshallingException, TransportException, InterceptorException;

    /**
     * Sends a probe message using the given parameters with a limited number of results until future finishes.
     * <p>
     * ProbeMatches messages are handled by the observers.
     *
     * @param probeId    of the probe
     * @param types      to probe for
     * @param scopes     to probe for
     * @param maxResults number of results to wait for at most
     * @return Future containing the number of matches within the given timeout.
     * @throws MarshallingException on marshalling issues in the outgoing message
     * @throws TransportException   on transport issues in the outgoing message
     * @throws InterceptorException on preprocessing issues with the outgoing message
     * @see #registerHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver)
     */
    ListenableFuture<Integer> sendProbe(String probeId, Collection<QName> types,
                                        Collection<String> scopes, Integer maxResults)
            throws MarshallingException, TransportException, InterceptorException;

    /**
     * Sends a directed probe to a device.
     *
     * @param rrClient to send request on
     * @param types    to probe for
     * @param scopes   to probe for
     * @return future providing probe matches response
     * <p>
     * TODO LDe: This is inconsistent with the rest, why does it not throw any exceptions?
     */
    ListenableFuture<ProbeMatchesType> sendDirectedProbe(RequestResponseClient rrClient, List<QName> types,
                                                         List<String> scopes);

    /**
     * Sends a resolve message for a given endpoint reference.
     *
     * @param epr to resolve for
     * @return future providing resolve matches response
     * @throws MarshallingException on marshalling issues in the outgoing message
     * @throws TransportException   on transport issues in the outgoing message
     * @throws InterceptorException on preprocessing issues with the outgoing message
     */
    ListenableFuture<ResolveMatchesType> sendResolve(EndpointReferenceType epr)
            throws MarshallingException, TransportException, InterceptorException;

    /**
     * Register an observer to handle Hello, Bye and ProbeMatches messages.
     *
     * @param observer to listen on
     */
    void registerHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver observer);

    /**
     * Unregister an observer handling Hello, Bye and ProbeMatches messages.
     *
     * @param observer to unregister
     */
    void unregisterHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver observer);
}
