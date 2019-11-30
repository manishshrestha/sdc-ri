package org.somda.sdc.glue.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.somda.sdc.dpws.service.HostingServiceProxy;

public class LogPrepender {
    public static Logger getLogger(HostingServiceProxy hostingService, Class<?> theClass) {
        final Logger logger = LoggerFactory.getLogger(theClass);
        return new Logger() {
            @Override
            public String getName() {
                return logger.getName();
            }

            @Override
            public boolean isTraceEnabled() {
                return logger.isTraceEnabled();
            }

            @Override
            public void trace(String s) {
                logger.trace(prepend(hostingService, s));
            }

            @Override
            public void trace(String s, Object o) {
                logger.trace(prepend(hostingService, s), o);
            }

            @Override
            public void trace(String s, Object o, Object o1) {
                logger.trace(prepend(hostingService, s), o, o1);
            }

            @Override
            public void trace(String s, Object... objects) {
                logger.trace(prepend(hostingService, s), objects);
            }

            @Override
            public void trace(String s, Throwable throwable) {
                logger.trace(prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isTraceEnabled(Marker marker) {
                return logger.isTraceEnabled(marker);
            }

            @Override
            public void trace(Marker marker, String s) {
                logger.trace(marker, prepend(hostingService, s));
            }

            @Override
            public void trace(Marker marker, String s, Object o) {
                logger.trace(marker, prepend(hostingService, s), o);
            }

            @Override
            public void trace(Marker marker, String s, Object o, Object o1) {
                logger.trace(marker, prepend(hostingService, s), o, o1);
            }

            @Override
            public void trace(Marker marker, String s, Object... objects) {
                logger.trace(marker, prepend(hostingService, s), objects);
            }

            @Override
            public void trace(Marker marker, String s, Throwable throwable) {
                logger.trace(marker, prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isDebugEnabled() {
                return logger.isDebugEnabled();
            }

            @Override
            public void debug(String s) {
                logger.debug(prepend(hostingService, s));
            }

            @Override
            public void debug(String s, Object o) {
                logger.debug(prepend(hostingService, s), o);
            }

            @Override
            public void debug(String s, Object o, Object o1) {
                logger.debug(prepend(hostingService, s), o, o1);
            }

            @Override
            public void debug(String s, Object... objects) {
                logger.debug(prepend(hostingService, s), objects);
            }

            @Override
            public void debug(String s, Throwable throwable) {
                logger.debug(prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isDebugEnabled(Marker marker) {
                return logger.isDebugEnabled(marker);
            }

            @Override
            public void debug(Marker marker, String s) {
                logger.debug(marker, prepend(hostingService, s));
            }

            @Override
            public void debug(Marker marker, String s, Object o) {
                logger.debug(marker, prepend(hostingService, s), o);
            }

            @Override
            public void debug(Marker marker, String s, Object o, Object o1) {
                logger.debug(marker, prepend(hostingService, s), o, o1);
            }

            @Override
            public void debug(Marker marker, String s, Object... objects) {
                logger.debug(marker, prepend(hostingService, s), objects);
            }

            @Override
            public void debug(Marker marker, String s, Throwable throwable) {
                logger.debug(marker, prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isInfoEnabled() {
                return logger.isInfoEnabled();
            }

            @Override
            public void info(String s) {
                logger.info(prepend(hostingService, s));
            }

            @Override
            public void info(String s, Object o) {
                logger.info(prepend(hostingService, s), o);
            }

            @Override
            public void info(String s, Object o, Object o1) {
                logger.info(prepend(hostingService, s), o, o1);
            }

            @Override
            public void info(String s, Object... objects) {
                logger.info(prepend(hostingService, s), objects);
            }

            @Override
            public void info(String s, Throwable throwable) {
                logger.info(prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isInfoEnabled(Marker marker) {
                return logger.isInfoEnabled(marker);
            }

            @Override
            public void info(Marker marker, String s) {
                logger.info(marker, prepend(hostingService, s));
            }

            @Override
            public void info(Marker marker, String s, Object o) {
                logger.info(marker, prepend(hostingService, s), o);
            }

            @Override
            public void info(Marker marker, String s, Object o, Object o1) {
                logger.info(marker, prepend(hostingService, s), o, o1);
            }

            @Override
            public void info(Marker marker, String s, Object... objects) {
                logger.info(marker, prepend(hostingService, s), objects);
            }

            @Override
            public void info(Marker marker, String s, Throwable throwable) {
                logger.info(marker, prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isWarnEnabled() {
                return logger.isWarnEnabled();
            }

            @Override
            public void warn(String s) {
                logger.warn(prepend(hostingService, s));
            }

            @Override
            public void warn(String s, Object o) {
                logger.warn(prepend(hostingService, s), o);
            }

            @Override
            public void warn(String s, Object o, Object o1) {
                logger.warn(prepend(hostingService, s), o, o1);
            }

            @Override
            public void warn(String s, Object... objects) {
                logger.warn(prepend(hostingService, s), objects);
            }

            @Override
            public void warn(String s, Throwable throwable) {
                logger.warn(prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isWarnEnabled(Marker marker) {
                return logger.isWarnEnabled(marker);
            }

            @Override
            public void warn(Marker marker, String s) {
                logger.warn(marker, prepend(hostingService, s));
            }

            @Override
            public void warn(Marker marker, String s, Object o) {
                logger.warn(marker, prepend(hostingService, s), o);
            }

            @Override
            public void warn(Marker marker, String s, Object o, Object o1) {
                logger.warn(marker, prepend(hostingService, s), o, o1);
            }

            @Override
            public void warn(Marker marker, String s, Object... objects) {
                logger.warn(marker, prepend(hostingService, s), objects);
            }

            @Override
            public void warn(Marker marker, String s, Throwable throwable) {
                logger.warn(marker, prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isErrorEnabled() {
                return logger.isErrorEnabled();
            }

            @Override
            public void error(String s) {
                logger.error(prepend(hostingService, s));
            }

            @Override
            public void error(String s, Object o) {
                logger.error(prepend(hostingService, s), o);
            }

            @Override
            public void error(String s, Object o, Object o1) {
                logger.error(prepend(hostingService, s), o, o1);
            }

            @Override
            public void error(String s, Object... objects) {
                logger.error(prepend(hostingService, s), objects);
            }

            @Override
            public void error(String s, Throwable throwable) {
                logger.error(prepend(hostingService, s), throwable);
            }

            @Override
            public boolean isErrorEnabled(Marker marker) {
                return logger.isErrorEnabled(marker);
            }

            @Override
            public void error(Marker marker, String s) {
                logger.error(marker, prepend(hostingService, s));
            }

            @Override
            public void error(Marker marker, String s, Object o) {
                logger.error(marker, prepend(hostingService, s), o);
            }

            @Override
            public void error(Marker marker, String s, Object o, Object o1) {
                logger.error(marker, prepend(hostingService, s), o, o1);
            }

            @Override
            public void error(Marker marker, String s, Object... objects) {
                logger.error(marker, prepend(hostingService, s), objects);
            }

            @Override
            public void error(Marker marker, String s, Throwable throwable) {
                logger.error(marker, prepend(hostingService, s), throwable);
            }
        };
    }

    private static String prepend(HostingServiceProxy hostingServiceProxy, String logMessage) {
        return new StringBuffer()
                .append('[')
                .append(hostingServiceProxy.getEndpointReferenceAddress().toString())
                .append(',')
                .append(hostingServiceProxy.getActiveXAddr().toString())
                .append("] ")
                .append(logMessage)
                .toString();
    }
}
