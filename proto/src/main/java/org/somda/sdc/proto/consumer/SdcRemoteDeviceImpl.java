package org.somda.sdc.proto.consumer;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.access.MdibAccessObservable;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.proto.consumer.report.ReportProcessor;
import org.somda.sdc.proto.consumer.sco.ScoController;
import org.somda.sdc.proto.consumer.sco.ScoTransaction;
import org.somda.sdc.proto.model.discovery.DeviceMetadata;
import org.somda.sdc.proto.model.discovery.Endpoint;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Default implementation of {@linkplain SdcRemoteDevice}.
 */
@SuppressWarnings("UnstableApiUsage")
public class SdcRemoteDeviceImpl extends AbstractIdleService implements SdcRemoteDevice {
    private static final Logger LOG = LogManager.getLogger(SdcRemoteDeviceImpl.class);
    private final RemoteMdibAccess remoteMdibAccess;
    private final ReportProcessor reportProcessor;
    private final ScoController scoController;
    private final org.somda.sdc.proto.consumer.Consumer consumer;
    private final SdcRemoteDeviceWatchdog watchdog;
    private final Duration maxWait;
    private final Logger instanceLogger;

    @AssistedInject
    SdcRemoteDeviceImpl(@Assisted org.somda.sdc.proto.consumer.Consumer consumer,
                        @Assisted RemoteMdibAccess remoteMdibAccess,
                        @Assisted ReportProcessor reportProcessor,
                        @Assisted @Nullable ScoController scoController,
                        @Assisted SdcRemoteDeviceWatchdog watchdog,
                        @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWait,
                        @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier); // HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
        this.remoteMdibAccess = remoteMdibAccess;
        this.reportProcessor = reportProcessor;
        this.scoController = scoController;
        this.consumer = consumer;
        this.watchdog = watchdog;
        this.maxWait = maxWait;
    }

    @Override
    public DeviceMetadata getDeviceMetadata() {
        checkRunning();
        return getDeviceMetadata();
    }

    @Override
    public RemoteMdibAccess getMdibAccess() {
        checkRunning();
        return remoteMdibAccess;
    }

    @Override
    public MdibAccessObservable getMdibAccessObservable() {
        return remoteMdibAccess;
    }

    @Override
    public SetServiceAccess getSetServiceAccess() {
        checkRunning();
        if (scoController == null) {
            final String message = "Remote device does not provide a set service. {} refused.";
            return new SetServiceAccess() {
                @Override
                public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>>
                invoke(T setRequest, Class<V> responseClass) {
                    instanceLogger.warn(message, setRequest.getClass().getSimpleName());
                    return Futures.immediateCancelledFuture();
                }

                @Override
                public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>>
                invoke(T setRequest, @Nullable Consumer<OperationInvokedReport.ReportPart> reportListener,
                       Class<V> responseClass) {
                    instanceLogger.warn(message, setRequest.getClass().getSimpleName());
                    return Futures.immediateCancelledFuture();
                }
            };
        }

        return scoController;
    }

    @Override
    public void registerWatchdogObserver(WatchdogObserver watchdogObserver) {
        checkRunning();
    }

    @Override
    public void unregisterWatchdogObserver(WatchdogObserver watchdogObserver) {
        checkRunning();
    }

    @Override
    protected void startUp() throws TimeoutException, IOException {
        if (this.watchdog != null) {
            watchdog.startAsync().awaitRunning(maxWait.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    protected void shutDown() {
        if (this.watchdog != null) {
            try {
                watchdog.stopAsync().awaitTerminated(maxWait.getSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                instanceLogger.error("Could not stop the remote device watchdog", e);
            }
        }
        try {
            reportProcessor.stopAsync().awaitTerminated(maxWait.getSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            instanceLogger.error("Could not stop the report processor", e);
        }
        consumer.disconnect();
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException(String.format("Tried to access a disconnected SDC remote device instance " +
                    "with EPR address %s", consumer.getEprAddress()));
        }
    }
}
