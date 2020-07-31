package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationState;
import org.somda.sdc.biceps.testutil.Handles;

public class SetContextStateOperationRoundTrip extends OperationRoundTrip<SetContextStateOperationDescriptor,
        SetContextStateOperationState> {
    SetContextStateOperationRoundTrip(MdibDescriptionModifications modifications) {
        super(modifications,
                Handles.OPERATION_2,
                SetContextStateOperationDescriptor.class,
                SetContextStateOperationState.class);
    }
}
