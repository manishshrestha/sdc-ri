package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.DerivationMethod;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.Range;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.SampleArrayValue;
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

public class RealTimeDistributionSampleArrayRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.METRIC_3;
    private static final String HANDLE_MIN = HANDLE + "Min";

    RealTimeDistributionSampleArrayRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new RealTimeSampleArrayMetricDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.TEN);
            descriptor.setSafetyClassification(SafetyClassification.INF);
            descriptor.setMetricCategory(MetricCategory.MSRMT);
            descriptor.setDerivationMethod(DerivationMethod.AUTO);
            descriptor.setMetricAvailability(MetricAvailability.CONT);
            descriptor.setMaxMeasurementTime(Duration.ofMinutes(444));
            descriptor.setMaxDelayTime(Duration.ofMinutes(433));
            descriptor.setDeterminationPeriod(Duration.ofHours(1));
            descriptor.setLifeTimePeriod(Duration.ofMinutes(555));
            descriptor.setActivationDuration(Duration.ofSeconds(1));

            descriptor.setType(TypeCollection.CODED_VALUE);
            descriptor.setUnit(TypeCollection.CODED_VALUE);
            descriptor.setBodySite(List.of(TypeCollection.CODED_VALUE, TypeCollection.CODED_VALUE));
            descriptor.setRelation(List.of(TypeCollection.RELATION));

            descriptor.setResolution(BigDecimal.valueOf(123123123));
            descriptor.setSamplePeriod(Duration.ofDays(365));

            descriptor.setTechnicalRange(List.of(TypeCollection.RANGE, TypeCollection.RANGE, new Range()));
        }
        var state = new RealTimeSampleArrayMetricState();
        {
            state.setStateVersion(BigInteger.ZERO);
            state.setDescriptorHandle(HANDLE);
            state.setDescriptorVersion(BigInteger.TEN);
            state.setActivationState(ComponentActivation.FAIL);
            state.setActiveDeterminationPeriod(Duration.ofDays(4));
            state.setLifeTimePeriod(Duration.ofMillis(1));

            state.setBodySite(List.of(TypeCollection.CODED_VALUE));
            state.setPhysicalConnector(TypeCollection.PHYSICAL_CONNECTOR_INFO);

            var annotation = new SampleArrayValue.ApplyAnnotation();
            annotation.setAnnotationIndex(1);
            annotation.setSampleIndex(3);

            var value = new SampleArrayValue();
            value.setStartTime(UnitTestUtil.makeTestTimestamp());
            value.setStopTime(UnitTestUtil.makeTestTimestamp());
            value.setDeterminationTime(UnitTestUtil.makeTestTimestamp());
            value.setSamples(List.of(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.valueOf(2)));

            value.setMetricQuality(TypeCollection.METRIC_QUALITY);
            value.setAnnotation(List.of(TypeCollection.ANNOTATION));

            value.setApplyAnnotation(List.of(annotation, annotation));

            state.setPhysiologicalRange(List.of(TypeCollection.RANGE, new Range()));
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new RealTimeSampleArrayMetricDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setMetricCategory(MetricCategory.MSRMT);
            descriptor.setMetricAvailability(MetricAvailability.CONT);

            descriptor.setUnit(TypeCollection.CODED_VALUE);

            descriptor.setResolution(BigDecimal.valueOf(123123123));
            descriptor.setSamplePeriod(Duration.ofDays(365));
        }
        var state = new RealTimeSampleArrayMetricState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, RealTimeSampleArrayMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, RealTimeSampleArrayMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, RealTimeSampleArrayMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, RealTimeSampleArrayMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, RealTimeSampleArrayMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, RealTimeSampleArrayMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, RealTimeSampleArrayMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, RealTimeSampleArrayMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
