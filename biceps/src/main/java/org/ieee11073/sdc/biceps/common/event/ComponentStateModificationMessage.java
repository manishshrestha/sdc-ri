package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractDeviceComponentState;

import java.util.List;

/**
 * Subscribe to this message in order to receive component state changes.
 * <p>
 * <em>Remark: components are clock, battery, MDS, VMD, channels, SCO and system context.</em>
 */
public class ComponentStateModificationMessage extends StateModificationMessage<AbstractDeviceComponentState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public ComponentStateModificationMessage(MdibAccess mdibAccess, List<AbstractDeviceComponentState> states) {
        super(mdibAccess, states);
    }
}
