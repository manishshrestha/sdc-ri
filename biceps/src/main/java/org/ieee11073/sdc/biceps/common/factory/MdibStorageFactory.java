package org.ieee11073.sdc.biceps.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.MdibStorage;
import org.ieee11073.sdc.biceps.common.MdibVersion;

public interface MdibStorageFactory {
    MdibStorage createMdibStorage(@Assisted MdibVersion mdibVersion);
    MdibStorage createMdibStorage();
}
