package it.org.ieee11073.sdc.dpws.soap;

import com.google.inject.AbstractModule;
import it.org.ieee11073.sdc.dpws.IntegrationTestPeer;
import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.client.Client;
import org.ieee11073.sdc.dpws.factory.DpwsFrameworkFactory;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;

import javax.annotation.Nullable;

public class ClientPeer extends IntegrationTestPeer {
    private final Client client;
    private final DpwsFramework dpwsFramework;

    public ClientPeer(DefaultDpwsConfigModule configModule) {
        this(configModule, null);
    }

    public ClientPeer(DefaultDpwsConfigModule configModule, @Nullable AbstractModule overridingModule) {
        setupInjector(configModule, overridingModule);
        this.dpwsFramework = getInjector().getInstance(DpwsFrameworkFactory.class).createDpwsFramework();
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
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
