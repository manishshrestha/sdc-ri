package org.somda.sdc.dpws.network;

import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsFramework;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalAddressResolverImplTest {

    @Test
    void testResolutionOnCorrectAdapter() throws IOException {
        // mock address for test
        var targetIp = "127.88.77.66";
        var targetInetAddr = InetAddress.getByName(targetIp);
        // get loopback iface
        var testIface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        var framework = mock(DpwsFramework.class);
        when(framework.getNetworkInterface()).thenReturn(Optional.of(testIface));

        // create dummy socket to connect to
        var resolver = new LocalAddressResolverImpl(framework);
        Optional<String> result;
        try (ServerSocket socket = new ServerSocket(0, 50, targetInetAddr)) {
            var targetPort = socket.getLocalPort();
            var targetAddress = String.format("http://%s:%s", targetIp, targetPort);
            result = resolver.getLocalAddress(targetAddress);
        }


        var validAddresses = testIface.getInterfaceAddresses().stream()
                .map(iface -> iface.getAddress().getHostAddress())
                .collect(Collectors.toList());

        assertTrue(result.isPresent());
        assertTrue(validAddresses.contains(result.get()));
    }

    @Test
    void testFailedResolution() throws IOException {
        // get loopback iface
        var testIface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        var framework = mock(DpwsFramework.class);
        when(framework.getNetworkInterface()).thenReturn(Optional.of(testIface));

        var targetAddress = "http://this.will.never.be.reachable.i.am.sureofit";

        var resolver = new LocalAddressResolverImpl(framework);
        Optional<String> result = resolver.getLocalAddress(targetAddress);

        assertTrue(result.isEmpty());
    }
}
