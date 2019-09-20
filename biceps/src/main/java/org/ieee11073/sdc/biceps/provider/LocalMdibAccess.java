package org.ieee11073.sdc.biceps.provider;

import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.MdibStateModifications;
import org.ieee11073.sdc.biceps.common.PreprocessingException;
import org.ieee11073.sdc.biceps.common.access.*;

public interface LocalMdibAccess extends MdibAccess, ReadTransactionProvider, MdibAccessObservable {
    WriteDescriptionResult writeDescription(MdibDescriptionModifications mdibDescriptionModifications) throws PreprocessingException;

    WriteStateResult writeStates(MdibStateModifications states) throws PreprocessingException;

}
