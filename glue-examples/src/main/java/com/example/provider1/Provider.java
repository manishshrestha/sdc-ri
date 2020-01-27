package com.example.provider1;

import com.example.ProviderMdibConstants;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.Timestamp;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.factory.DpwsFrameworkFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.LocationDetailQueryMapper;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.mdpws.common.CommonConstants;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is an example provider using an mdib provided as XML as well as TLS secured communication.
 *
 * It is the complement to {@link com.example.consumer1.Consumer} in terms of used
 * functionality and features.
 */
public class Provider extends AbstractIdleService {
    static {
        // TODO: Workaround for
        //  javax.net.ssl.SSLHandshakeException: No subject alternative names present
        //  when using the jersey client
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    private static final ProviderUtil IT = new ProviderUtil();
    private static final Logger LOG = LoggerFactory.getLogger(Provider.class);

    private Injector injector;
    private LocalMdibAccess mdibAccess;
    private DpwsFramework dpwsFramework;
    private SdcDevice sdcDevice;

    private InstanceIdentifier instanceIdentifier;
    private Optional<LocationDetail> currentLocation;

    /**
     * Create an instance of an SDC Provider
     * @param networkAdapterName name of the adapter the provider bindes to, e.g. eth1
     * @param eprAddress WS-Addressing EndpointReference Address element
     * @throws SocketException thrown if network adapter cannot be set up
     */
    public Provider(String networkAdapterName, String eprAddress) throws SocketException {
        this.injector = IT.getInjector();
        final NetworkInterface networkInterface = NetworkInterface.getByName(networkAdapterName);
        this.dpwsFramework = injector.getInstance(DpwsFrameworkFactory.class).createDpwsFramework(networkInterface);
        this.mdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();

        var handler = new OperationHandler(this.mdibAccess);
        this.sdcDevice = injector.getInstance(SdcDeviceFactory.class).createSdcDevice(new DeviceSettings() {
            @Override
            public EndpointReferenceType getEndpointReference() {
                return injector.getInstance(WsAddressingUtil.class)
                        .createEprWithAddress(eprAddress);
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                return networkInterface;
            }
        }, this.mdibAccess, List.of(handler));

        // set SDC types
        this.sdcDevice.getDiscoveryAccess().setTypes(List.of(CommonConstants.MEDICAL_DEVICE_TYPE));

        this.instanceIdentifier = new InstanceIdentifier();
        this.instanceIdentifier.setRootName("AwesomeExampleInstance");

        this.currentLocation = Optional.empty();
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

        final ModificationsBuilderFactory modificationsBuilderFactory = injector.getInstance(ModificationsBuilderFactory.class);

        // load initial mdib from file
        final MdibXmlIo mdibXmlIo = injector.getInstance(MdibXmlIo.class);
        InputStream mdibAsStream = Provider.class.getClassLoader().getResourceAsStream("provider1/mdib.xml");
        if (mdibAsStream == null) {
            throw new RuntimeException("Could not load mdib.xml as resource");
        }
        final Mdib mdib = mdibXmlIo.readMdib(mdibAsStream);
        final MdibDescriptionModifications modifications = modificationsBuilderFactory.createModificationsBuilder(mdib).get();
        mdibAccess.writeDescription(modifications);

        dpwsFramework.startAsync().awaitRunning();
        sdcDevice.startAsync().awaitRunning();

        if (currentLocation.isPresent()) {
            // update the location again to match mdib and scopes
            this.setLocation(currentLocation.get());
        }
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
        LOG.info("Updating location scopes");
        sdcDevice.getDiscoveryAccess().setScopes(
                List.of(
                        LocationDetailQueryMapper.createWithLocationDetailQuery(this.instanceIdentifier, location),
                        GlueConstants.SCOPE_SDC_PROVIDER
                )
        );
        if (this.isRunning() || this.state() == State.STARTING) {
            LOG.info("Updating location context");
            final MdibStateModifications locMod = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
            // update location state
            try (ReadTransaction readTransaction = mdibAccess.startTransaction()) {
                final var locDesc = readTransaction.getDescriptor(ProviderMdibConstants.HANDLE_LOCATIONCONTEXT, LocationContextDescriptor.class).orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", ProviderMdibConstants.HANDLE_LOCATIONCONTEXT)));

                var locState = new LocationContextState();
                locState.setLocationDetail(location);
                locState.setDescriptorVersion(locDesc.getDescriptorVersion());
                locState.setDescriptorHandle(locDesc.getHandle());
                locState.setStateVersion(BigInteger.ONE);
                locState.setHandle(locDesc.getHandle() + "State");
                locState.setBindingMdibVersion(mdibAccess.getMdibVersion().getVersion());
                locState.setContextAssociation(ContextAssociation.ASSOC);
                locState.getValidator().add(this.instanceIdentifier);

                locMod.add(locState);
            }
            mdibAccess.writeStates(locMod);
        }
        this.currentLocation = Optional.of(location);
    }

    private void addMetricQuality(AbstractMetricValue val) {
        if (val.getMetricQuality() == null) {
            var qual = new AbstractMetricValue.MetricQuality();
            qual.setMode(GenerationMode.DEMO);
            qual.setValidity(MeasurementValidity.VLD);
            val.setMetricQuality(qual);
        }
    }

    /**
     * Adds a sine wave to the data of a waveform
     * @param handle descriptor handle of waveform state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeWaveform(String handle) throws PreprocessingException {
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.WAVEFORM);

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
            int sampleCapacity = 100;

            // sine wave
            var values = new LinkedList<BigDecimal>();
            double delta = 2 * Math.PI / sampleCapacity;
            IntStream.range(0, sampleCapacity).forEachOrdered(n -> {
                values.add(
                        new BigDecimal((Math.sin(n * delta) + 1) / 2.0 * (maxValue - minValue) + minValue)
                                .setScale(15, RoundingMode.DOWN));
            });
            sampleArrayValue.setSamples(values);
            sampleArrayValue.setDeterminationTime(Timestamp.now());

            state.setMetricValue(sampleArrayValue);

            modifications.add(state);
        }

        mdibAccess.writeStates(modifications);
    }

    /**
     * Increments the value of a NumericMetricState.
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeNumericMetric(String handle) throws PreprocessingException {
        Optional<NumericMetricState> state = mdibAccess.getState(handle, NumericMetricState.class);
        NumericMetricState clone = (NumericMetricState) state.get().clone();
        var val = clone.getMetricValue();
        if (val != null && val.getValue() != null) {
            val.setValue(val.getValue().add(BigDecimal.ONE));
        } else {
            val = new NumericMetricValue();
            val.setValue(BigDecimal.ONE);
        }
        val.setDeterminationTime(Timestamp.now());

        addMetricQuality(val);

        clone.setMetricValue(val);
        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(clone));
    }

    /**
     * Changes the content of a StringMetricState, toggling between "UPPERCASE" and "lowercase" as content.
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeStringMetric(String handle) throws PreprocessingException {
        Optional<StringMetricState> state = mdibAccess.getState(handle, StringMetricState.class);
        StringMetricState clone = (StringMetricState) state.get().clone();
        var val = clone.getMetricValue();
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
        val.setDeterminationTime(Timestamp.now());

        addMetricQuality(val);

        clone.setMetricValue(val);
        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(clone));
    }

    /**
     * Changes the content of an EnumStringMetricState, selecting the next allowed value.
     * @param handle descriptor handle of metric state
     * @throws PreprocessingException if changes could not be committed to mdib
     */
    public void changeEnumStringMetric(String handle) throws PreprocessingException {
        Optional<MdibEntity> entityOpt = mdibAccess.getEntity(handle);
        MdibEntity mdibEntity = entityOpt.get();
        EnumStringMetricDescriptor descriptor = (EnumStringMetricDescriptor) mdibEntity.getDescriptor();

        List<String> allowedValue = descriptor.getAllowedValue().stream().map(x -> x.getValue()).collect(Collectors.toList());

        EnumStringMetricState state = (EnumStringMetricState) mdibEntity.getStates().get(0);
        EnumStringMetricState clone = (EnumStringMetricState) state.clone();

        var val = clone.getMetricValue();
        if (val != null && val.getValue() != null) {
            var actVal = val.getValue();

            Iterator<String> iter = Iterables.cycle(allowedValue).iterator();
            String next = iter.next();
            int i = 0;
            while (iter.hasNext() && i < 17) {
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
        val.setDeterminationTime(Timestamp.now());
        addMetricQuality(val);
        clone.setMetricValue(val);
        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(clone));
    }

    /**
     * Toggles an AlertSignalState and AlertConditionState between presence on and off
     * @param signalHandle signal handle to toggle
     * @param conditionHandle matching condition to toggle
     */
    public void changeAlertStuff(String signalHandle, String conditionHandle) {

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
            mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.ALERT).add(conditionState).add(signalState));
        } catch (PreprocessingException e) {
            LOG.error("", e);
        }

    }


    public static void main(String[] args) throws IOException, PreprocessingException {
        final Injector injector = IT.getInjector();

        Provider provider = new Provider(
                "eth1",
                "urn:uuid:857bf583-8a51-475f-a77f-d0ca7de69b11"
        );

        // set a location for scopes
        var loc = new LocationDetail();
        loc.setBed("TopBunk");
        loc.setPoC("LD1");
        loc.setFacility("sdcri");
        provider.setLocation(loc);

        provider.startAsync().awaitRunning();

        // generate some data
        var t1 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    provider.changeWaveform(ProviderMdibConstants.HANDLE_WAVEFORM);
                } catch (InterruptedException | PreprocessingException e) {
                    LOG.error("Thread loop stopping", e);
                    break;
                }
            }
        });
        t1.setDaemon(true);
        t1.start();

        var t2 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    provider.changeNumericMetric(ProviderMdibConstants.HANDLE_NUMERIC_DYNAMIC);
                    provider.changeStringMetric(ProviderMdibConstants.HANDLE_STRING_DYNAMIC);
                    provider.changeEnumStringMetric(ProviderMdibConstants.HANDLE_ENUM_DYNAMIC);
                    provider.changeAlertStuff(ProviderMdibConstants.HANDLE_ALERT_SIGNAL, ProviderMdibConstants.HANDLE_ALERT_CONDITION);
                } catch (InterruptedException | PreprocessingException e) {
                    LOG.error("Thread loop stopping", e);
                    break;
                }
            }
        });
        t2.setDaemon(true);
        t2.start();

        int read = System.in.read();

        t1.interrupt();
        t2.interrupt();

        provider.stopAsync().awaitTerminated();
    }
}
