package org.ieee11073.sdc.biceps.common.access;

import org.ieee11073.sdc.biceps.common.MdibVersion;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.util.List;

public class WriteStateResult {
    private final MdibVersion mdibVersion;
    private final List<AbstractState> states;

    public WriteStateResult(MdibVersion mdibVersion,
                       List<AbstractState> states) {
        this.mdibVersion = mdibVersion;
        this.states = states;
    }

    public MdibVersion getMdibVersion() {
        return mdibVersion;
    }

    public List<AbstractState> getStates() {
        return states;
    }
}
