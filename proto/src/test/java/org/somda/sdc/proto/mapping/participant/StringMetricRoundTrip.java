package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.DerivationMethod;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
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
        var descriptor = new StringMetricDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.ONE);
            descriptor.setSafetyClassification(SafetyClassification.INF);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setDerivationMethod(DerivationMethod.AUTO);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
            descriptor.setMaxMeasurementTime(Duration.ofSeconds(8));
            descriptor.setMaxDelayTime(Duration.ofMinutes(5));
            descriptor.setDeterminationPeriod(Duration.ofHours(3));
            descriptor.setLifeTimePeriod(Duration.ofDays(2));
            descriptor.setActivationDuration(Duration.ofDays(7));

            descriptor.setType(TypeCollection.CODED_VALUE);
            descriptor.setUnit(TypeCollection.CODED_VALUE);
            descriptor.setBodySite(List.of(TypeCollection.CODED_VALUE, TypeCollection.CODED_VALUE));
            descriptor.setRelation(List.of(TypeCollection.RELATION));
        }

        var state = new StringMetricState();
        {
            state.setStateVersion(BigInteger.TEN);
            state.setDescriptorHandle(HANDLE);
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(ComponentActivation.NOT_RDY);
            state.setActiveDeterminationPeriod(Duration.ofHours(2));
            state.setLifeTimePeriod(Duration.ofHours(2));

            state.setBodySite(List.of(TypeCollection.CODED_VALUE));
            state.setPhysicalConnector(TypeCollection.PHYSICAL_CONNECTOR_INFO);

            var value = new StringMetricValue();
            value.setValue("ﾟ･✿ヾ╲(｡◕‿◕｡)╱✿･ﾟ");
            value.setMetricQuality(TypeCollection.METRIC_QUALITY);

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
            descriptor.setUnit(TypeCollection.CODED_VALUE);
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
