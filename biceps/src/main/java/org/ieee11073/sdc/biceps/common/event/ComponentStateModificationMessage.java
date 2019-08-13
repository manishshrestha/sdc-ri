package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.MdibAccess;
import org.ieee11073.sdc.biceps.common.MdibVersion;
import org.ieee11073.sdc.biceps.model.participant.AbstractDeviceComponentState;

import java.util.List;

public class ComponentStateModificationMessage extends StateModificationMessage<AbstractDeviceComponentState> {
    public ComponentStateModificationMessage(MdibAccess mdibAccess, List<AbstractDeviceComponentState> states) {
        super(mdibAccess, states);
    }
}
