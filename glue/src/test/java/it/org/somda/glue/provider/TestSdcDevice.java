package it.org.somda.glue.provider;

import com.google.inject.Injector;
import it.org.somda.glue.common.IntegrationTestPeer;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.factory.DpwsFrameworkFactory;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class TestSdcDevice extends IntegrationTestPeer {
    private DpwsFramework dpwsFramework;
    private final SdcDevice sdcDevice;

    public TestSdcDevice(Collection<OperationInvocationReceiver> operationInvocationReceivers) {
        setupInjector(Collections.emptyList());

        final Injector injector = getInjector();
        final URI eprAddress = injector.getInstance(SoapUtil.class).createUriFromUuid(UUID.randomUUID());
        final WsAddressingUtil wsaUtil = injector.getInstance(WsAddressingUtil.class);
        final EndpointReferenceType epr = wsaUtil.createEprWithAddress(eprAddress);
        final DeviceSettings deviceSettings = new DeviceSettings() {
            @Override
            public EndpointReferenceType getEndpointReference() {
                return epr;
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                try {
                    return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        this.sdcDevice = injector.getInstance(SdcDeviceFactory.class).createSdcDevice(
                deviceSettings,
                injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess(),
                operationInvocationReceivers,
                Collections.emptyList());
    }

    public TestSdcDevice() {
        this(Collections.emptyList());
    }

    @Override
    protected void startUp() throws SocketException {
        this.dpwsFramework = getInjector().getInstance(DpwsFrameworkFactory.class)
                .createDpwsFramework(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();
        sdcDevice.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        sdcDevice.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }

    public SdcDevice getSdcDevice() {
        return sdcDevice;
    }
}
