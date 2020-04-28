package org.somda.sdc.common.util;

import com.google.inject.Inject;
import com.rits.cloning.Cloner;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;

/**
 * Default implementation of {@linkplain ObjectUtil}.
 */
public class ObjectUtilImpl implements ObjectUtil {
    private static final Logger LOG = LogManager.getLogger(ObjectUtilImpl.class);
    private final Cloner cloner;

    @Inject
    ObjectUtilImpl(Cloner cloner) {
        this.cloner = cloner;
    }

    @Override
    public <T> T deepCopy(@Nullable T obj) {
        if (obj == null) {
            return null;
        }

        return cloner.deepClone(obj);
    }
}