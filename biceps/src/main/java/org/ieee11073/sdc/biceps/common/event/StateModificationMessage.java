package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.common.event.EventMessage;

import java.util.Collections;
import java.util.List;

/**
 * Base class for any state modification messages.
 *
 * @param <T> the state type that is provided by the {@linkplain StateModificationMessage}.
 */
public class StateModificationMessage<T extends AbstractState> extends AbstractMdibAccessMessage implements EventMessage {
    private final List<T> states;

    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states     the states of the message.
     */
    protected StateModificationMessage(MdibAccess mdibAccess, List<T> states) {
        super(mdibAccess);
        this.states = Collections.unmodifiableList(states);
    }

    public List<T> getStates() {
        return states;
    }

}
