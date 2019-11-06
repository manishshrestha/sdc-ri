package it.org.ieee11073.sdc.dpws;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;

import java.time.Duration;

public class IntegrationTestUtil {
    public static final Duration MAX_WAIT_TIME = Duration.ofSeconds(10);

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
