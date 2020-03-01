package org.somda.sdc.common.util;

import com.google.inject.Inject;
import com.rits.cloning.Cloner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Default implementation of {@linkplain ObjectUtil}.
 */
public class ObjectUtilImpl implements ObjectUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectUtilImpl.class);
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
        try {
            return cloner.deepClone(obj);
        } catch (Exception e) {
            LOG.warn("Unable to deep-copy object due to: {}", e.getMessage());
            LOG.trace("Unable to deep-copy object", e);
        }
        throw new RuntimeException("Fatal error on object deepp-copy");
    }
}