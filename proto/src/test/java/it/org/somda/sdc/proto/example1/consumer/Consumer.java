package it.org.somda.sdc.proto.example1.consumer;

import com.google.common.collect.Streams;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.udp.UdpMessageQueueServiceImpl;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.somda.sdc.proto.consumer.ConnectConfiguration;
import org.somda.sdc.proto.consumer.PrerequisitesException;
import org.somda.sdc.proto.consumer.SdcRemoteDevice;
import org.somda.sdc.proto.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.proto.discovery.consumer.Client;
import org.somda.sdc.proto.discovery.consumer.DiscoveryObserver;
import org.somda.sdc.proto.discovery.consumer.event.ProbedDeviceFoundMessage;
import org.somda.sdc.proto.model.ActionFilter;
import org.somda.sdc.proto.model.EpisodicReportRequest;
import org.somda.sdc.proto.model.EpisodicReportStream;
import org.somda.sdc.proto.model.Filter;
import org.somda.sdc.proto.model.GetMdibRequest;
import org.somda.sdc.proto.model.discovery.Endpoint;
import org.somda.sdc.proto.model.discovery.ScopeMatcher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.somda.sdc.proto.consumer.ConnectConfiguration.EPISODIC_REPORTS;
import static org.somda.sdc.proto.consumer.ConnectConfiguration.STREAMING_REPORTS;

public class Consumer extends AbstractIdleService {

    private static final Logger LOG = LogManager.getLogger(Consumer.class);
    private static final Duration MAX_WAIT = Duration.ofSeconds(11);
    private static final long MAX_WAIT_SEC = MAX_WAIT.getSeconds();

    private static final long REPORT_TIMEOUT = Duration.ofSeconds(30).toMillis();
    private final ConsumerUtil consumerUtil;
    private final Injector injector;
    private final SdcRemoteDevicesConnector connector;
    private final org.somda.sdc.proto.consumer.Consumer consumer;
    private final Client discoveryClient;
    private final UdpMessageQueueService udpQueue;
    private final UdpBindingService udpBindingService;

    /**
     * Creates an SDC Consumer instance.
     *
     * @param consumerUtil utility containing injector and settings
     * @throws SocketException      if network adapter couldn't be bound
     * @throws UnknownHostException if localhost couldn't be determined
     */
    public Consumer(ConsumerUtil consumerUtil) throws Exception {
        this.consumerUtil = consumerUtil;
        this.injector = consumerUtil.getInjector();
        this.consumer = injector.getInstance(org.somda.sdc.proto.consumer.Consumer.class);
        this.connector = injector.getInstance(SdcRemoteDevicesConnector.class);
        this.discoveryClient = injector.getInstance(Client.class);
        var serverAddr = new InetSocketAddress("127.0.0.1", 13373);
        var epr = "urn:uuid:" + UUID.randomUUID().toString();
        var networkInterface = NetworkInterface.networkInterfaces()
                .filter(iface -> Streams.stream(iface.getInetAddresses().asIterator())
                        .map(InetAddress::getHostAddress)
                                .anyMatch(addr -> addr.contains(serverAddr.getAddress().getHostAddress()))
                ).findFirst().orElseThrow(Exception::new);

        udpQueue = injector.getInstance(Key.get(UdpMessageQueueService.class, DiscoveryUdpQueue.class));

        var wsdMulticastAddress = InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS);

        udpBindingService = injector.getInstance(UdpBindingServiceFactory.class).createUdpBindingService(
                networkInterface,
                wsdMulticastAddress,
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        udpQueue.setUdpBinding(udpBindingService);
        udpBindingService.setMessageReceiver(udpQueue);
    }

    SdcRemoteDevice connect() throws PrerequisitesException, ExecutionException, InterruptedException {
        var fut = connector.connect(consumer, ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));
        return fut.get();
    }

    public org.somda.sdc.proto.consumer.Consumer getConsumer() {
        return consumer;
    }

    public Client getDiscoveryClient() {
        return discoveryClient;
    }

    @Override
    protected void startUp() throws Exception {
        discoveryClient.startAsync().awaitRunning();
        udpQueue.startAsync().awaitRunning();
        udpBindingService.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        discoveryClient.stopAsync().awaitTerminated();
        udpQueue.stopAsync().awaitTerminated();
        udpBindingService.stopAsync().awaitTerminated();
    }

    public static void runConsumer() throws Exception {
        var settings = new ConsumerUtil();

        // TODO: Unmagic me.
        var targetEpr = "urn:uuid:d4e63551-8546-492c-bead-ae764dad89f6";

        var consumer = new Consumer(settings);
        consumer.startAsync().awaitRunning();


        // see if device using the provided epr address is available
        LOG.info("Starting discovery for {}", targetEpr);
        final SettableFuture<Endpoint> endpointSettableFuture = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                message.getPayload().forEach(endpoint -> {
                    if (endpoint.hasEndpointReference()) {
                        if (targetEpr.equals(endpoint.getEndpointReference().getAddress())) {
                            LOG.info("Found device with epr {}", targetEpr);
                            endpointSettableFuture.set(endpoint);
                        } else {
                            LOG.info("Found non-matching device with epr {}", endpoint.getEndpointReference().getAddress());
                        }
                    }
                });
            }
        };

        var discovery = consumer.getDiscoveryClient();
        discovery.registerObserver(obs);

        consumer.getDiscoveryClient().probe(ScopeMatcher.newBuilder().build(), 1);

        Endpoint endpoint = null;
        try {
            endpoint = endpointSettableFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Couldn't find target with EPR {}", targetEpr, e);
            System.exit(1);
        }
        consumer.getDiscoveryClient().unregisterObserver(obs);
        consumer.getDiscoveryClient().stopAsync().awaitTerminated();

        LOG.info("Connecting to {} at ", targetEpr, endpoint);
        consumer.getConsumer().connect(endpoint);



        var mdib = consumer.getConsumer().getGetService().get().getMdib(GetMdibRequest.newBuilder().build());

        assert mdib != null;

        var reporting = consumer.getConsumer().getMdibReportingService().get();
        var requested_subs = EpisodicReportRequest.newBuilder().setFilter(
                Filter.newBuilder().setActionFilter(ActionFilter.newBuilder()
                        .addAllAction(EPISODIC_REPORTS)
                        .addAllAction(STREAMING_REPORTS)
                        .build()
                ).build()
        ).build();

        reporting.episodicReport(requested_subs, new StreamObserver<EpisodicReportStream>() {
            @Override
            public void onNext(final EpisodicReportStream value) {
                LOG.info("new report:\n{}", value);
            }

            @Override
            public void onError(final Throwable t) {
                LOG.error("Observer died", t);
            }

            @Override
            public void onCompleted() {
                LOG.info("Observer finished");
            }
        });

        LOG.info("Mdib:\n{}", mdib);

        Thread.sleep(10000);
        consumer.stopAsync().awaitTerminated();
    }

    public static void main(String[] args) throws Exception {
        runConsumer();
    }

}
