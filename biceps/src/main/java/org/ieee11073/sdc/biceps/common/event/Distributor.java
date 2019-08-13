package org.ieee11073.sdc.biceps.common.event;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.MdibAccess;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.MdibStateModifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

public class Distributor {
    private static final Logger LOG = LoggerFactory.getLogger(Distributor.class);

    private final EventBus eventBus;

    @Inject
    Distributor(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void registerObserver(Object observer) {
        eventBus.register(observer);
    }

    public void unregisterObserver(Object observer) {
        eventBus.unregister(observer);
    }

    public void sendDescriptionModificationEvent(MdibAccess mdibAccess,
                                                 List<MdibEntity> insertedEntities,
                                                 List<MdibEntity> updatedEntities,
                                                 List<String> deletedEntities) {
        eventBus.post(new DescriptionModificationMessage(mdibAccess, insertedEntities, updatedEntities, deletedEntities));
    }

    public void sendStateModificationEvent(MdibAccess mdibAccess, MdibStateModifications.Type changeType, List<?> states) {
        Constructor<?> ctor = null;
        for (Constructor<?> constructor : changeType.getEventMessageClass().getConstructors()) {
            if (constructor.getParameterCount() == 2) {
                ctor = constructor;
                break;
            }
        }

        if (ctor == null) {
            LOG.error("Expected constructor to create state modification message not found. Distribution failed.");
            return;
        }

        try {
            eventBus.post(ctor.newInstance(mdibAccess, Collections.unmodifiableList(states)));
        } catch (Exception e) {
            LOG.error("Failed to call state event message constructor", e);
        }
    }
}
