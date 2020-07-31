package org.somda.sdc.proto.mapping.participant;

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

    private static final String HANDLE = Handles.METRIC_0;
    private static final String HANDLE_MIN = HANDLE + "Min";

    public StringMetricRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    public void bigSet(MdibDescriptionModifications modifications) {
        // TODO: Complete
        var descriptor = new StringMetricDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.ONE);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
        }

        var state = new StringMetricState();
        {
            state.setDescriptorHandle(HANDLE);
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

    public void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new StringMetricDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
        }

        var state = new StringMetricState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
        }

        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, StringMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, StringMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, StringMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, StringMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, StringMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, StringMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, StringMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, StringMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
