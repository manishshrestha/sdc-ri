package org.somda.sdc.glue.guice;

import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.glue.GlueConstants;

public class GlueDpwsConfigModule extends DefaultDpwsConfigModule {
    @Override
    protected void customConfigure() {
        bind(SoapConfig.JAXB_CONTEXT_PATH,
                String.class,
                GlueConstants.JAXB_CONTEXT_PATH);
    }
}
