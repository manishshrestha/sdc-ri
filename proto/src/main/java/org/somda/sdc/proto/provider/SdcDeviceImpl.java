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
import org.somda.sdc.proto.common.ProtoConstants;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.provider.factory.ProviderFactory;
import org.somda.sdc.proto.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.proto.provider.sco.OperationInvokedEventSource;
import org.somda.sdc.proto.provider.sco.ScoController;
import org.somda.sdc.proto.provider.sco.SetService;
import org.somda.sdc.proto.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.proto.provider.sco.factory.SetServiceFactory;
import org.somda.sdc.proto.provider.service.HighPriorityServices;
import org.somda.sdc.proto.provider.service.guice.ServiceFactory;

import java.net.InetSocketAddress;
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
    private final SdcDevicePluginProcessor pluginProcessor;
    private final HighPriorityServices highPriorityServices;

    @Inject
    SdcDeviceImpl(@Assisted String eprAddress,
                  @Assisted InetSocketAddress networkAddress,
                  @Assisted LocalMdibAccess mdibAccess,
                  @Assisted("operationInvocationReceivers")
                          Collection<OperationInvocationReceiver> operationInvocationReceivers,
                  @Assisted("plugins") Collection<SdcDevicePlugin> plugins,
                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                  ProviderFactory providerFactory,
                  ScoControllerFactory scoControllerFactory,
                  SetServiceFactory setServiceFactory,
                  ServiceFactory serviceFactory,
                  OperationInvokedEventSource operationInvokedEventSource) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eprAddress = eprAddress;
        this.mdibAccess = mdibAccess;
        this.operationInvocationReceivers = operationInvocationReceivers;
        this.plugins = plugins;
        var providerSettings = ProviderSettings.builder()
                .setProviderName("SdcDevice " + eprAddress)
                .setNetworkAddress(networkAddress).build();
        this.provider = providerFactory.create(eprAddress, providerSettings);
        this.highPriorityServices = serviceFactory.createHighPriorityServices(mdibAccess);
        this.scoController = scoControllerFactory.create(operationInvokedEventSource, mdibAccess);
        this.setService = setServiceFactory.create(scoController, operationInvokedEventSource);

        this.pluginProcessor = new SdcDevicePluginProcessor(this.plugins, this);
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
        pluginProcessor.beforeStartUp();
        provider.addService(ProtoConstants.SET_SERVICE_QNAME, setService);
        highPriorityServices.getServices().forEach(provider::addService);
        provider.startAsync().awaitRunning();
        pluginProcessor.afterStartUp();
    }

    @Override
    protected void shutDown() {
        pluginProcessor.beforeShutDown();
        provider.stopAsync().awaitTerminated();
        pluginProcessor.afterShutDown();
    }
}
