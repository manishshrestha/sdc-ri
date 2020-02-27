package it.org.somda.sdc.dpws;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerUtil {

    public static HttpServer spawnHttpServer(InetSocketAddress addr, HttpHandler httpHandler) throws IOException {
        HttpServer server = HttpServer.create(addr, 0);
        server.createContext("/", httpHandler);
        server.setExecutor(null); // creates a default executor
        server.start();
        return server;
    }
}
