package it.org.somda.sdc.proto.example1.provider;

import com.example.Constants;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.proto.provider.SdcDevice;
import org.somda.sdc.proto.provider.factory.SdcDeviceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
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


public class Provider extends AbstractIdleService {

    private static final Logger LOG = LogManager.getLogger(com.example.provider1.Provider.class);

    private static final int MAX_ENUM_ITERATIONS = 17;

    private Injector injector;
    private LocalMdibAccess mdibAccess;
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

        var serverAddr = new InetSocketAddress("127.0.0.1", 13373);
        var epr = "urn:uuid:" + UUID.randomUUID().toString();
        this.mdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();

        var handler = new OperationHandler(this.mdibAccess);
        String finalEpr = epr;
        this.sdcDevice = injector.getInstance(SdcDeviceFactory.class).createSdcDevice(
                epr, serverAddr, mdibAccess, Collections.emptyList(), Collections.emptyList()
        );
        this.instanceIdentifier = new InstanceIdentifier();
        this.instanceIdentifier.setRootName("AwesomeExampleInstance");

        this.currentLocation = null;
    }

    @Override
    protected void startUp() throws Exception {
        // load mdib
        final ModificationsBuilderFactory modificationsBuilderFactory = injector.getInstance(ModificationsBuilderFactory.class);

        var mdibXmlIo = injector.getInstance(MdibXmlIo.class);
        InputStream mdibAsStream = Provider.class.getClassLoader().getResourceAsStream("example1/mdib.xml");
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
        this.sdcDevice.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        this.sdcDevice.stopAsync().awaitTerminated();

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
        NumericMetricState state = stateOpt.get();
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
        StringMetricState state = stateOpt.get();
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
        MdibEntity mdibEntity = entityOpt.get();
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
     * Toggles an AlertSignalState and AlertConditionState between presence on and off.
     *
     * @param signalHandle    signal handle to toggle
     * @param conditionHandle matching condition to toggle
     */
    public void changeAlertSignalAndConditionPresence(String signalHandle, String conditionHandle) {

        var signalEntity = mdibAccess.getEntity(signalHandle).get();
        var conditionEntity = mdibAccess.getEntity(conditionHandle).get();

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

    public static void main(String[] args) throws Exception {
        var util = new ProviderUtil(args);

        var targetFacility = System.getenv().getOrDefault("ref_fac", Constants.DEFAULT_FACILITY);
        var targetBed = System.getenv().getOrDefault("ref_bed", Constants.DEFAULT_BED);
        var targetPoC = System.getenv().getOrDefault("ref_poc", Constants.DEFAULT_POC);

        var provider = new Provider(util);

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
}
