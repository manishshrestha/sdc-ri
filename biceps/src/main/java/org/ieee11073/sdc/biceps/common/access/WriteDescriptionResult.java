package org.ieee11073.sdc.biceps.common.access;

import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;

import java.util.List;

public class WriteDescriptionResult {
    private final MdibVersion mdibVersion;
    private final List<MdibEntity> insertedEntities;
    private final List<MdibEntity> updatedEntities;
    private final List<String> deletedEntities;

    public WriteDescriptionResult(MdibVersion mdibVersion,
                                  List<MdibEntity> insertedEntities,
                                  List<MdibEntity> updatedEntities,
                                  List<String> deletedEntities) {
        this.mdibVersion = mdibVersion;
        this.insertedEntities = insertedEntities;
        this.updatedEntities = updatedEntities;
        this.deletedEntities = deletedEntities;
    }

    public MdibVersion getMdibVersion() {
        return mdibVersion;
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
