package it.org.ieee11073.sdc.dpws.soap;

import it.org.ieee11073.sdc.dpws.IntegrationTestPeer;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.client.Client;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.soap.SoapConfig;

public class ClientPeer extends IntegrationTestPeer {
    Client client;

    public ClientPeer() {
        this(new DefaultDpwsConfigModule() {
            @Override
            protected void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        });
    }

    public ClientPeer(DefaultDpwsConfigModule configModule) {
        super(configModule);
        this.client = getInjector().getInstance(Client.class);
    }

    public Client getClient() {
        if (!this.isRunning()) {
            throw new RuntimeException("ClientPeer is not running");
        }
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
    protected void shutDown() {
        client.stopAsync().awaitTerminated();
    }
}
