package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractMetricState;

import java.util.List;

/**
 * Subscribe to this message in order to receive metric state changes.
 */
public class MetricStateModificationMessage extends StateModificationMessage<AbstractMetricState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public MetricStateModificationMessage(MdibAccess mdibAccess, List<AbstractMetricState> states) {
        super(mdibAccess, states);
    }
}
