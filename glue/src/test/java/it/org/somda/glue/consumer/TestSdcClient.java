package it.org.somda.glue.consumer;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import it.org.somda.glue.common.IntegrationTestPeer;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.common.util.ExecutorWrapperUtil;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.glue.consumer.ConsumerConfig;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;
import test.org.somda.common.CIDetector;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class TestSdcClient extends IntegrationTestPeer {
    private static final Logger LOG = LoggerFactory.getLogger(TestSdcClient.class);
    public static final Duration REQUESTED_EXPIRES = Duration.ofSeconds(20);

    private final Client client;
    private final SdcRemoteDevicesConnector connector;
    private DpwsFramework dpwsFramework;


    public TestSdcClient() {
        setupInjector(List.of(
                new MockedUdpBindingModule(),
                new GlueDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        super.customConfigure();

                        bind(ConsumerConfig.REQUESTED_EXPIRES,
                                Duration.class,
                                REQUESTED_EXPIRES);

                        if (CIDetector.isRunningInCi()) {
                            var httpTimeouts = Duration.ofSeconds(120);
                            var futureTimeouts = Duration.ofSeconds(30);
                            LOG.info("CI detected, setting relaxed HTTP client timeouts of {}s",
                                    httpTimeouts.toSeconds());
                            bind(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT,
                                    Duration.class,
                                    httpTimeouts);

                            bind(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT,
                                    Duration.class,
                                    httpTimeouts);

                            List<String> increasedTimeouts = List.of(
                                    DpwsConfig.MAX_WAIT_FOR_FUTURES,
                                    WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES,
                                    WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES
                            );
                            increasedTimeouts.forEach(item -> {
                                LOG.info("CI detected, setting {} to {}s", item, futureTimeouts.toSeconds());
                                bind(item,
                                        Duration.class,
                                        futureTimeouts);
                            });
                        }
                    }
                },
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        super.configure();
                        // bump network pool size because of parallelism tests
                        {
                            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                    30,
                                    new ThreadFactoryBuilder()
                                            .setNameFormat("NetworkJobThreadPool-thread-%d")
                                            .setDaemon(true)
                                            .build()
                            ));
                            ExecutorWrapperUtil.bindListeningExecutor(this, executor, NetworkJobThreadPool.class);
                        }
                        {
                            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                    30,
                                    new ThreadFactoryBuilder()
                                            .setNameFormat("WsDiscovery-thread-%d")
                                            .setDaemon(true)
                                            .build()
                            ));
                            ExecutorWrapperUtil.bindListeningExecutor(this, executor, WsDiscovery.class);
                        }
                    }
                }

        ));

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
        this.dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        this.dpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
