package org.somda.sdc.dpws.http.apache;

import org.apache.http.client.HttpClient;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;


/**
 * Creates {@linkplain ClientTransportBinding} instances.
 */
public interface ClientTransportBindingFactory {

    /**
     * Instantiates {@linkplain ClientTransportBinding} with the given objects and injected objects.
     * @param client used for binding
     * @param clientUri uri to connect to
     * @param marshalling marshalling service
     * @param soapUtil utility to create {@linkplain org.somda.sdc.dpws.soap.SoapMessage}
     * @return a new {@linkplain ClientTransportBinding}
     */
    ClientTransportBinding create(HttpClient client, String clientUri, SoapMarshalling marshalling, SoapUtil soapUtil);

    /**
     * Instantiates an {@linkplain ApacheHttpClient}.
     *
     * @param client to use as backend
     * @return new client
     */
    ApacheHttpClient createHttpClient(HttpClient client);
}
