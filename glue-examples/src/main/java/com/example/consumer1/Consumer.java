package com.example.consumer1;

import com.example.Constants;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Injector;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.biceps.model.message.SetStringResponse;
import org.somda.sdc.biceps.model.message.SetValue;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.wsdl.WsdlRetriever;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.PrerequisitesException;
import org.somda.sdc.glue.consumer.SdcDiscoveryFilterBuilder;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.consumer.SetServiceAccess;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
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


/**
 * This is an example consumer which matches {@link com.example.provider1.Provider} in functionality
 * <p>
 * This consumer executes the following steps and prints whether each step was successful
 * 1. discovery of device with specific endpoint
 * 2. connect to device with specific endpoint
 * 3. read mdib of provider
 * 4. subscribe metrics, alerts, waveforms
 * 5. check that least one patient context exists
 * 6. check that at least one location context exists
 * 7. check that the metric (see above) changes within 30 seconds at least 5 times
 * 8. check that the alert condition (see above)change within 30 seconds at least 5 times
 * 9. execute operations (Activate, SetString, SetValue) as specified and check that result is “finished”
 */
public class Consumer {

    private static final Logger LOG = LogManager.getLogger(Consumer.class);
    private static final Duration MAX_WAIT = Duration.ofSeconds(11);
    private static final long MAX_WAIT_SEC = MAX_WAIT.getSeconds();

    private static final long REPORT_TIMEOUT = Duration.ofSeconds(30000).toMillis();

    private final ConsumerUtil consumerUtil;
    private final Client client;
    private final SdcRemoteDevicesConnector connector;
    private DpwsFramework dpwsFramework;
    private final Injector injector;
    private NetworkInterface networkInterface;

    /**
     * Creates an SDC Consumer instance.
     *
     * @param consumerUtil utility containing injector and settings
     * @throws SocketException      if network adapter couldn't be bound
     * @throws UnknownHostException if localhost couldn't be determined
     */
    public Consumer(ConsumerUtil consumerUtil) throws SocketException, UnknownHostException {
        this.consumerUtil = consumerUtil;
        this.injector = consumerUtil.getInjector();
        this.client = injector.getInstance(Client.class);
        this.connector = injector.getInstance(SdcRemoteDevicesConnector.class);
        if (consumerUtil.getIface() != null && !consumerUtil.getIface().isBlank()) {
            LOG.info("Starting with interface {}", consumerUtil.getIface());
            this.networkInterface = NetworkInterface.getByName(consumerUtil.getIface());
        } else {
            if (consumerUtil.getAddress() != null && !consumerUtil.getAddress().isBlank()) {
                // bind to adapter matching ip
                LOG.info("Starting with address {}", consumerUtil.getAddress());
                this.networkInterface = NetworkInterface.getByInetAddress(
                        InetAddress.getByName(consumerUtil.getAddress())
                );
            } else {
                // find loopback interface for fallback
                networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                LOG.info("Starting with fallback default adapter {}", networkInterface);
            }
        }
    }

    public Client getClient() {
        return client;
    }

    public SdcRemoteDevicesConnector getConnector() {
        return connector;
    }

    protected void startUp() throws SocketException {
        // provide the name of your network adapter
        this.dpwsFramework = injector.getInstance(DpwsFramework.class);
        this.dpwsFramework.setNetworkInterface(networkInterface);
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();
    }

