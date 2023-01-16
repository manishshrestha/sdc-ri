package com.example.provider1;

import com.draeger.medical.t2iapi.ResponseTypes;
import com.draeger.medical.t2iapi.activation_state.ActivationStateTypes;
import com.draeger.medical.t2iapi.context.ContextTypes;
import com.draeger.medical.t2iapi.metric.MetricTypes;
import com.example.Constants;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.SetValue;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalPresence;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.ClockState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.MeansContextDescriptor;
import org.somda.sdc.biceps.model.participant.MeansContextState;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricValue;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.OperatorContextDescriptor;
import org.somda.sdc.biceps.model.participant.OperatorContextState;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.SampleArrayValue;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.biceps.model.participant.WorkflowContextDescriptor;
import org.somda.sdc.biceps.model.participant.WorkflowContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.model.Body;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;
import org.somda.sdc.glue.provider.services.HighPriorityServices;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is an example provider using an mdib provided as XML as well as TLS secured communication.
 * <p>
 * It is the complement to {@link com.example.consumer1.Consumer} in terms of used
 * functionality and features.
 */
public class Provider extends AbstractIdleService {

    private static final Logger LOG = LogManager.getLogger(Provider.class);

    private static final int MAX_ENUM_ITERATIONS = 17;
    private static final String GRPC_HOST = "localhost";
    private static final int GRPC_PORT = 50051;

    private final Injector injector;
    private final LocalMdibAccess mdibAccess;
    private final DpwsFramework dpwsFramework;
    private final SdcDevice sdcDevice;
    private final SoapMessageFactory soapMessageFactory;

    private InstanceIdentifier instanceIdentifier;
    private LocationDetail currentLocation;

    /**
     * Create an instance of an SDC Provider.
     *
     * @param providerUtil options and configured injector
     * @throws SocketException      thrown if network adapter cannot be set up
     * @throws UnknownHostException if provided address cannot be resolved to an adapter
     */
    public Provider(ProviderUtil providerUtil) throws SocketException, UnknownHostException {
        this.injector = providerUtil.getInjector();

        NetworkInterface networkInterface;
        if (providerUtil.getIface() != null && !providerUtil.getIface().isEmpty()) {
            LOG.info("Starting with interface {}", providerUtil.getIface());
            networkInterface = NetworkInterface.getByName(providerUtil.getIface());
        } else {
            if (providerUtil.getAddress() != null && !providerUtil.getAddress().isBlank()) {
                // bind to adapter matching ip
                LOG.info("Starting with address {}", providerUtil.getAddress());
                networkInterface = NetworkInterface.getByInetAddress(
                        InetAddress.getByName(providerUtil.getAddress())
                );
            } else {
                // find loopback interface for fallback
                networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                LOG.info("Starting with fallback default adapter {}", networkInterface);
            }
        }
        assert networkInterface != null;
        this.dpwsFramework = injector.getInstance(DpwsFramework.class);
        this.dpwsFramework.setNetworkInterface(networkInterface);
        this.mdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        this.soapMessageFactory = injector.getInstance(SoapMessageFactory.class);

        var epr = providerUtil.getEpr();
        if (epr == null) {
            epr = "urn:uuid:" + UUID.randomUUID();
            LOG.info("No epr address provided, generated random epr {}", epr);
        }

        var handler = new OperationHandler(this.mdibAccess);
        String finalEpr = epr;
        this.sdcDevice = injector.getInstance(SdcDeviceFactory.class)
                .createSdcDevice(new DeviceSettings() {
                                     @Override
                                     public EndpointReferenceType getEndpointReference() {
                                         return injector.getInstance(WsAddressingUtil.class)
                                                 .createEprWithAddress(finalEpr);
                                     }

                                     @Override
                                     public NetworkInterface getNetworkInterface() {
                                         return networkInterface;
                                     }
                                 }, this.mdibAccess, List.of(handler),
                        Collections.singleton(injector.getInstance(SdcRequiredTypesAndScopes.class)));

        this.instanceIdentifier = new InstanceIdentifier();
        this.instanceIdentifier.setRootName("AwesomeExampleInstance");

        this.currentLocation = null;
    }

