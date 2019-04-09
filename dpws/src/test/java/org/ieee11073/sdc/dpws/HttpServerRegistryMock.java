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
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    public static Map<URI, HttpHandler> getRegistry() {
        return handlerRegistry;
    }

    @Override
    public URI registerContext(String host, @Nullable Integer port, String contextPath, HttpHandler handler) {
        URI uri = URI.create("http://" + host + ":" + port + contextPath);
        handlerRegistry.put(uri, handler);
        return uri;
    }

    @Override
    public URI registerContext(String host, Integer port, String contextPath, String mediaType, HttpHandler handler) {
        URI uri = URI.create("http://" + host + ":" + port + contextPath);
        handlerRegistry.put(uri, handler);
        return uri;
    }

    @Override
    public void unregisterContext(String host, Integer port, String contextPath) {
        handlerRegistry.remove(URI.create("http://" + host + ":" + port + contextPath));
    }
}
