package org.somda.sdc.dpws.http.jetty.factory;

import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;

public interface JettyHttpServerHandlerFactory {
    
    JettyHttpServerHandler create(String mediaType, HttpHandler handler);

}
