package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;

import java.util.List;

/**
 * Subscribe to this message in order to receive context state changes.
 */
public class ContextStateModificationMessage extends StateModificationMessage<AbstractContextState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public ContextStateModificationMessage(MdibAccess mdibAccess, List<AbstractContextState> states) {
        super(mdibAccess, states);
    }
}
