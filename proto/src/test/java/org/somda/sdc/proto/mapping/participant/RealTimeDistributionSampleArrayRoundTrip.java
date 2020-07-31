package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.DerivationMethod;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
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
        // TODO: Complete
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
            descriptor.setLifeTimePeriod(Duration.ofMinutes(555));
            descriptor.setActivationDuration(Duration.ofSeconds(1));
            descriptor.setResolution(BigDecimal.valueOf(123123123));
            descriptor.setSamplePeriod(Duration.ofDays(365));
        }
        var state = new RealTimeSampleArrayMetricState();
        {
            state.setStateVersion(BigInteger.ZERO);
            state.setDescriptorHandle(HANDLE);
            state.setDescriptorVersion(BigInteger.TEN);
            state.setActivationState(ComponentActivation.FAIL);
            state.setActiveDeterminationPeriod(Duration.ofDays(4));
            state.setLifeTimePeriod(Duration.ofMillis(1));
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new RealTimeSampleArrayMetricDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setMetricCategory(MetricCategory.MSRMT);
            descriptor.setMetricAvailability(MetricAvailability.CONT);
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
