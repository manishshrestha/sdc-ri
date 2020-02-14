package it.org.somda.glue.provider;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import it.org.somda.glue.common.IntegrationTestPeer;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.factory.DpwsFrameworkFactory;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import test.org.somda.common.CIDetector;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestSdcDevice extends IntegrationTestPeer {
    private static final Logger LOG = LoggerFactory.getLogger(TestSdcDevice.class);

    private DpwsFramework dpwsFramework;
    private final SdcDevice sdcDevice;

    public TestSdcDevice(Collection<OperationInvocationReceiver> operationInvocationReceivers) {
        setupInjector(List.of(
                new MockedUdpBindingModule(),
                new GlueDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        super.customConfigure();

                        // bump network pool size because of parallelism tests
                        bind(ListeningExecutorService.class)
                                .annotatedWith(NetworkJobThreadPool.class)
                                .toInstance(MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                        30,
                                        new ThreadFactoryBuilder()
                                                .setNameFormat("NetworkJobThreadPool-thread-%d")
                                                .setDaemon(true)
                                                .build()
                                )));

                        bind(ExecutorService.class)
                                .annotatedWith(WsDiscovery.class)
                                .toInstance(Executors.newFixedThreadPool(
                                        30,
                                        new ThreadFactoryBuilder()
                                                .setNameFormat("WsDiscovery-thread-%d")
                                                .setDaemon(true)
                                                .build()
                                ));

                        if (CIDetector.isRunningInCi()) {
                            var httpTimeouts = Duration.ofSeconds(120);
                            var futureTimeouts = Duration.ofSeconds(30);
                            LOG.info("CI detected, setting relaxed http client timeouts of {}s",
                                    httpTimeouts.toSeconds());
                            bind(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT,
                                    Duration.class,
                                    httpTimeouts);

                            bind(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT,
                                    Duration.class,
                                    httpTimeouts);

                            LOG.info("CI detected, setting {} to {}s", DpwsConfig.MAX_WAIT_FOR_FUTURES, futureTimeouts.toSeconds());
                            bind(DpwsConfig.MAX_WAIT_FOR_FUTURES,
                                    Duration.class,
                                    futureTimeouts);

                        }
                    }
                }
        ));

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
                operationInvocationReceivers);
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
