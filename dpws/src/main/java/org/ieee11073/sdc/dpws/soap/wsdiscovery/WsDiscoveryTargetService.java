package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import com.google.common.primitives.UnsignedInteger;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * WS-Discovery Target Service interface.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231815">Conceptual Message Content</a>
 */
public interface WsDiscoveryTargetService extends Interceptor {
    EndpointReferenceType getEndpointReference();

    void setTypes(List<QName> qNames);
    List<QName> getTypes();

    void setScopes(List<String> uris);
    List<String> getScopes();

    void setXAddrs(List<String> xAddrs);
    List<String> getXAddrs();

    void setMatchBy(MatchBy matchBy);
    MatchBy getMatchBy();

    void setMetadataModified();

    UnsignedInteger getMetadataVersion();

    UnsignedInteger sendHello() throws MarshallingException, TransportException;

    UnsignedInteger sendHello(boolean forceNewMetadataVersion) throws MarshallingException, TransportException;

    void sendBye() throws MarshallingException, TransportException;
}
