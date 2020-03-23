package com.example.consumer1;

import com.example.ProviderMdibConstants;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsdiscovery.MatchBy;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryUtil;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.uri.LocationDetailQueryMapper;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.PrerequisitesException;
import org.somda.sdc.glue.consumer.SdcDiscoveryFilterBuilder;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.consumer1.Consumer.invokeActivate;
import static com.example.consumer1.Consumer.invokeSetString;
import static com.example.consumer1.Consumer.invokeSetValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ConsumerIT {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerIT.class);
    private static final Duration MAX_WAIT = Duration.ofSeconds(11);

    public static final String DEFAULT_FACILITY = "sdcri";
    public static final String DEFAULT_BED = "TopBunk";
    public static final String DEFAULT_POC = "DontCare";
    public static final String DEFAULT_REPORT_TIMEOUT = "30";

    private static Duration reportTimeout;
    private static String iface;
    private static String targetFacility;
    private static String targetBed;
    private static String targetPoC;
    private ConsumerReportProcessor reportObs;

    @BeforeAll
    static void setUp() {
        iface = System.getProperty("iface");
        assertNotNull(iface);
        targetFacility = System.getProperty("targetFacility", DEFAULT_FACILITY);
        targetBed = System.getProperty("targetBed", DEFAULT_BED);
        targetPoC = System.getProperty("targetPoC", DEFAULT_POC);
        reportTimeout = Duration.ofSeconds(
                Long.parseLong(
                        System.getProperty("reportTimeout", DEFAULT_REPORT_TIMEOUT)
                )
        );
    }

    @Test
    void runIT() throws Exception {
        var consumer = new Consumer(iface);
        consumer.startUp();

        var targetEpr = discoverDevice(consumer);
        var hostingService = connectDevice(consumer, targetEpr);
        var remoteDevice = connectMdibAndSubscribe(consumer, hostingService);
        verifyContexts(remoteDevice);
        verifyReports();
        verifyOperationInvocation(remoteDevice);

        // shut down
        remoteDevice.getMdibAccessObservable().unregisterObserver(reportObs);
        remoteDevice.stopAsync().awaitTerminated();

        consumer.getConnector().disconnect(targetEpr);
        consumer.shutDown();
    }

    private String discoverDevice(Consumer consumer) throws Exception {

        var wsdUtil = consumer.getInjector().getInstance(WsDiscoveryUtil.class);

        var location = new LocationDetail();
        location.setFacility(targetFacility);
        location.setBed(targetBed);
        location.setPoC(targetPoC);

        var instanceIdentifierOpt = FallbackInstanceIdentifier.create(location);
        assertTrue(instanceIdentifierOpt.isPresent());

        var query = LocationDetailQueryMapper.createWithLocationDetailQuery(
                instanceIdentifierOpt.get(),
                location
        );

        LOG.info("Starting discovery for location {}", query);
        final SettableFuture<String> targetEpr = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                DiscoveredDevice payload = message.getPayload();
                if (wsdUtil.isScopesMatching(payload.getScopes(), List.of(query), MatchBy.RFC3986)) {
                    LOG.info("Found device with epr {}", payload.getEprAddress());
                    targetEpr.set(payload.getEprAddress());
                } else {
                    LOG.info("Found non-matching device with epr {}", payload.getEprAddress());
                }
            }
        };
        consumer.getClient().registerDiscoveryObserver(obs);

        // filter discovery for SDC devices only
        SdcDiscoveryFilterBuilder discoveryFilterBuilder = SdcDiscoveryFilterBuilder.create();
        discoveryFilterBuilder.addScope(query);
        consumer.getClient().probe(discoveryFilterBuilder.get());

        try {
            return targetEpr.get(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Couldn't find target with location {}", location, e);
            fail("Couldn't find target with location " + location, e);
        } finally {
            consumer.getClient().unregisterDiscoveryObserver(obs);
        }
        return null;
    }

    HostingServiceProxy connectDevice(Consumer consumer, String targetEpr) throws InterceptorException {
        LOG.info("Connecting to {}", targetEpr);

        var hostingServiceFuture = consumer.getClient().connect(targetEpr);

        HostingServiceProxy hostingServiceProxy = null;
        try {
            hostingServiceProxy = hostingServiceFuture.get(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Couldn't connect to EPR {}", targetEpr, e);
            fail("Couldn't connect to EPR " + targetEpr, e);
        }
        return hostingServiceProxy;
    }

    SdcRemoteDevice connectMdibAndSubscribe(Consumer consumer, HostingServiceProxy hostingServiceProxy) {
        LOG.info("Attaching to remote mdib and subscriptions");
        ListenableFuture<SdcRemoteDevice> remoteDeviceFuture;
        SdcRemoteDevice sdcRemoteDevice = null;
        try {
            remoteDeviceFuture = consumer.getConnector()
                    .connect(
                            hostingServiceProxy,
                            ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)
                    );
            sdcRemoteDevice = remoteDeviceFuture.get(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
        } catch (PrerequisitesException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Couldn't attach to remote mdib and subscriptions", e);
            fail("Couldn't attach to remote mdib and subscriptions", e);
        }

        // attach report listener
        reportObs = new ConsumerReportProcessor();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(reportObs);
        return sdcRemoteDevice;
    }

    void verifyContexts(SdcRemoteDevice sdcRemoteDevice) {
        // verify that provider has at least one patient and location context attached
        List<AbstractContextState> contextStates = sdcRemoteDevice.getMdibAccess().getContextStates();

        // has patient
        long numPatientContexts = contextStates.stream()
                .filter(x -> PatientContextState.class.isAssignableFrom(x.getClass()))
                .filter(x -> ContextAssociation.ASSOC.equals(x.getContextAssociation()))
                .count();
        assertTrue(numPatientContexts >= 1, "No associated patient context found");
        // has location context
        long numLocationContexts = contextStates.stream()
                .filter(x -> LocationContextState.class.isAssignableFrom(x.getClass()))
                .filter(x -> ContextAssociation.ASSOC.equals(x.getContextAssociation()))
                .filter(x -> targetFacility.equals(((LocationContextState) x).getLocationDetail().getFacility()))
                .count();
        assertTrue(numLocationContexts >= 1, "No associated location context matching discovery data found");
    }

    void verifyReports() throws InterruptedException {

        // wait for incoming reports
        Thread.sleep(reportTimeout.toMillis());

        // expected number of reports given 5 second interval
        int minNumberReports = ((int) (reportTimeout.dividedBy(Duration.ofSeconds(5))) - 1);

        // verify the number of reports for the expected metrics is at least five during the timeout
        assertTrue(
                reportObs.numMetricChanges >= minNumberReports,
                "Did not receive metric reports, expected at least "
                        + minNumberReports
                        + " but received "
                        + reportObs.numMetricChanges
                        + " instead."
        );
        assertTrue(
                reportObs.numConditionChanges >= minNumberReports,
                "Did not receive alert condition reports, expected at least "
                        + minNumberReports
                        + " but received "
                        + reportObs.numConditionChanges
                        + " instead."
        );
    }

    void verifyOperationInvocation(SdcRemoteDevice sdcRemoteDevice) {

        // invoke all target operations
        var setServiceAccess = sdcRemoteDevice.getSetServiceAccess();

        boolean operationFailed = false;
        try {
            invokeSetString(setServiceAccess, ProviderMdibConstants.HANDLE_SET_STRING, "SDCri was here");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_SET_STRING, e);
        }
        try {
            invokeSetString(setServiceAccess, ProviderMdibConstants.HANDLE_SET_STRING_ENUM, "OFF");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_SET_STRING_ENUM, e);
        }
        try {
            invokeSetValue(setServiceAccess, ProviderMdibConstants.HANDLE_SET_VALUE, BigDecimal.valueOf(20));
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_SET_VALUE, e);
        }
        try {
            invokeActivate(setServiceAccess, ProviderMdibConstants.HANDLE_ACTIVATE, Collections.emptyList());
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", ProviderMdibConstants.HANDLE_ACTIVATE, e);
        }
        assertFalse(operationFailed, "Operation invocation failed unexpectedly, check the log");

        LOG.info("Done, quitting");
    }

}
