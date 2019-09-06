package org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.soap.SoapMarshalling;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.ieee11073.sdc.dpws.udp.UdpBindingService;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Default implementation of {@link DpwsFramework}.
 */
public class DpwsFrameworkImpl extends AbstractIdleService implements DpwsFramework {
    private static final Logger LOG = LoggerFactory.getLogger(DpwsFrameworkImpl.class);

    private NetworkInterface networkInterface;
    private final UdpMessageQueueService udpMessageQueueService;
    private final UdpBindingServiceFactory udpBindingServiceFactory;
    private final HttpServerRegistry httpServerRegistry;
    private final SoapMarshalling soapMarshalling;

    private ServiceManager serviceManager;
    private UdpBindingService udpBindingService;

    @AssistedInject
    DpwsFrameworkImpl(@DiscoveryUdpQueue UdpMessageQueueService udpMessageQueueService,
                      UdpBindingServiceFactory udpBindingServiceFactory,
                      HttpServerRegistry httpServerRegistry,
                      SoapMarshalling soapMarshalling) {
        this(null, udpMessageQueueService, udpBindingServiceFactory, httpServerRegistry, soapMarshalling);
    }

    @AssistedInject
    DpwsFrameworkImpl(@Assisted @Nullable NetworkInterface networkInterface,
                      @DiscoveryUdpQueue UdpMessageQueueService udpMessageQueueService,
                      UdpBindingServiceFactory udpBindingServiceFactory,
                      HttpServerRegistry httpServerRegistry,
                      SoapMarshalling soapMarshalling) {
        this.networkInterface = networkInterface;
        this.udpMessageQueueService = udpMessageQueueService;
        this.udpBindingServiceFactory = udpBindingServiceFactory;
        this.httpServerRegistry = httpServerRegistry;
        this.soapMarshalling = soapMarshalling;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Start SDCri DPWS framework.");

        if (networkInterface == null) {
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        }

        printNetworkInterfaceInformation();

        configureDiscovery();
        serviceManager = new ServiceManager(Arrays.asList(udpBindingService, udpMessageQueueService,
                httpServerRegistry, soapMarshalling));
        serviceManager.startAsync().awaitHealthy();
        LOG.info("SDCri DPWS framework is ready for use.");
    }

    private void printNetworkInterfaceInformation() throws SocketException {
        Iterator<NetworkInterface> networkInterfaceIterator = NetworkInterface.getNetworkInterfaces().asIterator();
        while (networkInterfaceIterator.hasNext())
        {
            NetworkInterface networkInterface = networkInterfaceIterator.next();
            LOG.info("Found network interface: [{};isUp={};isLoopBack={},supportsMulticast={},MTU={},isVirtual={}]",
                    networkInterface,
                    networkInterface.isUp(),
                    networkInterface.isLoopback(),
                    networkInterface.supportsMulticast(),
                    networkInterface.getMTU(),
                    networkInterface.isVirtual());
            Iterator<InetAddress> inetAddressIterator = networkInterface.getInetAddresses().asIterator();
            int i = 0;
            while (inetAddressIterator.hasNext())
            {
                LOG.info("{}.address[{}]: {}", networkInterface.getName(), i++, inetAddressIterator.next());
            }
        }
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down SDCri DPWS framework.");
        serviceManager.stopAsync().awaitStopped();
        LOG.info("SDCri DPWS framework shut down.");
    }

    private void configureDiscovery() throws UnknownHostException, SocketException {
        InetAddress wsdMulticastAddress;
        try {
            wsdMulticastAddress = InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS);
        } catch (UnknownHostException e) {
            LOG.warn("WS-Discovery multicast port could not be retrieved as InetAddress: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        udpBindingService = udpBindingServiceFactory.createUdpBindingService(
                networkInterface,
                wsdMulticastAddress,
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        udpMessageQueueService.setUdpBinding(udpBindingService);
        udpBindingService.setMessageReceiver(udpMessageQueueService);
    }
}
