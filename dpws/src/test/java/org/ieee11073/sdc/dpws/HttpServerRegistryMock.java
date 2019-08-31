package org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpServerRegistryMock extends AbstractIdleService implements HttpServerRegistry {
    private static final Map<URI, HttpHandler> handlerRegistry = new HashMap<>();

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
    }

    public static Map<URI, HttpHandler> getRegistry() {
        return handlerRegistry;
    }

    @Override
    public URI initHttpServer(URI schemeAndAuthority) {
        return null;
    }

    @Override
    public URI registerContext(URI host, @Nullable String contextPath, HttpHandler handler) {
        URI uri = URI.create(host + contextPath);
        handlerRegistry.put(uri, handler);
        return uri;
    }

    @Override
    public URI registerContext(URI host, String contextPath, String mediaType, HttpHandler handler) {
        URI uri = URI.create(host + contextPath);
        handlerRegistry.put(uri, handler);
        return uri;
    }

    @Override
    public void unregisterContext(URI host, String contextPath) {
        handlerRegistry.remove(host + contextPath);
    }
}
