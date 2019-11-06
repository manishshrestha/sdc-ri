package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractAlertState;

import java.util.List;

/**
 * Subscribe to this message in order to receive alert state changes.
 */
public class AlertStateModificationMessage  extends StateModificationMessage<AbstractAlertState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public AlertStateModificationMessage(MdibAccess mdibAccess, List<AbstractAlertState> states) {
        super(mdibAccess, states);
    }
}
