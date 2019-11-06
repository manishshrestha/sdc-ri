package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractOperationState;

import java.util.List;

/**
 * Subscribe to this message in order to receive operation state changes.
 */
public class OperationStateModificationMessage extends StateModificationMessage<AbstractOperationState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public OperationStateModificationMessage(MdibAccess mdibAccess, List<AbstractOperationState> states) {
        super(mdibAccess, states);
    }
}
