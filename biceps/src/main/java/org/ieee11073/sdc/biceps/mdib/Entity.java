package org.ieee11073.sdc.biceps.mdib;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface Entity<T> {
    String getHandle();
    T getModel();
    Optional<Entity<?>> getParent();
    List<Entity<?>>
}
