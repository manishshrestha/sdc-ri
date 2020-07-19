package it.org.somda.sdc.proto.discovery;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;
import it.org.somda.sdc.proto.IntegrationTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.proto.discovery.consumer.Client;
import org.somda.sdc.proto.discovery.consumer.event.DeviceEnteredMessage;
import org.somda.sdc.proto.discovery.provider.factory.TargetServiceFactory;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;
import test.org.somda.common.LoggingTestWatcher;
import test.org.somda.common.TimedWait;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class DiscoveryIT {
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();
    private Injector injector;

    @BeforeEach
    void beforeEach() {
        this.injector = IT.getInjector();
    }

    @Test
    @DisplayName("Hello")
    void testHello() throws Exception {
        var epr = injector.getInstance(SoapUtil.class).createRandomUuidUri();
        var targetService = injector.getInstance(TargetServiceFactory.class).create(epr);
        var client = injector.getInstance(Client.class);
        var observer = new DiscoveryObserver();
        var dpwsFramework = injector.getInstance(DpwsFramework.class);

        dpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();


        client.registerObserver(observer);
        targetService.startAsync().awaitRunning();
        assertTrue(observer.waitForMessages(1, Duration.ofSeconds(10)));
        assertEquals(epr, observer.timedWait.getData().get(0).getEndpointReference().getAddress());
    }

    @Test
    @DisplayName("Probe")
    void testProbe() {

    }

    @Test
    @DisplayName("Resolve")
    void testResolve() {

    }

    private class DiscoveryObserver implements org.somda.sdc.proto.discovery.consumer.DiscoveryObserver {
        TimedWait<List<DiscoveryTypes.Endpoint>> timedWait = new TimedWait<>(ArrayList::new);

        @Subscribe
        void onEnteredDevice(DeviceEnteredMessage message) {
            timedWait.modifyData(testNotifications -> testNotifications.add(message.getPayload()));
        }

        boolean waitForMessages(int messageCount, Duration wait) {
            return timedWait.waitForData(notifications -> notifications.size() >= messageCount, wait);
        }
    }
}