    @Override
    protected void startUp() throws Exception {
        DpwsUtil dpwsUtil = injector.getInstance(DpwsUtil.class);

        sdcDevice.getHostingServiceAccess().setThisDevice(dpwsUtil.createDeviceBuilder()
                .setFriendlyName(dpwsUtil.createLocalizedStrings()
                        .add("en", "Provider Example Unit")
                        .get())
                .setFirmwareVersion("v1.2.3")
                .setSerialNumber("1234-5678-9101-1121").get());

        sdcDevice.getHostingServiceAccess().setThisModel(dpwsUtil.createModelBuilder()
                .setManufacturer(dpwsUtil.createLocalizedStrings()
                        .add("en", "Provider Example Inc.")
                        .add("de", "Beispiel Provider AG")
                        .add("cn", "范例公司")
                        .get())
                .setManufacturerUrl("http://www.example.com")
                .setModelName(dpwsUtil.createLocalizedStrings()
                        .add("PEU")
                        .get())
                .setModelNumber("54-32-1")
                .setPresentationUrl("http://www.example.com")
                .get());

        final ModificationsBuilderFactory modificationsBuilderFactory =
                injector.getInstance(ModificationsBuilderFactory.class);

        // load initial mdib from file
        final MdibXmlIo mdibXmlIo = injector.getInstance(MdibXmlIo.class);
        InputStream mdibAsStream = Provider.class.getClassLoader().getResourceAsStream("provider1/mdib.xml");
        if (mdibAsStream == null) {
            throw new RuntimeException("Could not load mdib.xml as resource");
        }
        final Mdib mdib = mdibXmlIo.readMdib(mdibAsStream);
        final MdibDescriptionModifications modifications =
                modificationsBuilderFactory.createModificationsBuilder(mdib).get();
        mdibAccess.writeDescription(modifications);

        if (currentLocation != null) {
            // update the location again to match mdib and scopes
            this.setLocation(currentLocation);
        }

        dpwsFramework.startAsync().awaitRunning();
        sdcDevice.startAsync().awaitRunning();

        GrpcServer.startGrpcServer(GRPC_PORT, GRPC_HOST, this);
        System.out.println("GRPC Server started on " + GRPC_HOST + ":" + GRPC_PORT);
    }

    @Override
    protected void shutDown() {
        sdcDevice.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
        sdcDevice.stopAsync().awaitTerminated();
    }

    public void setLocation(LocationDetail location) throws PreprocessingException {
        Optional<InstanceIdentifier> newInstanceIdentifier = FallbackInstanceIdentifier.create(location);
        newInstanceIdentifier.ifPresent(
                ii -> {
                    this.instanceIdentifier = ii;
                    LOG.info("Updated instanceIdentifier to {}", ii.getRootName());
                });
        if (this.isRunning() || this.state() == State.STARTING) {
            LOG.info("Updating location context");
            final MdibStateModifications locMod = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
            // update location state
            try (ReadTransaction readTransaction = mdibAccess.startTransaction()) {
                final var locDesc = readTransaction.getDescriptor(
                        Constants.HANDLE_LOCATIONCONTEXT, LocationContextDescriptor.class).orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s",
                                Constants.HANDLE_LOCATIONCONTEXT)));

                var locState = new LocationContextState();
                locState.setLocationDetail(location);
                locState.setDescriptorVersion(locDesc.getDescriptorVersion());
                locState.setDescriptorHandle(locDesc.getHandle());
                locState.setStateVersion(BigInteger.ONE);
                locState.setHandle(locDesc.getHandle() + "State");
                locState.setBindingMdibVersion(mdibAccess.getMdibVersion().getVersion());
                locState.setContextAssociation(ContextAssociation.ASSOC);
                locState.getValidator().add(this.instanceIdentifier);
                locState.getIdentification().add(this.instanceIdentifier);

