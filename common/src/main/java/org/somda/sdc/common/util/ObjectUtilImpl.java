package org.somda.sdc.common.util;

import com.google.inject.Inject;
import com.rits.cloning.Cloner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public <T> T deepCopy(T obj) {
        try {
            return cloner.deepClone(obj);
        } catch (Exception e) {
            LOG.warn("Unable to deepCopy object due to: {}", e.getMessage());
        }
        throw new RuntimeException("Fatal error on object deepCopy");
    }
}