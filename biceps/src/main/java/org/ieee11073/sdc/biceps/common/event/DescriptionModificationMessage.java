package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.common.MdibEntity;

import java.util.List;

public class DescriptionModificationMessage extends AbstractMdibAccessMessage {
    private final List<MdibEntity> insertedEntities;
    private final List<MdibEntity> updatedEntities;
    private final List<String> deletedEntities;

    public DescriptionModificationMessage(MdibAccess mdibAccess,
                                          List<MdibEntity> insertedEntities,
                                          List<MdibEntity> updatedEntities,
                                          List<String> deletedEntities) {
        super(mdibAccess);
        this.insertedEntities = insertedEntities;
        this.updatedEntities = updatedEntities;
        this.deletedEntities = deletedEntities;
    }

    public List<MdibEntity> getInsertedEntities() {
        return insertedEntities;
    }

    public List<MdibEntity> getUpdatedEntities() {
        return updatedEntities;
    }

    public List<String> getDeletedEntities() {
        return deletedEntities;
    }
}
