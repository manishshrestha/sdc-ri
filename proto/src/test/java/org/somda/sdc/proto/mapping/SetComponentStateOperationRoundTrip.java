package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationState;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationState;
import org.somda.sdc.biceps.testutil.Handles;

public class SetComponentStateOperationRoundTrip extends OperationRoundTrip<SetComponentStateOperationDescriptor,
        SetComponentStateOperationState> {
    SetComponentStateOperationRoundTrip(MdibDescriptionModifications modifications) {
        super(modifications,
                Handles.OPERATION_1,
                SetComponentStateOperationDescriptor.class,
                SetComponentStateOperationState.class);
    }
}
