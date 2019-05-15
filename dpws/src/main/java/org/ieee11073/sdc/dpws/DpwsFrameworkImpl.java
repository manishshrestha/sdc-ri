package org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.soap.SoapMarshalling;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.ieee11073.sdc.dpws.udp.UdpBindingService;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Default implementation of {@link DpwsFramework}.
 */
public class DpwsFrameworkImpl extends AbstractIdleService implements DpwsFramework {
    private static final Logger LOG = LoggerFactory.getLogger(DpwsFrameworkImpl.class);

    private final UdpMessageQueueService udpMessageQueueService;
    private final UdpBindingServiceFactory udpBindingServiceFactory;
    private final HttpServerRegistry httpServerRegistry;
    private final SoapMarshalling soapMarshalling;

    private ServiceManager serviceManager;
    private UdpBindingService udpBindingService;

    @Inject
    DpwsFrameworkImpl(@DiscoveryUdpQueue UdpMessageQueueService udpMessageQueueService,
                      UdpBindingServiceFactory udpBindingServiceFactory,
                      HttpServerRegistry httpServerRegistry,
                      SoapMarshalling soapMarshalling) {
        this.udpMessageQueueService = udpMessageQueueService;
        this.udpBindingServiceFactory = udpBindingServiceFactory;
        this.httpServerRegistry = httpServerRegistry;
        this.soapMarshalling = soapMarshalling;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Start SDC reference implementation DPWS framework.");
        configureDiscovery();
        serviceManager = new ServiceManager(Arrays.asList(udpBindingService, udpMessageQueueService,
                httpServerRegistry, soapMarshalling));
        serviceManager.startAsync().awaitHealthy();
        LOG.info("easySDC DPWS framework is ready for use.");
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down easySDC DPWS framework.");
        serviceManager.stopAsync().awaitStopped();
        LOG.info("easySDC DPWS framework shut down.");
    }

    private void configureDiscovery() {
        InetAddress wsdMulticastAddress;
        try {
            wsdMulticastAddress = InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS);
        } catch (UnknownHostException e) {
            LOG.warn("WS-Discovery multicast port could not be retrieved as InetAddress: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        udpBindingService = udpBindingServiceFactory.createUdpBindingService(wsdMulticastAddress,
                DpwsConstants.DISCOVERY_PORT, DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        udpMessageQueueService.setUdpBinding(udpBindingService);
    }
}
