package org.somda.sdc.common.util;

import com.google.common.util.concurrent.AbstractIdleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wraps an {@linkplain ExecutorService} into a guava {@linkplain AbstractIdleService}.
 * <p>
 * Wrapping {@linkplain ExecutorService}s into guava services allows orchestrating thread pool instances, especially
 * shutting them down properly when shutting down a parent service instance.
 *
 * @param <T> actual type of the {@linkplain ExecutorService}.
 */
public class ExecutorWrapperService<T extends ExecutorService> extends AbstractIdleService {
    private static Logger LOG = LoggerFactory.getLogger(ExecutorWrapperService.class);
    private static long STOP_TIMEOUT = 5;
    private static TimeUnit STOP_TIMEUNIT = TimeUnit.SECONDS;

    private final Callable<T> serviceCreator;
    private final String serviceName;
    private T executorService;


    /**
     * Creates a wrapper around an {@linkplain ExecutorService}.
     *
     * @param serviceCreator {@linkplain Callable} which returns an {@linkplain ExecutorService}.
     */
    public ExecutorWrapperService(Callable<T> serviceCreator) {
        this(serviceCreator, "UNKNOWN");
    }

    /**
     * Creates a wrapper around an {@linkplain ExecutorService}.
     *
     * @param serviceCreator {@linkplain Callable} which returns an {@linkplain ExecutorService}.
     * @param serviceName name for the service, used in logging.
     */
    public ExecutorWrapperService(Callable<T> serviceCreator, String serviceName) {
        this.serviceCreator = serviceCreator;
        this.serviceName = serviceName;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.debug("[{}] Starting executor service wrapper", serviceName);
        this.executorService = serviceCreator.call();
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("[{}] Stopping executor service wrapper", serviceName);
        if (this.executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(STOP_TIMEOUT, STOP_TIMEUNIT);
            } catch (InterruptedException e) {
                LOG.error("[{}] Could not stop all threads!", serviceName);
                throw e;
            }
        }
    }

    /**
     * Gets the {@linkplain ExecutorService} instance when service is running.
     * <p>
     * <em>Only ever access this once the service has been started!</em>
     *
     * @return wrapped {@linkplain ExecutorService} instance.
     * @throws RuntimeException when the service isn't running yet.
     */
    public T get() {
        if (isRunning()) {
            return executorService;
        } else {
            LOG.error("[{}] get was called before the service was running", serviceName);
            throw new RuntimeException(String.format(
                    "get called before startup of %s has finished",
                    serviceName
            ));
        }
    }

    @Override
    public String toString() {
        return "ExecutorWrapperService{" +
                "serviceName='" + serviceName + '\'' +
                ", executorService=" + executorService +
                '}';
    }
}
