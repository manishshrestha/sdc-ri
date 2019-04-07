package org.ieee11073.sdc.common.guice;

import com.google.inject.AbstractModule;
import org.ieee11073.sdc.common.helper.JaxbUtil;
import org.ieee11073.sdc.common.helper.JaxbUtilImpl;
import org.ieee11073.sdc.common.helper.ObjectUtil;
import org.ieee11073.sdc.common.helper.ObjectUtilImpl;

/**
 * Guice module to bind helper functionality.
 */
public class DefaultHelperModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(JaxbUtil.class).to(JaxbUtilImpl.class).asEagerSingleton();
        bind(ObjectUtil.class).to(ObjectUtilImpl.class).asEagerSingleton();
    }
}
