package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;

import java.util.List;

/**
 * Subscribe to this message in order to receive metric state changes.
 */
public class WaveformStateModificationMessage extends StateModificationMessage<RealTimeSampleArrayMetricState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public WaveformStateModificationMessage(MdibAccess mdibAccess, List<RealTimeSampleArrayMetricState> states) {
        super(mdibAccess, states);
    }
}
