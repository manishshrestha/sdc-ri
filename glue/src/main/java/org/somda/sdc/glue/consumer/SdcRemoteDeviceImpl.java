package org.somda.sdc.glue.consumer;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.report.ReportProcessor;
import org.somda.sdc.glue.consumer.sco.InvocationException;
import org.somda.sdc.glue.consumer.sco.ScoController;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.function.Consumer;

public class SdcRemoteDeviceImpl extends AbstractIdleService implements SdcRemoteDevice {
    private final Logger LOG;
    private final RemoteMdibAccess remoteMdibAccess;
    private final ReportProcessor reportProcessor;
    private final ScoController scoController;
    private final HostingServiceProxy hostingServiceProxy;

    @AssistedInject
    SdcRemoteDeviceImpl(@Assisted HostingServiceProxy hostingServiceProxy,
                        @Assisted RemoteMdibAccess remoteMdibAccess,
                        @Assisted ReportProcessor reportProcessor,
                        @Assisted @Nullable ScoController scoController) {
        LOG = LogPrepender.getLogger(hostingServiceProxy, SdcRemoteDeviceImpl.class);
        this.remoteMdibAccess = remoteMdibAccess;
        this.reportProcessor = reportProcessor;
        this.scoController = scoController;
        this.hostingServiceProxy = hostingServiceProxy;
    }

    @Override
    public HostingServiceProxy getHostingServiceProxy() {
        checkRunning();
        return hostingServiceProxy;
    }

    @Override
    public RemoteMdibAccess getMdibAccess() {
        checkRunning();
        return remoteMdibAccess;
    }

    @Override
    public SetServiceAccess getSetServiceAccess() {
        checkRunning();
        if (scoController == null) {
            final String message = "Remote device does not provide a set service. {} refused.";
            return new SetServiceAccess() {
                @Override
                public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(T setRequest, Class<V> responseClass) {
                    LOG.warn(message, setRequest.getClass().getSimpleName());
                    return Futures.immediateCancelledFuture();
                }

                @Override
                public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(T setRequest, @Nullable Consumer<OperationInvokedReport.ReportPart> reportListener, Class<V> responseClass) {
                    LOG.warn(message, setRequest.getClass().getSimpleName());
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
    protected void startUp() {

    }

    @Override
    protected void shutDown() {
        reportProcessor.stopAsync().awaitTerminated();

    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException(String.format("Tried to access a disconnected SDC remote device instance with EPR address %s",
                    hostingServiceProxy.getEndpointReferenceAddress().toString()));
        }
    }
}
