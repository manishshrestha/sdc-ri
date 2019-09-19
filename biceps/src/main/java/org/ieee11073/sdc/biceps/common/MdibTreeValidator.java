package org.ieee11073.sdc.biceps.common;

import com.google.common.collect.HashMultimap;
import org.ieee11073.sdc.biceps.model.participant.*;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Set;

public class MdibTreeValidator {
    private final HashMultimap<Class<?>, Class<?>> allowedParents;

    @Inject
    MdibTreeValidator() {
        this.allowedParents = HashMultimap.create();
        setupAllowedParents();
    }

    public boolean isValidParent(AbstractDescriptor parent, AbstractDescriptor child) {
        return allowedParents.containsEntry(child.getClass(), parent.getClass());
    }

    public Set<Class<?>> allowedParents(AbstractDescriptor child) {
        return allowedParents.get(child.getClass());
    }

    private void setupAllowedParents() {
        allowedParents.put(VmdDescriptor.class, MdsDescriptor.class);
        allowedParents.put(ChannelDescriptor.class, VmdDescriptor.class);
        allowedParents.put(NumericMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(StringMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(EnumStringMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(RealTimeSampleArrayMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(DistributionSampleArrayMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.replaceValues(AlertSystemDescriptor.class, Arrays.asList(MdsDescriptor.class, VmdDescriptor.class));
        allowedParents.put(AlertConditionDescriptor.class, AlertSystemDescriptor.class);
        allowedParents.put(LimitAlertConditionDescriptor.class, AlertSystemDescriptor.class);
        allowedParents.put(AlertSignalDescriptor.class, AlertSystemDescriptor.class);
        allowedParents.replaceValues(ScoDescriptor.class, Arrays.asList(MdsDescriptor.class, VmdDescriptor.class));
        allowedParents.put(ActivateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetStringOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetValueOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetComponentStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetMetricStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetContextStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetAlertStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(ClockDescriptor.class, MdsDescriptor.class);
        allowedParents.put(BatteryDescriptor.class, MdsDescriptor.class);
        allowedParents.put(SystemContextDescriptor.class, MdsDescriptor.class);
        allowedParents.put(PatientContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(LocationContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(EnsembleContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(WorkflowContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(MeansContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(OperatorContextDescriptor.class, SystemContextDescriptor.class);
    }
}
