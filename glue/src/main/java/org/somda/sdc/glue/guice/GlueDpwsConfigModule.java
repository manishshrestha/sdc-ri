package org.somda.sdc.glue.guice;

import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.CommonConstants;

public class GlueDpwsConfigModule extends DefaultDpwsConfigModule {
    @Override
    protected void customConfigure() {
        bind(SoapConfig.JAXB_CONTEXT_PATH,
                String.class,
                GlueConstants.JAXB_CONTEXT_PATH);
        bind(SoapConfig.JAXB_SCHEMA_PATH,
                String.class,
                GlueConstants.SCHEMA_PATH);
        bind(SoapConfig.NAMESPACE_MAPPINGS,
                String.class,
                org.somda.sdc.glue.common.CommonConstants.NAMESPACE_PREFIX_MAPPINGS_MDPWS +
                        org.somda.sdc.glue.common.CommonConstants.NAMESPACE_PREFIX_MAPPINGS_BICEPS +
                        CommonConstants.NAMESPACE_PREFIX_MAPPINGS_GLUE);
    }
}
