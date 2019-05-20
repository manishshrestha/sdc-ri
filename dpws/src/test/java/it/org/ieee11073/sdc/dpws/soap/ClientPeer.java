package it.org.ieee11073.sdc.dpws.soap;

import it.org.ieee11073.sdc.dpws.IntegrationTestPeer;
import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.client.Client;

public class ClientPeer extends IntegrationTestPeer {
    Client client;

    public ClientPeer() {
        this.client = getInjector().getInstance(Client.class);
    }

    public Client getClient() {
        return client;
    }

    @Override
    protected void startUp() {
        DpwsFramework dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        dpwsFramework.startAsync().awaitRunning();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> dpwsFramework.stopAsync().awaitTerminated()));

        client.startAsync().awaitRunning();
    }

    @Override
    protected void run() throws Exception {
        while (!Thread.interrupted()) {
            Thread.sleep(1000);
        }
    }

    @Override
    protected void shutDown() {
        client.stopAsync().awaitTerminated();
    }
}
