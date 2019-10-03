package org.ieee11073.sdc.biceps.common.access;

import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.ieee11073.sdc.common.helper.ObjectStringifier;
import org.ieee11073.sdc.common.helper.Stringified;

import java.util.List;

/**
 * Read-only result set of a write states call.
 */
public class WriteStateResult {
    @Stringified
    private final MdibVersion mdibVersion;
    private final List<AbstractState> states;

    /**
     * Constructor to initialize all values of the result set.
     *
     * @param mdibVersion the MDIB version.
     * @param states      all updated states.
     */
    public WriteStateResult(MdibVersion mdibVersion,
                            List<AbstractState> states) {
        this.mdibVersion = mdibVersion;
        this.states = states;
    }

    /**
     * Gets the MDIB version that ensued during the preceding write operation.
     *
     * @return the MDIB version.
     */
    public MdibVersion getMdibVersion() {
        return mdibVersion;
    }

    /**
     * Gets all updated states.
     *
     * @return the states.
     */
    public List<AbstractState> getStates() {
        return states;
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }
}
