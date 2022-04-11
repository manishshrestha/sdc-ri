package org.somda.sdc.dpws;

import org.somda.sdc.common.util.JaxbModelCloning;

/**
 * Implementation of {@linkplain DpwsModelCloning}.
 */
public class DpwsModelCloningImpl extends JaxbModelCloning implements DpwsModelCloning {

    public DpwsModelCloningImpl() {
        super(DpwsConstants.DPWS_JAXB_CONTEXT_PATH);
    }
}
