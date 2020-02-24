package it.org.somda.sdc.dpws;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import test.org.somda.common.CIDetector;

import java.time.Duration;

public class IntegrationTestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestUtil.class);
    public static Duration MAX_WAIT_TIME;
    static {
        if (!CIDetector.isRunningInCi()) {
            MAX_WAIT_TIME = Duration.ofSeconds(10);
        } else {
            MAX_WAIT_TIME = Duration.ofSeconds(20);
            LOG.info("CI detected, setting MAX_WAIT_TIME to {}s", MAX_WAIT_TIME.getSeconds());
        }
    }

    private final Injector injector;

    public static void preferIpV4Usage() {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public IntegrationTestUtil() {
        injector = Guice.createInjector(
                new DefaultDpwsModule(),
                new DefaultHelperModule(),
                new DefaultDpwsConfigModule());
    }

    public Injector getInjector() {
        return injector;
    }
}
