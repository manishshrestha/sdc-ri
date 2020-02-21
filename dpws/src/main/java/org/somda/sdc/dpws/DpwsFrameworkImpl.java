package org.somda.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.guice.AppDelayExecutor;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.helper.ExecutorWrapperService;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

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

    private final List<Service> registeredServices;
    private UdpBindingService udpBindingService;
    private boolean wasStarted;

    @Inject
    DpwsFrameworkImpl(@DiscoveryUdpQueue UdpMessageQueueService udpMessageQueueService,
                      UdpBindingServiceFactory udpBindingServiceFactory,
                      HttpServerRegistry httpServerRegistry,
                      SoapMarshalling soapMarshalling,
                      @AppDelayExecutor ExecutorWrapperService<ScheduledExecutorService> appDelayExecutor,
                      @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> networkJobExecutor,
                      @WsDiscovery ExecutorWrapperService<ListeningExecutorService> wsDiscoveryExecutor) {
        this.udpMessageQueueService = udpMessageQueueService;
        this.udpBindingServiceFactory = udpBindingServiceFactory;
        this.httpServerRegistry = httpServerRegistry;
        this.soapMarshalling = soapMarshalling;
        this.wasStarted = false;
        this.registeredServices = new ArrayList<>();
        registeredServices.addAll(List.of(
                // dpws thread pools
                appDelayExecutor, networkJobExecutor, wsDiscoveryExecutor
        ));
    }

    @Override
    protected void startUp() throws SocketException, UnknownHostException {
        if (!isRunning() && wasStarted) {
            LOG.error("DPWS framework cannot be restarted after a shutdown!");
            throw new RuntimeException("DPWS framework cannot be restarted after a shutdown!");
        }
        LOG.info("Start SDCri DPWS framework");

        if (networkInterface == null) {
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
            LOG.info("Initializing dpws framework with loopback interface {}", networkInterface);
        }

        printNetworkInterfaceInformation();

        configureDiscovery();
        registeredServices.addAll(List.of(
                // dpws services
                udpBindingService, udpMessageQueueService, httpServerRegistry, soapMarshalling
        ));
        registeredServices.forEach(service -> service.startAsync().awaitRunning());
        this.wasStarted = true;
        LOG.info("SDCri DPWS framework is ready for use");
    }

    private void printNetworkInterfaceInformation() throws SocketException {
        Iterator<NetworkInterface> networkInterfaceIterator = NetworkInterface.getNetworkInterfaces().asIterator();
        while (networkInterfaceIterator.hasNext()) {
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
            while (inetAddressIterator.hasNext()) {
                LOG.info("{}.address[{}]: {}", networkInterface.getName(), i++, inetAddressIterator.next());
            }
        }
    }

    @Override
    protected void shutDown() {
        LOG.info("Shut down SDCri DPWS framework");
        registeredServices.forEach(service -> service.stopAsync().awaitTerminated());
        LOG.info("SDCri DPWS framework shut down");
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

    public DpwsFramework setNetworkInterface(NetworkInterface networkInterface) {
        if (isRunning()) {
            LOG.warn("Framework is already running, cannot change network interface");
            return this;
        }
        this.networkInterface = networkInterface;
        return this;
    }

    public void registerService(Collection<Service> services) {
        // don't add any duplicates
        services.forEach(
                service -> {
                    if (!registeredServices.contains(service)) {
                        registeredServices.add(service);
                    }
                }
        );

        // if we're already running, we need to start the service now
        if (isRunning() || state().equals(State.STARTING)) {
            services.forEach(
                    service -> {
                        if (!isRunning()) {
                            LOG.info("Delayed start of service {}", service);
                            service.startAsync().awaitRunning();
                        }
                    }
            );
        }
    }
}
