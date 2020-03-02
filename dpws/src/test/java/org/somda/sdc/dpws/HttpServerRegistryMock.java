package org.somda.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpServerRegistryMock extends AbstractIdleService implements HttpServerRegistry {
    private static final Map<String, HttpHandler> handlerRegistry = new HashMap<>();

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
    }

    public static Map<String, HttpHandler> getRegistry() {
        return handlerRegistry;
    }

    @Override
    public String initHttpServer(String schemeAndAuthority) {
        return null;
    }

    @Override
    public String registerContext(String host, @Nullable String contextPath, HttpHandler handler) {
        URI uri = URI.create(host + contextPath);
        handlerRegistry.put(uri.toString(), handler);
        return uri.toString();
    }

    @Override
    public String registerContext(String host, String contextPath, String mediaType, HttpHandler handler) {
        URI uri = URI.create(host + contextPath);
        handlerRegistry.put(uri.toString(), handler);
        return uri.toString();
    }

    @Override
    public void unregisterContext(String host, String contextPath) {
        handlerRegistry.remove(URI.create(host + contextPath).toString());
    }
}
