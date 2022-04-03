package org.somda.sdc.common.util;

import org.somda.sdc.common.CommonConfig;

/**
 * Implementation of {@linkplain BicepsModelCloning}.
 */
public class BicepsModelCloningImpl extends JaxbModelCloning implements BicepsModelCloning {

    public BicepsModelCloningImpl() {
        super(CommonConfig.BICEPS_JAXB_CONTEXT_PATH);
    }
}