                locMod.add(locState);
            }
            mdibAccess.writeStates(locMod);
        }
        this.currentLocation = (LocationDetail) location.clone();
    }

    public void setComponentActivation(String handle, ComponentActivation activation) {
        // TODO: where do I find the Components?
        final Optional<AbstractDeviceComponentDescriptor> descriptor =
            mdibAccess.getDescriptor(handle, AbstractDeviceComponentDescriptor.class);
        // TODO: implement properly
        //mdibAccess.getSTa

        // activate the Component
        // - naive approach: find the AbstractDeviceComponentState and set its activationState.
        //   NOTE that this is NOT Standard-compliant as Activation/Deactivation of Components must
        //        also take the activationState of its parent Component into account.
        // NOTE: according to Lukas, there is currently no implementation for this in SDC.ri
        // TODO: implement
    }

    /**
     * Adds a sine wave to the data of a waveform.
     *
     * @param handle descriptor handle of waveform state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeWaveform(String handle) throws PreprocessingException {
        final MdibStateModifications modifications =
                MdibStateModifications.create(MdibStateModifications.Type.WAVEFORM);

        try (ReadTransaction readTransaction = mdibAccess.startTransaction()) {
            final var state = readTransaction.getState(handle, RealTimeSampleArrayMetricState.class).orElseThrow(() ->
                    new RuntimeException(String.format("Could not find state for handle %s", handle)));

            final var metricQuality = new SampleArrayValue.MetricQuality();
            metricQuality.setMode(GenerationMode.REAL);
            metricQuality.setValidity(MeasurementValidity.VLD);

            SampleArrayValue sampleArrayValue = new SampleArrayValue();
            sampleArrayValue.setMetricQuality(metricQuality);

            int minValue = 0;
            int maxValue = 50;
            int sampleCapacity = 10;

            // sine wave
            var values = new LinkedList<BigDecimal>();
            double delta = 2 * Math.PI / sampleCapacity;
            IntStream.range(0, sampleCapacity).forEachOrdered(n -> {
                values.add(
                        new BigDecimal((Math.sin(n * delta) + 1) / 2.0 * (maxValue - minValue) + minValue)
                                .setScale(15, RoundingMode.DOWN));
            });
            sampleArrayValue.setSamples(values);
            sampleArrayValue.setDeterminationTime(Instant.now());

            state.setMetricValue(sampleArrayValue);

            modifications.add(state);
        }

        mdibAccess.writeStates(modifications);
    }

    /**
     * Increments the value of a NumericMetricState.
     *
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeNumericMetric(String handle) throws PreprocessingException {
        Optional<NumericMetricState> stateOpt = mdibAccess.getState(handle, NumericMetricState.class);
        NumericMetricState state = stateOpt.orElseThrow();
        var val = state.getMetricValue();
        if (val != null && val.getValue() != null) {
            val.setValue(val.getValue().add(BigDecimal.ONE));
        } else {
            val = new NumericMetricValue();
            val.setValue(BigDecimal.ONE);
        }
        val.setDeterminationTime(Instant.now());

        ProviderUtil.addMetricQualityDemo(val);

        state.setMetricValue(val);
        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(state));
    }

    /**
     * Changes the content of a StringMetricState, toggling between "UPPERCASE" and "lowercase" as content.
     *
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeStringMetric(String handle) throws PreprocessingException {
        Optional<StringMetricState> stateOpt = mdibAccess.getState(handle, StringMetricState.class);
        StringMetricState state = stateOpt.orElseThrow();
        var val = state.getMetricValue();
        if (val != null && val.getValue() != null) {
            var actVal = val.getValue();
            if (actVal.equals("UPPERCASE")) {
                val.setValue("lowercase");
            } else {
                val.setValue("UPPERCASE");
            }
        } else {
            val = new StringMetricValue();
            val.setValue("initial VALUE");
        }
        val.setDeterminationTime(Instant.now());

        ProviderUtil.addMetricQualityDemo(val);

        state.setMetricValue(val);
        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(state));
    }

    /**
     * Changes the content of an EnumStringMetricState, selecting the next allowed value.
     *
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeEnumStringMetric(String handle) throws PreprocessingException {
        Optional<MdibEntity> entityOpt = mdibAccess.getEntity(handle);
        MdibEntity mdibEntity = entityOpt.orElseThrow();
        EnumStringMetricDescriptor descriptor = (EnumStringMetricDescriptor) mdibEntity.getDescriptor();

        List<String> allowedValue =
                descriptor.getAllowedValue().stream().map(x -> x.getValue()).collect(Collectors.toList());

        EnumStringMetricState state = (EnumStringMetricState) mdibEntity.getStates().get(0);

        var val = state.getMetricValue();
        if (val != null && val.getValue() != null) {
            var actVal = val.getValue();

            Iterator<String> iter = Iterables.cycle(allowedValue).iterator();
            String next = iter.next();
            // since Iterables.cycle by definition creates an infinite iterator,
            // having a break condition is a safety mechanism
            // this will either select the *next* allowed value or stop at some value
            // It's by no means a good idea for a production solution, but kinda
            // neat for this example.
            int i = 0;
            while (iter.hasNext() && i < MAX_ENUM_ITERATIONS) {
                if (next.equals(actVal)) {
                    next = iter.next();
                    break;
                }
                next = iter.next();
                i++;
            }

            val.setValue(next);
        } else {
            val = new StringMetricValue();
            val.setValue(allowedValue.get(0));
        }
        val.setDeterminationTime(Instant.now());
        ProviderUtil.addMetricQualityDemo(val);
        state.setMetricValue(val);
        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(state));
    }

    /**
     * Change the specified ClockState by updating its date and time.
     * @param clockHandle handle of the clockState to update.
     */
    public void updateClockState(String clockHandle) {
        final MdibEntity clockEntity = mdibAccess.getEntity(clockHandle).orElseThrow();
        final ClockState clockState = (ClockState) clockEntity.getStates().get(0);
        final Instant now = Instant.now();
        clockState.setDateAndTime(now);
        clockState.setLastSet(now);

        try {
            mdibAccess.writeStates(MdibStateModifications
                .create(MdibStateModifications.Type.COMPONENT).add(clockState));
        } catch (PreprocessingException e) {
            LOG.error("Could not update clock state", e);
        }
    }

    /**
     * Toggles an AlertSignalState and AlertConditionState between presence on and off.
     *
     * @param signalHandle    signal handle to toggle
     * @param conditionHandle matching condition to toggle
     */
    public void changeAlertSignalAndConditionPresence(String signalHandle, String conditionHandle) {

        var signalEntity = mdibAccess.getEntity(signalHandle).orElseThrow();
        var conditionEntity = mdibAccess.getEntity(conditionHandle).orElseThrow();

        AlertSignalState signalState = (AlertSignalState) signalEntity.getStates().get(0);
        AlertConditionState conditionState = (AlertConditionState) conditionEntity.getStates().get(0);

        if (signalState.getPresence() == null || signalState.getPresence() == AlertSignalPresence.ON) {
            signalState.setPresence(AlertSignalPresence.OFF);
            conditionState.setPresence(false);
        } else {
            signalState.setPresence(AlertSignalPresence.ON);
            conditionState.setPresence(true);
        }

        try {
            mdibAccess.writeStates(MdibStateModifications
                    .create(MdibStateModifications.Type.ALERT).add(conditionState).add(signalState));
        } catch (PreprocessingException e) {
            LOG.error("", e);
        }

    }

    public static void main(String[] args) throws IOException, PreprocessingException {

        var util = new ProviderUtil(args);

        var targetFacility = System.getenv().getOrDefault("ref_fac", Constants.DEFAULT_FACILITY);
        var targetBed = System.getenv().getOrDefault("ref_bed", Constants.DEFAULT_BED);
        var targetPoC = System.getenv().getOrDefault("ref_poc", Constants.DEFAULT_POC);

        Provider provider = new Provider(util);

        // set a location for scopes
        var loc = new LocationDetail();
        loc.setBed(targetBed);
        loc.setPoC(targetPoC);
        loc.setFacility(targetFacility);
        provider.setLocation(loc);

        provider.startAsync().awaitRunning();

        // generate some data
        var waveformInterval = util.getWaveformInterval().toMillis();
        LOG.info("Sending waveforms every {}ms", waveformInterval);
        var t1 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(waveformInterval);
                    provider.changeWaveform(Constants.HANDLE_WAVEFORM);
                } catch (Exception e) {
                    LOG.warn("Thread loop stopping", e);
                    break;
                }
            }
        });
        t1.setDaemon(true);
        t1.start();

        var reportInterval = util.getReportInterval().toMillis();
        LOG.info("Sending reports every {}ms", reportInterval);
        var t2 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(reportInterval);
                    provider.changeNumericMetric(Constants.HANDLE_NUMERIC_DYNAMIC);
                    provider.changeStringMetric(Constants.HANDLE_STRING_DYNAMIC);
                    provider.changeEnumStringMetric(Constants.HANDLE_ENUM_DYNAMIC);
                    provider.changeAlertSignalAndConditionPresence(
                            Constants.HANDLE_ALERT_SIGNAL, Constants.HANDLE_ALERT_CONDITION);
                } catch (InterruptedException | PreprocessingException e) {
                    LOG.warn("Thread loop stopping", e);
                    break;
                }
            }
        });
        t2.setDaemon(true);
        t2.start();

        // graceful shutdown using sigterm
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            t1.interrupt();
            t2.interrupt();

            provider.stopAsync().awaitTerminated();
        }));

        try {
            System.in.read();
        } catch (IOException e) {
            // pass and quit
        }

        t1.interrupt();
        t2.interrupt();

        provider.stopAsync().awaitTerminated();
    }

    public Injector getInjector() {
        return injector;
    }

    public LocalMdibAccess getMdibAccess() {
        return mdibAccess;
    }


    /**
     * Toggles an AlertSignalState and AlertConditionState between presence on and off.
     *
     * @param handle          alert condition handle whose presence should be set
     * @param newPresenceValue new value for the @Presence attribute of the handle
     */
    public void setAlertConditionPresence(String handle, Boolean newPresenceValue) {
        // NOTE: problem: according to Biceps:R0114, the @ActivationState and @Presence of AlertSignals,
        //       AlertConditions and AlertSystems must conform to Table 9 in the Biceps Standard.
        // TODO: clarify what is supposed to happen when the @Presence set by this Method causes the
        //       combination to become invalid according to Table 9.
        var entity = mdibAccess.getEntity(handle).orElseThrow();

        AlertConditionState state = (AlertConditionState) entity.getStates().get(0);
        state.setPresence(newPresenceValue);

        try {
            mdibAccess.writeStates(MdibStateModifications
                .create(MdibStateModifications.Type.ALERT).add(state));
        } catch (PreprocessingException e) {
            LOG.error("", e);
        }
    }

    public void setAlertActivation(String handle, ActivationStateTypes.AlertActivation activation) {
        // TODO: implement
        // NOTE: problem: according to Biceps:R0114, the @ActivationState and @Presence of AlertSignals,
        //       AlertConditions and AlertSystems must conform to Table 9 in the Biceps Standard.
        // TODO: clarify what is supposed to happen when the ActivationState set by this Method causes the
        //       combination to become invalid according to Table 9.
    }

    public ResponseTypes.Result setMetricQualityValidity(String metricHandle, MetricTypes.MeasurementValidity validity) {

        final MdibEntity entity = mdibAccess.getEntity(metricHandle).orElseThrow();
        final List<AbstractMetricState> states = entity.getStates(AbstractMetricState.class);
        for (AbstractMetricState state : states) {
            if (state instanceof NumericMetricState) {
                final NumericMetricValue metricValue = ((NumericMetricState) state).getMetricValue();
                if (metricValue == null) {
                    continue;
                }
                AbstractMetricValue.MetricQuality metricQuality = metricValue.getMetricQuality();
                if (metricQuality == null) {
                    metricQuality = new AbstractMetricValue.MetricQuality();
                    metricValue.setMetricQuality(metricQuality);
                }
                metricQuality.setValidity(translateValidityFromT2IAPI(validity));

                try {
                    mdibAccess.writeStates(MdibStateModifications
                        .create(MdibStateModifications.Type.METRIC).add(state));
                } catch (PreprocessingException e) {
                    LOG.error("Could not write state to set metric quality validity", e);
                }

                return ResponseTypes.Result.RESULT_SUCCESS;
            } else if (state instanceof StringMetricState) {
                final StringMetricValue metricValue = ((StringMetricState) state).getMetricValue();
                if (metricValue == null) {
                    continue;
                }
                AbstractMetricValue.MetricQuality metricQuality = metricValue.getMetricQuality();
                if (metricQuality == null) {
                    metricQuality = new AbstractMetricValue.MetricQuality();
                    metricValue.setMetricQuality(metricQuality);
                }
                metricQuality.setValidity(translateValidityFromT2IAPI(validity));

                if (   MeasurementValidity.ONG.equals(metricQuality.getValidity())
                    || MeasurementValidity.NA.equals(metricQuality.getValidity())) {
                    // NOTE: According to Biceps:B.61, when MeasurementValidity is ONG or NA,
                    //       then no Value should be present.
                    //noinspection ConstantConditions
                    metricValue.setValue(null);
                }

                try {
                    mdibAccess.writeStates(MdibStateModifications
                        .create(MdibStateModifications.Type.METRIC).add(state));
                } catch (PreprocessingException e) {
                    LOG.error("Could not write state to set metric quality validity", e);
                }

                return ResponseTypes.Result.RESULT_SUCCESS;
            } else if (state instanceof RealTimeSampleArrayMetricState) {
                final SampleArrayValue metricValue = ((RealTimeSampleArrayMetricState) state).getMetricValue();
                if (metricValue == null) {
                    continue;
                }
                AbstractMetricValue.MetricQuality metricQuality = metricValue.getMetricQuality();
                if (metricQuality == null) {
                    metricQuality = new AbstractMetricValue.MetricQuality();
                    metricValue.setMetricQuality(metricQuality);
                }
                metricQuality.setValidity(translateValidityFromT2IAPI(validity));

                if (   MeasurementValidity.ONG.equals(metricQuality.getValidity())
                    || MeasurementValidity.NA.equals(metricQuality.getValidity())) {
                    // NOTE: According to Biceps:B.61, when MeasurementValidity is ONG or NA,
                    //       then no Value should be present.
                    metricValue.setSamples(List.of());
                }

                try {
                    mdibAccess.writeStates(MdibStateModifications
                        .create(MdibStateModifications.Type.METRIC).add(state));
                } catch (PreprocessingException e) {
                    LOG.error("Could not write state to set metric quality validity", e);
                }

                return ResponseTypes.Result.RESULT_SUCCESS;
            } else {
                // TODO: add support for DistributionSampleArrayMetricState
                // TODO: add support for RealTimeSampleArrayMetricState
                return ResponseTypes.Result.RESULT_NOT_IMPLEMENTED;
            }
        }
        return ResponseTypes.Result.RESULT_FAIL;
    }

    public ResponseTypes.Result switchAbstractOperationStateOperatingMode(String opHandle) {

        var opEntity = mdibAccess.getEntity(opHandle).orElseThrow();

        AbstractOperationState opState = (AbstractOperationState) opEntity.getStates().get(0);
        final OperatingMode operatingMode = opState.getOperatingMode();
        if (OperatingMode.DIS.equals(operatingMode)) {
            opState.setOperatingMode(OperatingMode.EN);
        } else {
            opState.setOperatingMode(OperatingMode.DIS);
        }

        try {
            mdibAccess.writeStates(MdibStateModifications
                .create(MdibStateModifications.Type.OPERATION).add(opState));
        } catch (PreprocessingException e) {
            LOG.error("", e);
        }

        return ResponseTypes.Result.RESULT_SUCCESS;
    }

    private MeasurementValidity translateValidityFromT2IAPI(MetricTypes.MeasurementValidity validity) {
        switch (validity) {
            case MEASUREMENT_VALIDITY_VALID: return MeasurementValidity.VLD;
            case MEASUREMENT_VALIDITY_VALIDATED_DATA: return MeasurementValidity.VLDATED;
            case MEASUREMENT_VALIDITY_INVALID: return MeasurementValidity.INV;
            case MEASUREMENT_VALIDITY_OVERFLOW: return MeasurementValidity.OFLW;
            case MEASUREMENT_VALIDITY_UNDERFLOW: return MeasurementValidity.UFLW;
            case MEASUREMENT_VALIDITY_MEASUREMENT_ONGOING: return MeasurementValidity.ONG;
            case MEASUREMENT_VALIDITY_CALIBRATION_ONGOING: return MeasurementValidity.CALIB;
            case MEASUREMENT_VALIDITY_QUESTIONABLE: return MeasurementValidity.QST;
            default: return MeasurementValidity.NA;
        }
    }

    public void invokeNumericSetValueOperation(String opHandle, BigDecimal value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // NOTE: HighPriorityServices.setValue() is not public. We hence have to call it
        //       via Reflection, just like Guice does.
        Method setValue = null;
        final HighPriorityServices highPriorityServices = sdcDevice.getHighPriorityServices();
        for (Method m : highPriorityServices.getClass().getDeclaredMethods()) {
            final MessageInterceptor annotation = m.getAnnotation(MessageInterceptor.class);
            if (annotation != null
                && annotation.value().equals(ActionConstants.ACTION_SET_VALUE)
                && m.getParameterCount() == 1
                && m.getParameterTypes()[0] == RequestResponseObject.class) {
                setValue = m;
            }
        }
        if (setValue == null) {
            throw new RuntimeException("could not find method setValue() in HighPriorityServices(). "
                + "Don't know how to invoke a SetValue Operation without this method.");
        }
        // setValue is not public, we hence have to make it accessible first.
        setValue.setAccessible(true);

        Envelope requestEnvelope = new Envelope();
        SetValue setValueAny = new SetValue();
        setValueAny.setOperationHandleRef(opHandle);
        setValueAny.setRequestedNumericValue(value);
        final Body setValueBody = new Body();
        setValueBody.setAny(List.of(setValueAny));
        requestEnvelope.setBody(setValueBody);
        final SoapMessage request = soapMessageFactory.createSoapMessage(requestEnvelope);
        Envelope responseEnvelope = new Envelope();
        Body responseBody = new Body();
        SetValueResponse setValueResponse = new SetValueResponse();
        responseBody.setAny(List.of(setValueResponse));
        responseEnvelope.setBody(responseBody);
        SoapMessage response = soapMessageFactory.createSoapMessage(responseEnvelope);
        ApplicationInfo applicationInfo = new ApplicationInfo();
        TransportInfo transportInfo = new TransportInfo("http",
            "localhost", 8080, "localhost", 5001, List.of());
        CommunicationContext communicationContext = new CommunicationContext(applicationInfo, transportInfo);
        final RequestResponseObject requestResponseObject =
            new RequestResponseObject(request, response, communicationContext);
        setValue.invoke(highPriorityServices, requestResponseObject);
    }

    public String createContextStateWithAssociation(String descriptorHandle,
                                                    ContextTypes.ContextAssociation contextAssociation)
        throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException,
        NullPointerException {

        // TODO: not finished, yet. SDCcc Test Case Biceps:R0133 is still failing.

        final AbstractContextDescriptor descriptor =
            mdibAccess.getDescriptor(descriptorHandle, AbstractContextDescriptor.class).orElseThrow();

        List<AbstractContextState> states = new ArrayList<>();
        if (descriptor instanceof LocationContextDescriptor
            && ContextTypes.ContextAssociation.CONTEXT_ASSOCIATION_ASSOCIATED.equals(contextAssociation)) {
            // Requirement Biceps:R0124 requires us to disassociate any previously associated LocationContextState
            //             prior to associating a new one.
            final List<LocationContextState> locationContextStates =
                mdibAccess.getContextStates(descriptorHandle, LocationContextState.class);
            for (LocationContextState state : locationContextStates) {
                if (ContextAssociation.ASSOC.equals(state.getContextAssociation())) {
                    state.setContextAssociation(ContextAssociation.DIS);
                    states.add(state);
                }
            }
        } else if (descriptor instanceof PatientContextDescriptor
            && ContextTypes.ContextAssociation.CONTEXT_ASSOCIATION_ASSOCIATED.equals(contextAssociation)) {
            // disassociate any previously associated PatientContextState
            final List<PatientContextState> patientContextStates =
                mdibAccess.getContextStates(descriptorHandle, PatientContextState.class);
            for (PatientContextState state : patientContextStates) {
                if (ContextAssociation.ASSOC.equals(state.getContextAssociation())) {
                    state.setContextAssociation(ContextAssociation.DIS);
                    states.add(state);
                }
            }
        }

        final Class<? extends AbstractContextState> contextStateClassFromContextDescriptorClass =
            findContextStateClassFromContextDescriptorClass(descriptor.getClass());
        if (contextStateClassFromContextDescriptorClass == null) {
            throw new IllegalArgumentException("Handle belongs to descriptor of unknown type: " + descriptor.getClass().getName());
        }
        final AbstractContextState newContextState
            = contextStateClassFromContextDescriptorClass
                .getDeclaredConstructor()
                .newInstance();
        newContextState.setDescriptorHandle(descriptorHandle);
        newContextState.setHandle(createRandomHandle());
        newContextState.setContextAssociation(translateContextAssociationFromT2IAPI(contextAssociation));
        mdibAccess.getContextStates().add(newContextState);
        states.add(newContextState);

        // TODO: set @BindingMdibVersion
        // TODO: set @UnbindingMdibVersion
        // TODO: set @BindingTime
        // TODO: set @UnbindingTime

        try {
            mdibAccess.writeStates(MdibStateModifications
                .create(MdibStateModifications.Type.CONTEXT).addAll(states));
        } catch (PreprocessingException e) {
            LOG.error("", e);
        }

        return newContextState.getHandle();
    }

    private Class<? extends AbstractContextState> findContextStateClassFromContextDescriptorClass
        (Class<? extends AbstractContextDescriptor> aClass) {
        if (LocationContextDescriptor.class.equals(aClass)) {
            return LocationContextState.class;
        } else if (PatientContextDescriptor.class.equals(aClass)) {
            return PatientContextState.class;
        } else if (WorkflowContextDescriptor.class.equals(aClass)) {
            return WorkflowContextState.class;
        } else if (OperatorContextDescriptor.class.equals(aClass)) {
            return OperatorContextState.class;
        } else if (MeansContextDescriptor.class.equals(aClass)) {
            return MeansContextState.class;
        } else if (EnsembleContextDescriptor.class.equals(aClass)) {
            return EnsembleContextState.class;
        }
        return null;
    }

    protected String createRandomHandle() {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int c = (int)Math.floor(Math.random() * 36);
            if (c < 10) {
                res.append(Character.toString('0' + c));
            } else {
                res.append(Character.toString('a' + (c - 10)));
            }
        }
        return res.toString();
    }

    private ContextAssociation translateContextAssociationFromT2IAPI(ContextTypes.ContextAssociation contextAssociation) {
        switch (contextAssociation) {
            case CONTEXT_ASSOCIATION_ASSOCIATED: return ContextAssociation.ASSOC;
            case CONTEXT_ASSOCIATION_DISASSOCIATED: return ContextAssociation.DIS;
            case CONTEXT_ASSOCIATION_PRE_ASSOCIATED: return ContextAssociation.PRE;
            default: return ContextAssociation.NO;
        }
    }
}
