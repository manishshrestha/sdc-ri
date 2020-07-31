package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricValue;
import org.somda.sdc.biceps.model.participant.Range;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumericMetricRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.METRIC_2;
    private static final String HANDLE_MIN = HANDLE + "Min";

    NumericMetricRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        // TODO: Complete
        var descriptor = new NumericMetricDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.ONE);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
            descriptor.setAveragingPeriod(Duration.ofHours(3));
            descriptor.setResolution(BigDecimal.TEN);

            var range1 = new Range();
            range1.setAbsoluteAccuracy(BigDecimal.ONE);
            range1.setLower(BigDecimal.ZERO);
            range1.setUpper(BigDecimal.TEN);
            range1.setRelativeAccuracy(BigDecimal.valueOf(2));
            range1.setStepWidth(BigDecimal.valueOf(222));
            descriptor.setTechnicalRange(List.of(range1));
        }

        var state = new NumericMetricState();
        {
            state.setDescriptorHandle(HANDLE);
            state.setActiveAveragingPeriod(Duration.ofMinutes(55));

            var range1 = new Range();
            range1.setAbsoluteAccuracy(BigDecimal.ONE);
            range1.setLower(BigDecimal.ZERO);
            range1.setUpper(BigDecimal.TEN);
            range1.setRelativeAccuracy(BigDecimal.valueOf(2));
            range1.setStepWidth(BigDecimal.valueOf(222));
            state.setPhysiologicalRange(List.of(range1));

            var value = new NumericMetricValue();
            state.setMetricValue(value);
            value.setValue(BigDecimal.valueOf(1337));
            value.setDeterminationTime(UnitTestUtil.makeTestTimestamp());

            var quality = new AbstractMetricValue.MetricQuality();
            value.setMetricQuality(quality);
            quality.setValidity(MeasurementValidity.INV);
            quality.setMode(GenerationMode.DEMO);
            quality.setQi(BigDecimal.valueOf(7331));
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new NumericMetricDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
            descriptor.setResolution(BigDecimal.TEN);

            var range1 = new Range();
            descriptor.setTechnicalRange(List.of(range1));
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
