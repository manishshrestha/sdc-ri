package it.org.somda.glue.consumer;

import com.google.inject.Injector;
import it.org.somda.glue.common.IntegrationTestPeer;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.factory.DpwsFrameworkFactory;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class TestSdcClient extends IntegrationTestPeer {
    private final Client client;
    private final SdcRemoteDevicesConnector connector;
    private DpwsFramework dpwsFramework;


    public TestSdcClient() {
        setupInjector(Collections.emptyList());

        final Injector injector = getInjector();
        this.client = injector.getInstance(Client.class);
        this.connector = injector.getInstance(SdcRemoteDevicesConnector.class);
    }

    public Client getClient() {
        return client;
    }

    public SdcRemoteDevicesConnector getConnector() {
        return connector;
    }

    @Override
    protected void startUp() throws SocketException {
        this.dpwsFramework = getInjector().getInstance(DpwsFrameworkFactory.class)
                .createDpwsFramework(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
