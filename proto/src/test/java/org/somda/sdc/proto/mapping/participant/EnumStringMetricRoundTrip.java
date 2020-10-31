package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.DerivationMethod;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumStringMetricRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.METRIC_1;
    private static final String HANDLE_MIN = HANDLE + "Min";

    public EnumStringMetricRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    public void bigSet(MdibDescriptionModifications modifications) {
        // TODO: Complete
        var descriptor = new EnumStringMetricDescriptor();
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

            var allowed1 = new EnumStringMetricDescriptor.AllowedValue();
            allowed1.setValue("٩(×̯×)۶");
            allowed1.setType(TypeCollection.CODED_VALUE);
            allowed1.setIdentification(TypeCollection.INSTANCE_IDENTIFIER);
            allowed1.setCharacteristic(TypeCollection.MEASUREMENT);

            var allowed2 = new EnumStringMetricDescriptor.AllowedValue();
            allowed2.setValue("ಭ_ಭ");

            descriptor.setAllowedValue(List.of(allowed1, allowed2));
        }

        var state = new EnumStringMetricState();
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
            value.setValue("ಭ_ಭ");

            state.setMetricValue(value);

            // TODO: Extension
//            state.setExtension();
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    public void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new EnumStringMetricDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setMetricCategory(MetricCategory.SET);
            descriptor.setMetricAvailability(MetricAvailability.INTR);
            descriptor.setUnit(TypeCollection.CODED_VALUE);

            var allowed1 = new EnumStringMetricDescriptor.AllowedValue();
            allowed1.setValue("٩(×̯×)۶");

            var allowed2 = new EnumStringMetricDescriptor.AllowedValue();
            allowed2.setValue("ಭ_ಭ");

            descriptor.setAllowedValue(List.of(allowed1, allowed2));
        }

        var state = new EnumStringMetricState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
        }
        modifications.insert(descriptor, state, Handles.CHANNEL_0);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, EnumStringMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, EnumStringMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, EnumStringMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, EnumStringMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, EnumStringMetricDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, EnumStringMetricState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, EnumStringMetricDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, EnumStringMetricState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
