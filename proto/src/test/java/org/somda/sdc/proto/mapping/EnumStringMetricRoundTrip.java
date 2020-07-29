package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumStringMetricRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    public EnumStringMetricRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new EnumStringMetricDescriptor();
        {
            descriptor.setHandle(Handles.METRIC_1);
            descriptor.setDescriptorVersion(BigInteger.ONE);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);

            var allowed1 = new EnumStringMetricDescriptor.AllowedValue();
            allowed1.setValue("٩(×̯×)۶");

            var allowed2 = new EnumStringMetricDescriptor.AllowedValue();
            allowed2.setValue("ಭ_ಭ");

            descriptor.setAllowedValue(List.of(allowed1, allowed2));
        }

        var state = new EnumStringMetricState();
        {
            state.setDescriptorHandle(Handles.METRIC_1);
            state.setStateVersion(BigInteger.TEN);
            state.setActivationState(ComponentActivation.NOT_RDY);
            state.setActiveDeterminationPeriod(Duration.ofHours(2));
            state.setLifeTimePeriod(Duration.ofHours(2));

            var value = new StringMetricValue();
            value.setValue("ಭ_ಭ");

            state.setMetricValue(value);
        }

        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.METRIC_1, EnumStringMetricDescriptor.class).get();
        var expectedState = localMdibAccess.getState(Handles.METRIC_1, EnumStringMetricState.class).get();
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.METRIC_1, EnumStringMetricDescriptor.class).get();
        var actualState = remoteMdibAccess.getState(Handles.METRIC_1, EnumStringMetricState.class).get();

        assertEquals(expectedDescriptor.getHandle(), actualDescriptor.getHandle());
        assertEquals(expectedDescriptor.getDescriptorVersion(), actualDescriptor.getDescriptorVersion());
        assertEquals(expectedDescriptor.getMetricCategory(), actualDescriptor.getMetricCategory());
        assertEquals(expectedDescriptor.getMetricAvailability(), expectedDescriptor.getMetricAvailability());
        assertEquals(expectedDescriptor.getAllowedValue(), actualDescriptor.getAllowedValue());

        assertEquals(expectedState.getDescriptorHandle(), actualState.getDescriptorHandle());
        assertEquals(expectedState.getStateVersion(), actualState.getStateVersion());
        assertEquals(expectedState.getActivationState(), actualState.getActivationState());
        assertEquals(expectedState.getActiveDeterminationPeriod(), actualState.getActiveDeterminationPeriod());
        assertEquals(expectedState.getLifeTimePeriod(), actualState.getLifeTimePeriod());

        assertEquals(expectedState.getMetricValue().getValue(), actualState.getMetricValue().getValue());
    }
}