    protected void shutDown() {
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
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

    public Injector getInjector() {
        return injector;
    }

    public static void main(String[] args) throws SocketException, UnknownHostException,
            InterceptorException, TransportException, InterruptedException {

        var settings = new ConsumerUtil(args);
        var targetEpr = settings.getEpr();

        var consumer = new Consumer(settings);
        consumer.startUp();

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

            keys.forEach(key -> System.out.println(
                    String.format("### Test %s ### %s", key, resultMap.get(key) ? "passed" : "failed")
            ));
        }));

        // see if device using the provided epr address is available
        LOG.info("Starting discovery for {}", targetEpr);
        final SettableFuture<List<String>> xAddrs = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                DiscoveredDevice payload = message.getPayload();
                if (payload.getEprAddress().equals(targetEpr)) {
                    LOG.info("Found device with epr {}", payload.getEprAddress());
                    xAddrs.set(payload.getXAddrs());
                } else {
                    LOG.info("Found non-matching device with epr {}", payload.getEprAddress());
                }
            }
        };
        consumer.getClient().registerDiscoveryObserver(obs);

        // filter discovery for SDC devices only
        SdcDiscoveryFilterBuilder discoveryFilterBuilder = SdcDiscoveryFilterBuilder.create();
        consumer.getClient().probe(discoveryFilterBuilder.get());

        try {
            List<String> targetXAddrs = xAddrs.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            resultMap.put(1, true);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Couldn't find target with EPR {}", targetEpr, e);
            System.exit(1);
        }
        consumer.getClient().unregisterDiscoveryObserver(obs);

        var deviceUri = targetEpr;
        LOG.info("Connecting to {}", targetEpr);
        var hostingServiceFuture = consumer.getClient().connect(deviceUri);

        HostingServiceProxy hostingServiceProxy = null;
        try {
            hostingServiceProxy = hostingServiceFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            resultMap.put(2, true);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Couldn't connect to EPR {}", targetEpr, e);
            System.exit(1);
        }

        // optionally retrieve the wsdl
        LOG.info("Retrieving device WSDL");
        var wsdlRetriever = consumer.getInjector().getInstance(WsdlRetriever.class);
        try {
            var wsdls = wsdlRetriever.retrieveWsdls(hostingServiceProxy);
            LOG.debug("Retrieved WSDLs");
            if (LOG.isDebugEnabled()) {
                wsdls.forEach((service, data) -> {
                    LOG.debug("WSDLs for service {}: {}", service, data);
                });
            }
        } catch (IOException e) {
            LOG.error("Could not retrieve WSDL", e);
        }

        LOG.info("Attaching to remote mdib and subscriptions for {}", targetEpr);
        ListenableFuture<SdcRemoteDevice> remoteDeviceFuture;
        SdcRemoteDevice sdcRemoteDevice = null;
        try {
            remoteDeviceFuture = consumer.getConnector()
                    .connect(
                            hostingServiceProxy,
                            ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)
                    );
            sdcRemoteDevice = remoteDeviceFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            resultMap.put(3, true);
            resultMap.put(4, true);
        } catch (PrerequisitesException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Couldn't attach to remote mdib and subscriptions for {}", targetEpr, e);
            System.exit(1);
        }

        // get Mdib test
        var soapUtil = consumer.getInjector().getInstance(SoapUtil.class);
        var messageModelFactory = consumer.getInjector().getInstance(ObjectFactory.class);
        var client = hostingServiceProxy.getHostedServices().get("HighPriorityServices").getRequestResponseClient();

        // attach report listener
        var reportObs = new ConsumerReportProcessor();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(reportObs);

        // verify that provider has at least one patient and location context attached
        List<AbstractContextState> contextStates = sdcRemoteDevice.getMdibAccess().getContextStates();

        // has patient
        long numPatientContexts = contextStates.stream()
                .filter(x -> PatientContextState.class.isAssignableFrom(x.getClass())).count();
        resultMap.put(5, numPatientContexts >= 1);
        // has location context
        long numLocationContexts = contextStates.stream()
                .filter(x -> LocationContextState.class.isAssignableFrom(x.getClass())).count();
        resultMap.put(6, numLocationContexts >= 1);

//        var repetiton = 1000;
//        for (int i = 0; i < repetiton; i++) {
//            var startTime = System.currentTimeMillis();
//            try {
//                client.sendRequestResponse(soapUtil.createMessage(ActionConstants.ACTION_GET_MDIB, messageModelFactory.createGetMdib()));
//            } catch (SoapFaultException | MarshallingException e) {
//                throw new RuntimeException(e);
//            }
//            var stopTime = System.currentTimeMillis();
//
//            LOG.info("GetMdib took {}ms", () -> stopTime - startTime);
//        }

        // wait for incoming reports
        Thread.sleep(REPORT_TIMEOUT);

        // expected number of reports given 5 second interval
        int minNumberReports = (int) (REPORT_TIMEOUT / Duration.ofSeconds(5).toMillis()) - 1;

        // verify the number of reports for the expected metrics is at least five during the timeout
        var metricChangesOk = reportObs.numMetricChanges >= minNumberReports;
        resultMap.put(7, metricChangesOk);
        var conditionChangesOk = reportObs.numConditionChanges >= minNumberReports;
        resultMap.put(8, conditionChangesOk);


        // invoke all target operations
        var setServiceAccess = sdcRemoteDevice.getSetServiceAccess();

        boolean operationFailed = false;
        try {
            invokeSetString(setServiceAccess, Constants.HANDLE_SET_STRING, "SDCri was here");
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_SET_STRING, e);
        }
        try {
            invokeSetString(setServiceAccess, Constants.HANDLE_SET_STRING_ENUM, "OFF");
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_SET_STRING_ENUM, e);
        }
        try {
            invokeSetValue(setServiceAccess, Constants.HANDLE_SET_VALUE, BigDecimal.valueOf(20));
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_SET_VALUE, e);
        }
        try {
            invokeActivate(setServiceAccess, Constants.HANDLE_ACTIVATE, Collections.emptyList());
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_ACTIVATE, e);
        }
        resultMap.put(9, !operationFailed);

        LOG.info("Done, quitting");

        sdcRemoteDevice.getMdibAccessObservable().unregisterObserver(reportObs);
        sdcRemoteDevice.stopAsync().awaitTerminated();

        consumer.getConnector().disconnect(deviceUri);
        consumer.shutDown();
    }

}
