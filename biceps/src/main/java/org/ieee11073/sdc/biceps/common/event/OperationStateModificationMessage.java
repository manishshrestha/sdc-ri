package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractOperationState;

import java.util.List;

public class OperationStateModificationMessage extends StateModificationMessage<AbstractOperationState> {
    public OperationStateModificationMessage(MdibAccess mdibAccess, List<AbstractOperationState> states) {
        super(mdibAccess, states);
    }
}
