package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationState;
import org.somda.sdc.biceps.testutil.Handles;

public class SetMetricStateOperationRoundTrip extends OperationRoundTrip<SetMetricStateOperationDescriptor,
        SetMetricStateOperationState> {
    SetMetricStateOperationRoundTrip(MdibDescriptionModifications modifications) {
        super(modifications,
                Handles.OPERATION_0,
                SetMetricStateOperationDescriptor.class,
                SetMetricStateOperationState.class);
    }
}
