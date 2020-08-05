package it.org.somda.sdc.proto.example1.consumer;

import com.example.Constants;
import com.google.common.collect.Streams;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.biceps.model.message.SetStringResponse;
import org.somda.sdc.biceps.model.message.SetValue;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.somda.sdc.proto.consumer.ConnectConfiguration;
import org.somda.sdc.proto.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.proto.consumer.SetServiceAccess;
import org.somda.sdc.proto.consumer.sco.ScoTransaction;
import org.somda.sdc.proto.discovery.consumer.Client;
import org.somda.sdc.proto.discovery.consumer.DiscoveryObserver;
import org.somda.sdc.proto.discovery.consumer.event.ProbedDeviceFoundMessage;
import org.somda.sdc.proto.guice.ProtoConsumer;
import org.somda.sdc.proto.model.GetContextStatesRequest;
import org.somda.sdc.proto.model.GetMdibRequest;
import org.somda.sdc.proto.model.biceps.AbstractContextStateOneOfMsg;
import org.somda.sdc.proto.model.discovery.Endpoint;
import org.somda.sdc.proto.model.discovery.ScopeMatcher;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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
        var networkInterface = NetworkInterface.networkInterfaces()
                .filter(iface -> Streams.stream(iface.getInetAddresses().asIterator())
                        .map(InetAddress::getHostAddress)
                        .anyMatch(addr -> addr.contains(serverAddr.getAddress().getHostAddress()))
                ).findFirst().orElseThrow(Exception::new);

        udpQueue = injector.getInstance(Key.get(UdpMessageQueueService.class, DiscoveryUdpQueue.class));

        // start required thread pool(s)
        injector.getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                },
                ProtoConsumer.class
        )).startAsync().awaitRunning();


        var wsdMulticastAddress = InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS);

        udpBindingService = injector.getInstance(UdpBindingServiceFactory.class).createUdpBindingService(
                networkInterface,
                wsdMulticastAddress,
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        udpQueue.setUdpBinding(udpBindingService);
        udpBindingService.setMessageReceiver(udpQueue);
    }

    public SdcRemoteDevicesConnector getConnector() {
        return connector;
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

    /**
     * Synchronously invokes a SetString on a given SetService using the provided handle and argument
     *
     * @param setServiceAccess SetService to call Activate on
     * @param handle           operation handle
     * @param value            desired value of operation target
     * @return InvocationState of final OperationInvokedReport
     * @throws ExecutionException   if retrieving the final OperationInvokedReport is aborted
     * @throws InterruptedException if retrieving the final OperationInvokedReport is interrupted
     * @throws TimeoutException     if retrieving the final OperationInvokedReport times out
     */
    static InvocationState invokeSetString(SetServiceAccess setServiceAccess, String handle, String value)
            throws ExecutionException, InterruptedException, TimeoutException {
        LOG.info("Invoking SetString for handle {} with value {}", handle, value);
        SetString setString = new SetString();
        setString.setOperationHandleRef(handle);
        setString.setRequestedStringValue(value);

        final ListenableFuture<ScoTransaction<SetStringResponse>> setStringFuture = setServiceAccess
                .invoke(setString, SetStringResponse.class);
        ScoTransaction<SetStringResponse> setStringResponse = setStringFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
        List<OperationInvokedReport.ReportPart> reportParts =
                setStringResponse.waitForFinalReport(Duration.ofSeconds(5));

        // return the final reports invocation state
        if (!reportParts.isEmpty()) {
            return reportParts.get(reportParts.size() - 1).getInvocationInfo().getInvocationState();
        } else {
            throw new InterruptedException("No report parts received, help.");
        }
    }


    /**
     * Synchronously invokes a SetValue on a given SetService using the provided handle and argument
     *
     * @param setServiceAccess SetService to call Activate on
     * @param handle           operation handle
     * @param value            desired value of operation target
     * @return InvocationState of final OperationInvokedReport
     * @throws ExecutionException   if retrieving the final OperationInvokedReport is aborted
     * @throws InterruptedException if retrieving the final OperationInvokedReport is interrupted
     * @throws TimeoutException     if retrieving the final OperationInvokedReport times out
     */
    static InvocationState invokeSetValue(SetServiceAccess setServiceAccess, String handle, BigDecimal value)
            throws ExecutionException, InterruptedException, TimeoutException {
        LOG.info("Invoking SetValue for handle {} with value {}", handle, value);
        SetValue setValue = new SetValue();
        setValue.setOperationHandleRef(handle);
        setValue.setRequestedNumericValue(value);

        final ListenableFuture<ScoTransaction<SetValueResponse>> setValueFuture = setServiceAccess
                .invoke(setValue, SetValueResponse.class);
        ScoTransaction<SetValueResponse> setValueResponse = setValueFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
        List<OperationInvokedReport.ReportPart> reportParts =
                setValueResponse.waitForFinalReport(Duration.ofSeconds(5));

        // return the final reports invocation state
        return reportParts.get(reportParts.size() - 1).getInvocationInfo().getInvocationState();
    }

    /**
     * Synchronously invokes an ActivateOperation on a given SetService using the provided handle and arguments
     *
     * @param setServiceAccess SetService to call Activate on
     * @param handle           operation handle
     * @param args             activate arguments
     * @return InvocationState of final OperationInvokedReport
     * @throws ExecutionException   if retrieving the final OperationInvokedReport is aborted
     * @throws InterruptedException if retrieving the final OperationInvokedReport is interrupted
     * @throws TimeoutException     if retrieving the final OperationInvokedReport times out
     */
    static InvocationState invokeActivate(SetServiceAccess setServiceAccess, String handle, List<String> args)
            throws ExecutionException, InterruptedException, TimeoutException {
        LOG.info("Invoking Activate for handle {} with arguments {}", handle, args);

        Activate activate = new Activate();
        List<Activate.Argument> argumentList = args.stream().map(x -> {
            var arg = new Activate.Argument();
            arg.setArgValue(x);
            return arg;
        }).collect(Collectors.toList());
        activate.setArgument(argumentList);
        activate.setOperationHandleRef(handle);

        final ListenableFuture<ScoTransaction<ActivateResponse>> activateFuture = setServiceAccess
                .invoke(activate, ActivateResponse.class);
        ScoTransaction<ActivateResponse> activateResponse = activateFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
        List<OperationInvokedReport.ReportPart> reportParts =
                activateResponse.waitForFinalReport(Duration.ofSeconds(5));

        // return the final reports invocation state
        return reportParts.get(reportParts.size() - 1).getInvocationInfo().getInvocationState();
    }

    public static void runConsumer() throws Exception {
        var settings = new ConsumerUtil();

        // TODO: Unmagic me.
        var targetEpr = "urn:uuid:d4e63551-8546-492c-bead-ae764dad89f6";

        var consumer = new Consumer(settings);
        consumer.startAsync().awaitRunning();

        // this map is used to track the outcome of each of the nine steps listed for this class
        Map<Integer, Boolean> resultMap = new HashMap<>(
                Map.of(
                        1, false,
                        2, false,
                        3, false,
                        4, false,
                        5, false,
                        6, false,
                        7, false,
                        8, false,
                        9, false
                )
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            var keys = new ArrayList<>(resultMap.keySet());
            Collections.sort(keys);

            keys.forEach(key -> System.out.printf("### Test %s ### %s%n", key, resultMap.get(key) ? "passed" : "failed"));
        }));

        try {

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
                resultMap.put(1, true);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Couldn't find target with EPR {}", targetEpr, e);
                System.exit(1);
            }
            consumer.getDiscoveryClient().unregisterObserver(obs);
            consumer.getDiscoveryClient().stopAsync().awaitTerminated();

            LOG.info("Connecting to {} at {}", targetEpr, endpoint);
            consumer.getConsumer().connect(endpoint);
            resultMap.put(2, true);

            var getService = consumer.getConsumer().getGetService().orElseThrow(RuntimeException::new);
            var mdib = getService.getMdib(GetMdibRequest.newBuilder().build());

            assert mdib != null;
            resultMap.put(3, true);

            LOG.info("Mdib:\n{}", mdib);

            var fut = consumer.getConnector().connect(
                    consumer.getConsumer(),
                    ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)
            );
            var reportProcessor = new ConsumerReportProcessor();
            var sdcRemoteDevice = fut.get();
            sdcRemoteDevice.getMdibAccessObservable().registerObserver(reportProcessor);
            resultMap.put(4, true);

            var numPatientContexts = getService.getContextStates(GetContextStatesRequest.newBuilder().build())
                    .getPayload()
                    .getContextStateList()
                    .stream()
                    .filter(AbstractContextStateOneOfMsg::hasPatientContextState)
                    .count();
            resultMap.put(5, numPatientContexts >= 1);

            var numLocationContexts = getService.getContextStates(GetContextStatesRequest.newBuilder().build())
                    .getPayload()
                    .getContextStateList()
                    .stream()
                    .filter(AbstractContextStateOneOfMsg::hasLocationContextState)
                    .count();
            resultMap.put(6, numLocationContexts >= 1);

            LOG.info("Number of patient context states: {}", numPatientContexts);
            LOG.info("Number of location context states: {}", numLocationContexts);

            Thread.sleep(REPORT_TIMEOUT);

            // expected number of reports given 5 second interval
            int minNumberReports = (int) (REPORT_TIMEOUT / Duration.ofSeconds(5).toMillis()) - 1;
            var metricChangesOk = reportProcessor.numMetricChanges >= minNumberReports;
            resultMap.put(7, metricChangesOk);
            var conditionChangesOk = reportProcessor.numConditionChanges >= minNumberReports;
            resultMap.put(8, conditionChangesOk);

            LOG.info("Number of metric changes: {} - ok: {}", reportProcessor.numMetricChanges, metricChangesOk);
            LOG.info("Number of condition changes: {} - ok: {}", reportProcessor.numConditionChanges, conditionChangesOk);

            var setService = consumer.getConsumer().getBlockingSetService();

            boolean operationFailed = false;

            try {
                invokeSetString(sdcRemoteDevice.getSetServiceAccess(), Constants.HANDLE_SET_STRING, "SDCri was here");
            } catch (Exception e) {
                operationFailed = true;
                LOG.error("Could not invoke {}", Constants.HANDLE_SET_STRING, e);
            }
            try {
                invokeSetString(sdcRemoteDevice.getSetServiceAccess(), Constants.HANDLE_SET_STRING_ENUM, "OFF");
            } catch (Exception e) {
                operationFailed = true;
                LOG.error("Could not invoke {}", Constants.HANDLE_SET_STRING_ENUM, e);
            }
            try {
                invokeSetValue(sdcRemoteDevice.getSetServiceAccess(), Constants.HANDLE_SET_VALUE, BigDecimal.valueOf(20));
            } catch (Exception e) {
                operationFailed = true;
                LOG.error("Could not invoke {}", Constants.HANDLE_SET_VALUE, e);
            }
            try {
                invokeActivate(sdcRemoteDevice.getSetServiceAccess(), Constants.HANDLE_ACTIVATE, Collections.emptyList());
            } catch (Exception e) {
                operationFailed = true;
                LOG.error("Could not invoke {}", Constants.HANDLE_ACTIVATE, e);
            }
            resultMap.put(9, !operationFailed);

            sdcRemoteDevice.getMdibAccessObservable().unregisterObserver(reportProcessor);
            sdcRemoteDevice.stopAsync().awaitTerminated();
        } finally {
            LOG.info("Done, quitting");
            consumer.stopAsync().awaitTerminated();
        }
    }

    public static void main(String[] args) throws Exception {
        runConsumer();
    }

}
