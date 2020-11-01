package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.DerivationMethod;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricValue;
import org.somda.sdc.biceps.model.participant.Range;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumericMetricRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.METRIC_2;
    private static final String HANDLE_MIN = HANDLE + "Min";
    private static final String HANDLE_MED = HANDLE + "Med";

    NumericMetricRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        mediumSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new NumericMetricDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.ONE);
            descriptor.setSafetyClassification(SafetyClassification.MED_A);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setDerivationMethod(DerivationMethod.MAN);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
            descriptor.setMaxMeasurementTime(Duration.ofHours(1));
            descriptor.setMaxDelayTime(Duration.ofHours(2));
            descriptor.setDeterminationPeriod(Duration.ofHours(3));
            descriptor.setLifeTimePeriod(Duration.ofHours(4));
            descriptor.setActivationDuration(Duration.ofHours(5));

            descriptor.setType(TypeCollection.CODED_VALUE);
            descriptor.setUnit(TypeCollection.CODED_VALUE);
            descriptor.setBodySite(List.of(TypeCollection.CODED_VALUE));
            descriptor.setRelation(List.of(TypeCollection.RELATION, TypeCollection.RELATION, TypeCollection.RELATION));

            descriptor.setResolution(BigDecimal.TEN);
            descriptor.setAveragingPeriod(Duration.ofHours(3));

            var range1 = new Range();
            range1.setAbsoluteAccuracy(BigDecimal.ONE);
            range1.setLower(BigDecimal.ZERO);
            range1.setUpper(BigDecimal.TEN);
            range1.setRelativeAccuracy(BigDecimal.valueOf(2));
            range1.setStepWidth(BigDecimal.valueOf(222));
            descriptor.setTechnicalRange(List.of(range1, new Range()));

            // TODO: Extension
//            descriptor.setExtension();
//            range1.setExtension();
        }

        var state = new NumericMetricState();
        {
            state.setStateVersion(BigInteger.valueOf(636345));
            state.setDescriptorHandle(HANDLE);
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(ComponentActivation.ON);
            state.setActiveDeterminationPeriod(Duration.ofMinutes(55));
            state.setLifeTimePeriod(Duration.ofHours(8));

            state.setBodySite(List.of(TypeCollection.CODED_VALUE));
            state.setPhysicalConnector(TypeCollection.PHYSICAL_CONNECTOR_INFO);

            state.setActiveAveragingPeriod(Duration.ofHours(77));

            var range1 = new Range();
            range1.setAbsoluteAccuracy(BigDecimal.ONE);
            range1.setLower(BigDecimal.ZERO);
            range1.setUpper(BigDecimal.TEN);
            range1.setRelativeAccuracy(BigDecimal.valueOf(2));
            range1.setStepWidth(BigDecimal.valueOf(222));
            state.setPhysiologicalRange(List.of(range1, TypeCollection.RANGE, new Range()));

            var value = new NumericMetricValue();
            value.setStartTime(UnitTestUtil.makeTestTimestamp());
            value.setStopTime(UnitTestUtil.makeTestTimestamp());
            value.setDeterminationTime(UnitTestUtil.makeTestTimestamp());
            value.setValue(BigDecimal.valueOf(1337));
            value.setMetricQuality(TypeCollection.METRIC_QUALITY);
            value.setAnnotation(List.of(TypeCollection.ANNOTATION));

            state.setMetricValue(value);

            // TODO: Extension
//            state.setExtension();
//            range1.setExtension();
//            value.setExtension();
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    // only mandatory fields in metric value set
    private void mediumSet(MdibDescriptionModifications modifications) {
        var descriptor = new NumericMetricDescriptor();
        {
            descriptor.setHandle(HANDLE_MED);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);

            descriptor.setUnit(TypeCollection.CODED_VALUE);

            descriptor.setResolution(BigDecimal.TEN);
        }

        var state = new NumericMetricState();
        {
            state.setDescriptorHandle(descriptor.getHandle());

            var quality = new AbstractMetricValue.MetricQuality();
            quality.setMode(GenerationMode.DEMO);

            var value = new NumericMetricValue();
            value.setMetricQuality(quality);

            state.setMetricValue(value);
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new NumericMetricDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);

            descriptor.setUnit(TypeCollection.CODED_VALUE);

            descriptor.setResolution(BigDecimal.TEN);
        }

        var state = new NumericMetricState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, NumericMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, NumericMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, NumericMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, NumericMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, NumericMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, NumericMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, NumericMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, NumericMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }

}
