package org.somda.sdc.proto.mapping;

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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumericMetricRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    NumericMetricRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new NumericMetricDescriptor();
        {
            descriptor.setHandle(Handles.METRIC_2);
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
            state.setDescriptorHandle(Handles.METRIC_2);
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
            value.setDeterminationTime(Instant.now());

            var quality = new AbstractMetricValue.MetricQuality();
            value.setMetricQuality(quality);
            quality.setValidity(MeasurementValidity.INV);
            quality.setMode(GenerationMode.DEMO);
            quality.setQi(BigDecimal.valueOf(7331));
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.METRIC_2, NumericMetricDescriptor.class).get();
        var expectedState = localMdibAccess.getState(Handles.METRIC_2, NumericMetricState.class).get();
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.METRIC_2, NumericMetricDescriptor.class).get();
        var actualState = remoteMdibAccess.getState(Handles.METRIC_2, NumericMetricState.class).get();

        assertEquals(expectedDescriptor, actualDescriptor);
        assertEquals(expectedState, actualState);
    }

}
