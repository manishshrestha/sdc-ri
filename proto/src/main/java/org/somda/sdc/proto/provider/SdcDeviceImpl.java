package org.somda.sdc.proto.provider;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.proto.common.ProtoConstants;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.discovery.provider.factory.TargetServiceFactory;
import org.somda.sdc.proto.provider.factory.ProviderFactory;
import org.somda.sdc.proto.provider.sco.OperationInvokedEventSource;
import org.somda.sdc.proto.provider.sco.ScoController;
import org.somda.sdc.proto.provider.sco.SetService;
import org.somda.sdc.proto.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.proto.provider.sco.factory.SetServiceFactory;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class SdcDeviceImpl extends AbstractIdleService implements SdcDevice {
    private static final Logger LOG = LogManager.getLogger(SdcDeviceImpl.class);

    private final Logger instanceLogger;
    private final LocalMdibAccess mdibAccess;
    private final Collection<OperationInvocationReceiver> operationInvocationReceivers;
    private final Collection<SdcDevicePlugin> plugins;
    private final Provider provider;
    private final String eprAddress;
    private final SetService setService;
    private final ScoController scoController;

    @Inject
    SdcDeviceImpl(@Assisted String eprAddress,
                  @Assisted NetworkInterface networkInterface,
                  @Assisted LocalMdibAccess mdibAccess,
                  @Assisted("operationInvocationReceivers")
                          Collection<OperationInvocationReceiver> operationInvocationReceivers,
                  @Assisted("plugins") Collection<SdcDevicePlugin> plugins,
                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                  ProviderFactory providerFactory,
                  ScoControllerFactory scoControllerFactory,
                  SetServiceFactory setServiceFactory,
                  OperationInvokedEventSource operationInvokedEventSource) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eprAddress = eprAddress;
        this.mdibAccess = mdibAccess;
        this.operationInvocationReceivers = operationInvocationReceivers;
        this.plugins = plugins;
        var nifAddresses = networkInterface.getInetAddresses();
        var providerSettings = ProviderSettings.builder()
                .setProviderName("SdcDevice " + eprAddress)
                .setNetworkAddress(new InetSocketAddress(nifAddresses.nextElement(), 0)).build();
        this.provider = providerFactory.create(eprAddress, providerSettings);
        this.scoController = scoControllerFactory.create(operationInvokedEventSource, mdibAccess);
        this.setService = setServiceFactory.create(scoController, operationInvokedEventSource);
    }

    @Override
    public String getEprAddress() {
        return eprAddress;
    }

    @Override
    public LocalMdibAccess getLocalMdibAccess() {
        return mdibAccess;
    }

    @Override
    public Collection<OperationInvocationReceiver> getOperationInvocationReceivers() {
        return operationInvocationReceivers;
    }

    @Override
    public Service.State getServiceState() {
        return state();
    }

    @Override
    public <T extends AbstractState> void sendPeriodicStateReport(final List<T> states, final MdibVersion mdibVersion) {
        // TODO
    }

    @Override
    public TargetService getTargetService() {
        return provider;
    }

    @Override
    protected void startUp() throws Exception {
        // todo provider.addService(ProtoConstants.GET_SERVICE_QNAME, ...);
        // todo provider provider.addService(ProtoConstants.MDIB_REPORTING_SERVICE_QNAME, ...);
        provider.addService(ProtoConstants.SET_SERVICE_QNAME, setService);
        provider.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        provider.stopAsync().awaitTerminated();
    }
}
