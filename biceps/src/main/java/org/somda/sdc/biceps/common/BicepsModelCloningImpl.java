package org.somda.sdc.biceps.common;

import org.somda.sdc.common.util.JaxbModelCloning;

/**
 * Implementation of {@linkplain BicepsModelCloning}.
 */
public class BicepsModelCloningImpl extends JaxbModelCloning implements BicepsModelCloning {

    public BicepsModelCloningImpl() {
        super(CommonConstants.BICEPS_JAXB_CONTEXT_PATH);
    }
}
