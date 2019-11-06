package org.somda.sdc.biceps.testutil;

import com.google.common.eventbus.Subscribe;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.*;

import java.util.ArrayList;
import java.util.List;

public class MdibAccessObserverSpy implements MdibAccessObserver {
    private List<AbstractMdibAccessMessage> recordedMessages;

    public MdibAccessObserverSpy() {
        recordedMessages = new ArrayList<>();
    }

    public List<AbstractMdibAccessMessage> getRecordedMessages() {
        return recordedMessages;
    }

    @Subscribe
    void onUpdate(AbstractMdibAccessMessage updates) {
        recordedMessages.add(updates);
    }
}
