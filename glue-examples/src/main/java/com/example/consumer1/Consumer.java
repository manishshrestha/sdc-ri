package com.example.consumer1;

import com.example.ProviderMdibConstants;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Injector;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.somda.sdc.dpws.factory.DpwsFrameworkFactory;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.consumer.*;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import java.math.BigDecimal;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


/**
 * This is an example consumer which matches {@link com.example.provider1.Provider} in functionality
 *
 * This consumer executes the following steps and prints whether each step was successful
 *      1. discovery of device with specific endpoint
 *      2. connect to device with specific endpoint
 *      3. read mdib of provider
 *      4. subscribe metrics, alerts, waveforms
 *      5. check that least one patient context exists
 *      6. check that at least one location context exists
 *      7. check that the metric (see above) changes within 30 seconds at least 5 times
 *      8. check that the alert condition (see above)change within 30 seconds at least 5 times
 *      9. execute operations (Activate, SetString, SetValue) as specified and check that result is “finished”
 */
public class Consumer {
    static {
        // TODO: Workaround for
        //  javax.net.ssl.SSLHandshakeException: No subject alternative names present
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);
    private static final ConsumerUtil IT = new ConsumerUtil();
    private static final Duration MAX_WAIT = Duration.ofSeconds(11);
    private static final long MAX_WAIT_SEC = MAX_WAIT.getSeconds();

    private static final long REPORT_TIMEOUT = Duration.ofSeconds(30).toMillis();

    private final Client client;
    private final SdcRemoteDevicesConnector connector;
    private DpwsFramework dpwsFramework;
    private final Injector injector;
    private NetworkInterface networkInterface;

