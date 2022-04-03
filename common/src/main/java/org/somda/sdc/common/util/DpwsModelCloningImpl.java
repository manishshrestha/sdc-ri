package org.somda.sdc.common.util;

import org.somda.sdc.common.CommonConfig;

/**
 * Implementation of {@linkplain DpwsModelCloning}.
 */
public class DpwsModelCloningImpl extends JaxbModelCloning implements DpwsModelCloning {

    public DpwsModelCloningImpl() {
        super(CommonConfig.DPWS_JAXB_CONTEXT_PATH);
    }
}
