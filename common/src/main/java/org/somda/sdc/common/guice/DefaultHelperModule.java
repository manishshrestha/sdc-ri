package org.somda.sdc.common.guice;

import com.google.inject.AbstractModule;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.common.util.JaxbUtilImpl;
import org.somda.sdc.common.util.ObjectUtil;
import org.somda.sdc.common.util.ObjectUtilImpl;

/**
 * Guice module to bind util functionality.
 */
public class DefaultHelperModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(JaxbUtil.class).to(JaxbUtilImpl.class).asEagerSingleton();
        bind(ObjectUtil.class).to(ObjectUtilImpl.class).asEagerSingleton();
    }
}
