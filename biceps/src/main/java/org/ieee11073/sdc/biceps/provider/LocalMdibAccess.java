package org.ieee11073.sdc.biceps.provider;

import org.ieee11073.sdc.biceps.common.MdibAccess;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.MdibStateModifications;
import org.ieee11073.sdc.biceps.common.PreprocessingException;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;

import java.util.Optional;

public interface LocalMdibAccess extends MdibAccess {
    void writeDescription(MdibDescriptionModifications mdibDescriptionModifications) throws PreprocessingException;

    void writeStates(MdibStateModifications states) throws PreprocessingException;
}
