package org.ieee11073.sdc.biceps.common.access.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.MdibStorage;
import org.ieee11073.sdc.biceps.common.access.ReadTransaction;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Lock;

public interface ReadTransactionFactory {
    ReadTransaction createReadTransaction(@Assisted MdibStorage mdibStorage,
                                          @Assisted @Nullable Lock lock);
}
