package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;

import java.util.List;

public class ContextStateModificationMessage extends StateModificationMessage<AbstractContextState> {
    public ContextStateModificationMessage(MdibAccess mdibAccess, List<AbstractContextState> states) {
        super(mdibAccess, states);
    }
}
