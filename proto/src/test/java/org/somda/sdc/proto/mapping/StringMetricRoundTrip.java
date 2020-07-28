package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.math.BigInteger;
import java.time.Duration;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringMetricRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {


    public StringMetricRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new StringMetricDescriptor();
        {
            descriptor.setHandle(Handles.METRIC_0);
            descriptor.setDescriptorVersion(BigInteger.ONE);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
        }

        var state = new StringMetricState();
        {
            state.setDescriptorHandle(Handles.METRIC_0);
            state.setStateVersion(BigInteger.TEN);
            state.setActivationState(ComponentActivation.NOT_RDY);
            state.setActiveDeterminationPeriod(Duration.ofHours(2));
            state.setLifeTimePeriod(Duration.ofHours(2));

            var value = new StringMetricValue();
            value.setValue("ﾟ･✿ヾ╲(｡◕‿◕｡)╱✿･ﾟ");

            state.setMetricValue(value);
        }

        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }


    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.METRIC_0, StringMetricDescriptor.class).get();
        var expectedState = localMdibAccess.getState(Handles.METRIC_0, StringMetricState.class).get();
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.METRIC_0, StringMetricDescriptor.class).get();
        var actualState = remoteMdibAccess.getState(Handles.METRIC_0, StringMetricState.class).get();

        assertEquals(expectedDescriptor.getHandle(), actualDescriptor.getHandle());
        assertEquals(expectedDescriptor.getDescriptorVersion(), actualDescriptor.getDescriptorVersion());
        assertEquals(expectedDescriptor.getMetricCategory(), actualDescriptor.getMetricCategory());
        assertEquals(expectedDescriptor.getMetricAvailability(), expectedDescriptor.getMetricAvailability());

        assertEquals(expectedState.getDescriptorHandle(), actualState.getDescriptorHandle());
        assertEquals(expectedState.getStateVersion(), actualState.getStateVersion());
        assertEquals(expectedState.getActivationState(), actualState.getActivationState());
        assertEquals(expectedState.getActiveDeterminationPeriod(), actualState.getActiveDeterminationPeriod());
        assertEquals(expectedState.getLifeTimePeriod(), actualState.getLifeTimePeriod());

        assertEquals(expectedState.getMetricValue().getValue(), actualState.getMetricValue().getValue());
    }
}
