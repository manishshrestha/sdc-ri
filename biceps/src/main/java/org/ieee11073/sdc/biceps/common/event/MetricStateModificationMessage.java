package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractMetricState;

import java.util.List;

public class MetricStateModificationMessage extends StateModificationMessage<AbstractMetricState> {
    public MetricStateModificationMessage(MdibAccess mdibAccess, List<AbstractMetricState> states) {
        super(mdibAccess, states);
    }
}