    public Consumer(@Nullable String adapterName) throws SocketException, UnknownHostException {
        this.injector = IT.getInjector();
        this.client = injector.getInstance(Client.class);
        this.connector = injector.getInstance(SdcRemoteDevicesConnector.class);
        if (adapterName != null && !adapterName.isEmpty()) {
            this.networkInterface = NetworkInterface.getByName(adapterName);
        } else {
            // find some kind of default interface
            this.networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
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
        this.dpwsFramework = injector.getInstance(DpwsFrameworkFactory.class)
                .createDpwsFramework(networkInterface);
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();
    }

    protected void shutDown() {
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }


    public static CommandLine parseCommandLineArgs(String[] args) {
        Options options = new Options();

        Option epr = new Option("e", "epr", true, "epr of target provider");
        epr.setRequired(true);
        options.addOption(epr);

        Option networkInterface = new Option("i", "iface", true, "network interface to use");
        networkInterface.setRequired(false);
        options.addOption(networkInterface);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        return cmd;
    }

    private static InvocationState invokeActivate(SetServiceAccess setServiceAccess, String handle, List<String> args) throws ExecutionException, InterruptedException, TimeoutException {
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
        List<OperationInvokedReport.ReportPart> reportParts = activateResponse.waitForFinalReport(Duration.ofSeconds(5));

        // return the final reports invocation state
        return reportParts.get(reportParts.size()-1).getInvocationInfo().getInvocationState();
    }

    private static InvocationState invokeSetValue(SetServiceAccess setServiceAccess, String handle, BigDecimal value) throws ExecutionException, InterruptedException, TimeoutException {
        LOG.info("Invoking SetValue for handle {} with value {}", handle, value);
        SetValue setValue = new SetValue();
        setValue.setOperationHandleRef(handle);
        setValue.setRequestedNumericValue(value);

        final ListenableFuture<ScoTransaction<SetValueResponse>> setValueFuture = setServiceAccess
                .invoke(setValue, SetValueResponse.class);
        ScoTransaction<SetValueResponse> setValueResponse = setValueFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
        List<OperationInvokedReport.ReportPart> reportParts = setValueResponse.waitForFinalReport(Duration.ofSeconds(5));

        // return the final reports invocation state
        return reportParts.get(reportParts.size()-1).getInvocationInfo().getInvocationState();
    }

    private static InvocationState invokeSetString(SetServiceAccess setServiceAccess, String handle, String value) throws ExecutionException, InterruptedException, TimeoutException {
        LOG.info("Invoking SetString for handle {} with value {}", handle, value);
        SetString setString = new SetString();
        setString.setOperationHandleRef(handle);
        setString.setRequestedStringValue(value);

        final ListenableFuture<ScoTransaction<SetStringResponse>> setStringFuture = setServiceAccess
                .invoke(setString, SetStringResponse.class);
        ScoTransaction<SetStringResponse> setStringResponse = setStringFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
        List<OperationInvokedReport.ReportPart> reportParts = setStringResponse.waitForFinalReport(Duration.ofSeconds(5));

        // return the final reports invocation state
        if (!reportParts.isEmpty()) {
            return reportParts.get(reportParts.size() - 1).getInvocationInfo().getInvocationState();
        } else {
            throw new InterruptedException("No report parts received, help.");
        }
    }

    public static void main(String[] args) throws SocketException, UnknownHostException, InterceptorException, TransportException, InterruptedException {

        var settings = parseCommandLineArgs(args);
        var targetEpr = settings.getOptionValue("epr");
        var networkInterface = settings.getOptionValue("iface", null);

        var consumer = new Consumer(networkInterface);
        consumer.startUp();

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

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                var keys = new ArrayList<Integer>(resultMap.keySet());
                Collections.sort(keys);

                keys.forEach(key -> {
                    System.out.println(
                            String.format("### Test %s ### %s", key, resultMap.get(key) ? "passed" : "failed")
                    );
                });
            }
        });

        // see if epr device is available
        LOG.info("Starting discovery for {}", targetEpr);
        final SettableFuture<List<String>> xAddrs = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                DiscoveredDevice payload = message.getPayload();
                if (payload.getEprAddress().toString().equals(targetEpr)) {
                    LOG.info("Found device with epr {}", payload.getEprAddress());
                    xAddrs.set(payload.getXAddrs());
                } else {
                    LOG.info("Found non-matching device with epr {}", payload.getEprAddress());
                }
            }
        };

        // When explicit discovery is triggered (filter set to none)
        consumer.getClient().registerDiscoveryObserver(obs);

        SdcDiscoveryFilterBuilder discoveryFilterBuilder = SdcDiscoveryFilterBuilder.create();
        consumer.getClient().probe(discoveryFilterBuilder.get());
        // default probe timeout is 10 seconds, so 10 should be sufficient
        try {
            List<String> targetXAddrs = xAddrs.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            resultMap.put(1, true);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Couldn't find target with EPR {}", targetEpr, e);
            System.exit(1);
        }
        consumer.getClient().unregisterDiscoveryObserver(obs);

        var device_uri = URI.create(targetEpr);
        LOG.info("Connecting to {}", targetEpr);
        var hostingServiceFuture = consumer.getClient().connect(device_uri);

        HostingServiceProxy hostingServiceProxy = null;
        try {
            hostingServiceProxy = hostingServiceFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            resultMap.put(2, true);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Couldn't connect to EPR {}", targetEpr, e);
            System.exit(1);
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

        Thread.sleep(1000);

        // attach report listener
        var reportObs = new ConsumerReportProcessor();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(reportObs);

        // verify that provider has at least one patient and location context attached
        List<AbstractContextState> contextStates = sdcRemoteDevice.getMdibAccess().getContextStates();

        // has patient
        long numPatientContexts = contextStates.stream().filter(x -> PatientContextState.class.isAssignableFrom(x.getClass())).count();
        resultMap.put(5, numPatientContexts >= 1);
        // has location context
        long numLocationContexts = contextStates.stream().filter(x -> LocationContextState.class.isAssignableFrom(x.getClass())).count();
        resultMap.put(6, numLocationContexts >= 1);

        Thread.sleep(REPORT_TIMEOUT);

        // verify the number of reports for the expected metrics is at least five during the timeout
        var metricChangesOk = reportObs.numMetricChanges >= 5;
        resultMap.put(7, metricChangesOk);
        var conditionChangesOk = reportObs.numConditionChanges >= 5;
        resultMap.put(8, conditionChangesOk);


        // invoke all target operations
        var setServiceAccess = sdcRemoteDevice.getSetServiceAccess();

        boolean operationFailed = false;
        try {
            invokeSetString(setServiceAccess, ProviderMdibConstants.HANDLE_SET_STRING, "SDCri was here");
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_SET_STRING, e);
        }
        try {
            invokeSetString(setServiceAccess, ProviderMdibConstants.HANDLE_SET_STRING_ENUM, "OFF");
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_SET_STRING_ENUM, e);
        }
        try {
            invokeSetValue(setServiceAccess, ProviderMdibConstants.HANDLE_SET_VALUE, BigDecimal.valueOf(20));
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_SET_VALUE, e);
        }
        try {
            invokeActivate(setServiceAccess, ProviderMdibConstants.HANDLE_ACTIVATE, Collections.emptyList());
        } catch (ExecutionException | TimeoutException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_ACTIVATE, e);
        }
        resultMap.put(9, !operationFailed);

        LOG.info("Done, quitting");

        sdcRemoteDevice.getMdibAccessObservable().unregisterObserver(reportObs);
        consumer.getConnector().disconnect(device_uri);
        consumer.shutDown();

    }

}
