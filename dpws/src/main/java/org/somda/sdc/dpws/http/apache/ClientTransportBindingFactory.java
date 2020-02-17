package org.somda.sdc.dpws.http.apache;

import java.net.URI;

import org.apache.http.client.HttpClient;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;


/**
 * Creates {@linkplain ClientTransportBinding} instances.
 */
public interface ClientTransportBindingFactory {

    /*
     * Instantiates ClientTransportBinding with the given objects and injected objects.
     */
    ClientTransportBinding create(HttpClient client, URI clientUri, SoapMarshalling marshalling, SoapUtil soapUtil);

}
