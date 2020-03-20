package it.org.somda.glue;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.sdc.dpws.MemoryCommunicationLog;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.guice.AppDelayExecutor;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnectorImpl;
import org.somda.sdc.glue.guice.Consumer;
import org.somda.sdc.glue.guice.WatchdogScheduledExecutor;
import test.org.somda.common.CIDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ShutdownIT {
    private static final Logger LOG = LoggerFactory.getLogger(ShutdownIT.class);
    private static Duration WAIT_TIME = Duration.ofSeconds(30);

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_TIME = Duration.ofSeconds(180);
            LOG.info("CI detected, setting WAIT_TIME to {}s", WAIT_TIME.getSeconds());
        }
    }

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;
    private MemoryCommunicationLog commLog;

    @BeforeEach
    void setUp() {
        commLog = new MemoryCommunicationLog();

        testDevice = new TestSdcDevice(Collections.emptyList());
        testClient = new TestSdcClient(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        super.configure();
                        bind(CommunicationLog.class).toInstance(commLog);
                        // make this a singleton to allow verifying the running state later
                        bind(SdcRemoteDevicesConnector.class)
                                .to(SdcRemoteDevicesConnectorImpl.class).in(Singleton.class);
                    }
                }
        );

    }

    @AfterEach
    void tearDown() throws Exception {
        if (testClient.isRunning()) {
            testClient.stopAsync().awaitTerminated(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        }
        if (testDevice.isRunning()) {
            testDevice.stopAsync().awaitTerminated(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Test
    void testConsumerDisconnectOnShutdown() throws Exception {
        testDevice.startAsync().awaitRunning(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        testClient.startAsync().awaitRunning(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        var hostingServiceProxy = hostingServiceFuture.get(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_PERIODIC_AND_WAVEFORM_REPORTS));

        var sdcRemoteDevice = remoteDeviceFuture.get(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        assertNotNull(testClient.getConnector().getConnectedDevice(testDevice.getSdcDevice().getEprAddress()));
        assertFalse(testDevice.getSdcDevice().getActiveSubscriptions().isEmpty());

        Thread.sleep(1000);

        testClient.stopAsync().awaitTerminated(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        assertTrue(testClient.getConnector().getConnectedDevices().isEmpty());

        var clientMessages = commLog.getMessages();

        assertFalse(clientMessages.isEmpty());

        var lastMessage = clientMessages.get(clientMessages.size() - 1);

        // the last message must be the unsubscribe response
        assertTrue(lastMessage.getMessage().contains("/UnsubscribeResponse"));

        /*
         * collect all services to check their state is terminated
         */
        var services = new ArrayList<Service>();

        services.add(sdcRemoteDevice);
        // scheduled
        List.of(AppDelayExecutor.class,
                WatchdogScheduledExecutor.class)
                .forEach(service ->
                        services.add(testClient.getInjector().getInstance(Key.get(
                                new TypeLiteral<ExecutorWrapperService<ScheduledExecutorService>>() {
                                },
                                service
                                ))
                        )
                );

        // listening
        List.of(NetworkJobThreadPool.class,
                WsDiscovery.class,
                Consumer.class)
                .forEach(service ->
                        services.add(testClient.getInjector().getInstance(Key.get(
                                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                                },
                                service
                                ))
                        )
                );

        // Collect all additional services
        List.of(SoapMarshalling.class,
                HttpServerRegistry.class,
                SdcRemoteDevicesConnector.class)
                .forEach(serviceClass -> services.add(testClient.getInjector().getInstance(serviceClass)));


        services.forEach(service -> {
            assertEquals(Service.State.TERMINATED, service.state(), service.toString() + " did not shut down");
        });
    }
}
