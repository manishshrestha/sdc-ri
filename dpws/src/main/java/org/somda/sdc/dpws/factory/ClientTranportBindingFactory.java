package org.somda.sdc.dpws.factory;

import java.net.URI;

import org.apache.http.client.HttpClient;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;

public interface ClientTranportBindingFactory {

    ClientTransportBinding create(HttpClient client, URI clientUri, SoapMarshalling marshalling, SoapUtil soapUtil);

}
