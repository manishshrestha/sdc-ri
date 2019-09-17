package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import com.google.common.util.concurrent.ListenableFuture;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Ws-Discovery Client interface.
 * <p>
 * todo DGr Create observable for receiving Hello and Bye messages
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231815">Conceptual Message Content</a>
 */
public interface WsDiscoveryClient extends Interceptor {
    ListenableFuture<Integer> sendProbe(String probeId, List<QName> types, List<String> scopes)
            throws MarshallingException, TransportException;
    ListenableFuture<Integer> sendProbe(String probeId, List<QName> types, List<String> scopes, Integer maxResults)
            throws MarshallingException, TransportException;
    ListenableFuture<ProbeMatchesType> sendDirectedProbe(RequestResponseClient rrClient, List<QName> types, List<String> scopes);
    ListenableFuture<ResolveMatchesType> sendResolve(EndpointReferenceType epr) throws MarshallingException, TransportException;
    void registerHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver observer);
    void unregisterHelloByeAndProbeMatchesObserver(HelloByeAndProbeMatchesObserver observer);
}
