package org.somda.sdc.glue.consumer.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.dpws.service.HostingServiceProxy;

/**
 * Logger helper utility to prepend remote connection information to log messages.
 * <p>
 * The prefixes are supposed to help identifying problems during log output analysis.
 */
public class LogPrepender {
    /**
     * Gets a logger.
     * <p>
     * Use this function as a replacement for {@link LogManager#getLogger(Class)}.
     *
     * @param hostingService the hosting service information that is prepended to log messages.
     * @param theClass the class used by the logger.
     * @return a child logger adapter that contains hosting service information in the logger name
     */
    public static Logger getLogger(HostingServiceProxy hostingService, Class<?> theClass) {
        return LogManager.getLogger(
                theClass.getCanonicalName() + "." + generatePrefix(hostingService)
        );
    }

    private static String generatePrefix(HostingServiceProxy hostingServiceProxy) {
        return '['
                + hostingServiceProxy.getEndpointReferenceAddress()
                + ','
                + hostingServiceProxy.getActiveXAddr()
                + "]";
    }
}
