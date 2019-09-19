package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.common.event.EventMessage;

import java.util.List;

public class StateModificationMessage<T extends AbstractState> extends AbstractMdibAccessMessage implements EventMessage {
    private final List<T> states;

    protected StateModificationMessage(MdibAccess mdibAccess, List<T> states) {
        super(mdibAccess);
        this.states = states;
    }

    public List<T> getStates() {
        return states;
    }

}
