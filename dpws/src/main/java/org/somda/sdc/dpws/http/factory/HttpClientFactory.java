package org.somda.sdc.dpws.http.factory;

import org.somda.sdc.dpws.http.HttpClient;

public interface HttpClientFactory {

    /**
     * Creates an http client
     *
     * @return a transport binding bound to endpointUri.
     */
    HttpClient createHttpClient();

}
