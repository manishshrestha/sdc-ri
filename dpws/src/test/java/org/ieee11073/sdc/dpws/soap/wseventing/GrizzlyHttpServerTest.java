package org.ieee11073.sdc.dpws.soap.wseventing;

import org.glassfish.grizzly.http.server.*;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.io.IOException;
import java.util.Collection;

public class GrizzlyHttpServerTest {
    public static void main(String[] args) {
        HttpServer simpleServer = HttpServer.createSimpleServer();
        simpleServer.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                System.out.println("Fetch request");
                response.setStatus(HttpStatus.OK_200);
            }
        });

        Collection<NetworkListener> listeners = simpleServer.getListeners();
        listeners.forEach(networkListener -> System.out.println(networkListener.getName()));

        try {
            simpleServer.start();
            System.out.println(simpleServer.getServerConfiguration().getScheme());
            System.in.read();
            simpleServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
