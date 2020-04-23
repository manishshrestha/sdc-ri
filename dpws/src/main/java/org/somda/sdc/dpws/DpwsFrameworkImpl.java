package org.somda.sdc.dpws;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.guice.AppDelayExecutor;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
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
    private static final Logger LOG = LogManager.getLogger(DpwsFrameworkImpl.class);

    private NetworkInterface networkInterface;
    private final UdpMessageQueueService udpMessageQueueService;
    private final UdpBindingServiceFactory udpBindingServiceFactory;
    private final HttpServerRegistry httpServerRegistry;
    private final SoapMarshalling soapMarshalling;
    private final FrameworkMetadata metadata;

    private final List<Service> registeredServices;
    private UdpBindingService udpBindingService;

    @Inject
    DpwsFrameworkImpl(@DiscoveryUdpQueue UdpMessageQueueService udpMessageQueueService,
                      UdpBindingServiceFactory udpBindingServiceFactory,
                      HttpServerRegistry httpServerRegistry,
                      SoapMarshalling soapMarshalling,
                      @AppDelayExecutor ExecutorWrapperService<ScheduledExecutorService> appDelayExecutor,
                      @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> networkJobExecutor,
                      @WsDiscovery ExecutorWrapperService<ListeningExecutorService> wsDiscoveryExecutor,
                      FrameworkMetadata metadata) {
        this.udpMessageQueueService = udpMessageQueueService;
        this.udpBindingServiceFactory = udpBindingServiceFactory;
        this.httpServerRegistry = httpServerRegistry;
        this.soapMarshalling = soapMarshalling;
        this.metadata = metadata;
        this.registeredServices = new ArrayList<>();
        registeredServices.addAll(List.of(
                // dpws thread pools
                appDelayExecutor, networkJobExecutor, wsDiscoveryExecutor,
                // dpws services
                this.soapMarshalling, this.httpServerRegistry
        ));
    }

    @Override
    protected void startUp() throws SocketException, UnknownHostException {
        LOG.info("Start SDCri DPWS framework");
        logMetadata();

        if (networkInterface == null) {
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
            LOG.info("Initializing dpws framework with loopback interface {}", networkInterface);
        }

        printNetworkInterfaceInformation();

        configureDiscovery();
        registeredServices.addAll(List.of(
                // remaining dpws services which depend on the network interface being set,
                // which is why they are added delayed
                udpBindingService, udpMessageQueueService
        ));
        registeredServices.forEach(service -> service.startAsync().awaitRunning());
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
        LOG.info("Shutting down SDCri DPWS framework");
        Lists.reverse(registeredServices).forEach(service -> service.stopAsync().awaitTerminated());
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

    public void setNetworkInterface(NetworkInterface networkInterface) {
        if (isRunning()) {
            LOG.warn("Framework is already running, cannot change network interface");
            return;
        }
        this.networkInterface = networkInterface;
    }

    synchronized public void registerService(Collection<Service> services) {
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
                        if (service.state().equals(State.NEW)) {
                            LOG.info("Delayed start of service {}", service);
                            service.startAsync().awaitRunning();
                        } else {
                            service.awaitRunning();
                        }
                    }
            );
        }
    }

    private void logMetadata() {
        LOG.info("SDCri version:\t{}", metadata.getFrameworkVersion());
        LOG.info("Java vendor:\t\t{}", metadata.getJavaVendor());
        LOG.info("Java version:\t{}", metadata.getJavaVersion());
        LOG.info("OS version:\t\t{}", metadata.getOsVersion());
    }

}
