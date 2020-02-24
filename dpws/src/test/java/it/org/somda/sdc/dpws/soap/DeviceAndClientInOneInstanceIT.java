package it.org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;
import test.org.somda.common.TestLogging;

import java.net.URI;

@ExtendWith(LoggingTestWatcher.class)
public class DeviceAndClientInOneInstanceIT {
    // private final IntegrationTestUtil IT = new IntegrationTestUtil();
    private URI eprAddress1;
    private URI eprAddress2;
    private DeviceAndClientPeer peer1;
    private DeviceAndClientPeer peer2;

    @BeforeAll
    static void beforeAll() {
        TestLogging.configure();
    }

    @BeforeEach
    void beforeEach() {
        eprAddress1 = URI.create("urn:uuid:11111111-1111-1111-1111-111111111111");
        eprAddress2 = URI.create("urn:uuid:22222222-2222-2222-2222-222222222222");
        peer1 = new DeviceAndClientPeer(eprAddress1);
        peer2 = new DeviceAndClientPeer(eprAddress2);
    }

    @Test
    void crossConnectWithRequestResponseAndNotification() throws Exception {
        peer1.startAsync().awaitRunning();
        peer2.startAsync().awaitRunning();

        peer1.startInteraction(eprAddress2);
        peer2.startInteraction(eprAddress1);

        peer1.stopAsync().awaitTerminated();
        peer2.stopAsync().awaitTerminated();
    }
}
