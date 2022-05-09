package com.example.provider1;

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
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalPresence;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricValue;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.SampleArrayValue;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
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

    private Injector injector;
    private LocalMdibAccess mdibAccess;
    private DpwsFramework dpwsFramework;
    private SdcDevice sdcDevice;

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

        var epr = providerUtil.getEpr();
        if (epr == null) {
            epr = "urn:uuid:" + UUID.randomUUID().toString();
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

        this.instanceIdentifier = InstanceIdentifier.builder()
            .withRootName("AwesomeExampleInstance")
            .build();

        this.currentLocation = null;
    }

    @Override
    protected void startUp() throws Exception {
        DpwsUtil dpwsUtil = injector.getInstance(DpwsUtil.class);

        sdcDevice.getHostingServiceAccess().setThisDevice(ThisDeviceType.builder()
            .withFriendlyName(dpwsUtil.createLocalizedStrings()
                .add("en", "Provider Example Unit")
                .get())
            .withFirmwareVersion("v1.2.3")
            .withSerialNumber("1234-5678-9101-1121")
            .build());

        sdcDevice.getHostingServiceAccess().setThisModel(
            ThisModelType.builder()
                .withManufacturer(dpwsUtil.createLocalizedStrings()
                        .add("en", "Provider Example Inc.")
                        .add("de", "Beispiel Provider AG")
                        .add("cn", "范例公司")
                        .get())
            .withManufacturerUrl("http://www.example.com")
            .withModelName(dpwsUtil.createLocalizedStrings()
                        .add("PEU")
                        .get())
            .withModelNumber("54-32-1")
            .withPresentationUrl("http://www.example.com")
            .build()
        );

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

                var locState = LocationContextState.builder()
                    .withLocationDetail(location)
                    .withDescriptorVersion(locDesc.getDescriptorVersion())
                    .withDescriptorHandle(locDesc.getHandle())
                    .withStateVersion(BigInteger.ONE)
                    .withHandle(locDesc.getHandle() + "State")
                    .withBindingMdibVersion(mdibAccess.getMdibVersion().getVersion())
                    .withContextAssociation(ContextAssociation.ASSOC)
                    .addValidator(this.instanceIdentifier)
                    .addIdentification(this.instanceIdentifier);

                locMod.add(locState.build());
            }
            mdibAccess.writeStates(locMod);
        }
        this.currentLocation = location.clone();
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

            final var metricQuality = SampleArrayValue.MetricQuality.builder()
                .withMode(GenerationMode.REAL)
                .withValidity(MeasurementValidity.VLD);

            var sampleArrayValue = SampleArrayValue.builder()
                .withMetricQuality(metricQuality.build());

            int minValue = 0;
            int maxValue = 50;
            int sampleCapacity = 10;

            // sine wave
            var values = new LinkedList<BigDecimal>();
            double delta = 2 * Math.PI / sampleCapacity;
            IntStream.range(0, sampleCapacity).forEachOrdered(n -> {
                values.add(
                        BigDecimal.valueOf((Math.sin(n * delta) + 1) / 2.0 * (maxValue - minValue) + minValue)
                                .setScale(15, RoundingMode.DOWN));
            });
            sampleArrayValue.withSamples(values)
                .withDeterminationTime(Instant.now());

            modifications.add(state.newCopyBuilder().withMetricValue(sampleArrayValue.build()).build());
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
        NumericMetricState state = stateOpt.get();
        var oldVal = state.getMetricValue();
        NumericMetricValue.Builder<?> newValBuilder;

        if (oldVal != null && oldVal.getValue() != null) {
            newValBuilder = oldVal.newCopyBuilder();
            newValBuilder.withValue(oldVal.getValue().add(BigDecimal.ONE));
        } else {
            newValBuilder = NumericMetricValue.builder();
            newValBuilder.withValue(BigDecimal.ONE);
        }
        newValBuilder.withDeterminationTime(Instant.now());
        ProviderUtil.addMetricQualityDemo(newValBuilder);

        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(
            state.newCopyBuilder().withMetricValue(newValBuilder.build()).build()
        ));
    }

    /**
     * Changes the content of a StringMetricState, toggling between "UPPERCASE" and "lowercase" as content.
     *
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeStringMetric(String handle) throws PreprocessingException {
        Optional<StringMetricState> stateOpt = mdibAccess.getState(handle, StringMetricState.class);
        StringMetricState state = stateOpt.get();
        var oldVal = state.getMetricValue();
        StringMetricValue.Builder<?> newVal;
        if (oldVal != null && oldVal.getValue() != null) {
            newVal = oldVal.newCopyBuilder();
            var actVal = oldVal.getValue();
            if (actVal.equals("UPPERCASE")) {
                newVal.withValue("lowercase");
            } else {
                newVal.withValue("UPPERCASE");
            }
        } else {
            newVal = StringMetricValue.builder();
            newVal.withValue("initial VALUE");
        }
        newVal.withDeterminationTime(Instant.now());
        ProviderUtil.addMetricQualityDemo(newVal);

        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(
            state.newCopyBuilder().withMetricValue(newVal.build()).build()
        ));
    }

    /**
     * Changes the content of an EnumStringMetricState, selecting the next allowed value.
     *
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeEnumStringMetric(String handle) throws PreprocessingException {
        Optional<MdibEntity> entityOpt = mdibAccess.getEntity(handle);
        MdibEntity mdibEntity = entityOpt.get();
        EnumStringMetricDescriptor descriptor = (EnumStringMetricDescriptor) mdibEntity.getDescriptor();

        List<String> allowedValue =
                descriptor.getAllowedValue().stream().map(x -> x.getValue()).collect(Collectors.toList());

        EnumStringMetricState state = (EnumStringMetricState) mdibEntity.getStates().get(0);
        var oldVal = state.getMetricValue();
        var newVal = StringMetricValue.builder();

        if (oldVal != null && oldVal.getValue() != null) {
            var actVal = oldVal.getValue();

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

            newVal.withValue(next);
        } else {
            newVal.withValue(allowedValue.get(0));
        }
        newVal.withDeterminationTime(Instant.now());
        ProviderUtil.addMetricQualityDemo(newVal);

        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(
            state.newCopyBuilder().withMetricValue(newVal.build()).build()
        ));
    }

    /**
     * Toggles an AlertSignalState and AlertConditionState between presence on and off.
     *
     * @param signalHandle    signal handle to toggle
     * @param conditionHandle matching condition to toggle
     */
    public void changeAlertSignalAndConditionPresence(String signalHandle, String conditionHandle) {

        var signalEntity = mdibAccess.getEntity(signalHandle).get();
        var conditionEntity = mdibAccess.getEntity(conditionHandle).get();

        AlertSignalState oldSignalState = (AlertSignalState) signalEntity.getStates().get(0);
        AlertConditionState oldConditionState = (AlertConditionState) conditionEntity.getStates().get(0);

        var newSignalState = oldSignalState.newCopyBuilder();
        var newConditionState = oldConditionState.newCopyBuilder();

        if (oldSignalState.getPresence() == null || oldSignalState.getPresence() == AlertSignalPresence.ON) {
            newSignalState.withPresence(AlertSignalPresence.OFF);
            newConditionState.withPresence(false);
        } else {
            newSignalState.withPresence(AlertSignalPresence.ON);
            newConditionState.withPresence(true);
        }

        try {
            mdibAccess.writeStates(
                MdibStateModifications
                    .create(MdibStateModifications.Type.ALERT)
                    .add(newConditionState.build())
                    .add(newSignalState.build())
            );
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
        var loc = LocationDetail.builder()
            .withBed(targetBed)
            .withPoC(targetPoC)
            .withFacility(targetFacility);
        provider.setLocation(loc.build());

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
}
