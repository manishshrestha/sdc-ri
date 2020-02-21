package org.somda.sdc.dpws.helper;

import com.google.common.util.concurrent.AbstractIdleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorWrapperService<T extends ExecutorService> extends AbstractIdleService {
    private static Logger LOG = LoggerFactory.getLogger(ExecutorWrapperService.class);
    private static long STOP_TIMEOUT = 5;
    private static TimeUnit STOP_TIMEUNIT = TimeUnit.SECONDS;

    private final Callable<T> serviceCreator;
    private final String serviceName;
    private T executorService;

    public ExecutorWrapperService(Callable<T> serviceCreator) {
        this.serviceCreator = serviceCreator;
        this.serviceName = "unknown service";
    }

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

    public T getExecutorService() {
        if (isRunning()) {
            return executorService;
        } else {
            LOG.error("[{}] getExecutorService was called before the service was running.", serviceName);
            throw new RuntimeException(String.format(
                    "getExecutorService called before startup of %s has finished",
                    serviceName
            ));
        }
    }
}
