package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractAlertState;

import java.util.List;

public class AlertStateModificationMessage  extends StateModificationMessage<AbstractAlertState> {
    public AlertStateModificationMessage(MdibAccess mdibAccess, List<AbstractAlertState> states) {
        super(mdibAccess, states);
    }
}
