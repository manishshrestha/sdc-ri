package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationState;
import org.somda.sdc.biceps.testutil.Handles;

public class SetAlertStateOperationRoundTrip extends OperationRoundTrip<SetAlertStateOperationDescriptor,
        SetAlertStateOperationState> {
    SetAlertStateOperationRoundTrip(MdibDescriptionModifications modifications) {
        super(modifications,
                Handles.OPERATION_3,
                SetAlertStateOperationDescriptor.class,
                SetAlertStateOperationState.class);
    }
}
