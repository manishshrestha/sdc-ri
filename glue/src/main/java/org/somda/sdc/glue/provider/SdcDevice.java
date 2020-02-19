package org.somda.sdc.glue.provider;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.device.*;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.WsdlConstants;
import org.somda.sdc.glue.provider.helper.SdcDevicePluginProcessor;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.services.HighPriorityServices;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;
import org.somda.sdc.mdpws.common.CommonConstants;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Adds SDC services to a DPWS device and manages incoming set service requests.
 * <p>
 * The purpose of the {@linkplain SdcDevice} class is to provide SDC data on the network.
 */
public class SdcDevice extends AbstractIdleService implements Device, EventSourceAccess {
    private final Device dpwsDevice;
    private final HostedServiceFactory hostedServiceFactory;
    private final HighPriorityServices highPriorityServices;
    private final Collection<OperationInvocationReceiver> operationInvocationReceivers;
    private final LocalMdibAccess mdibAccess;
    private final Collection<SdcDevicePlugin> plugins;
    private final SdcDevicePluginProcessor pluginProcessor;

    @AssistedInject
    SdcDevice(@Assisted DeviceSettings deviceSettings,
              @Assisted LocalMdibAccess mdibAccess,
              @Assisted("operationInvocationReceivers") Collection<OperationInvocationReceiver> operationInvocationReceivers,
              @Assisted("plugins") Collection<SdcDevicePlugin> plugins,
              DeviceFactory deviceFactory,
              ServicesFactory servicesFactory,
              HostedServiceFactory hostedServiceFactory) {
        this.mdibAccess = mdibAccess;
        this.plugins = plugins;
        this.dpwsDevice = deviceFactory.createDevice(deviceSettings);
        this.highPriorityServices = servicesFactory.createHighPriorityServices(mdibAccess);
        this.hostedServiceFactory = hostedServiceFactory;
        this.operationInvocationReceivers = operationInvocationReceivers;

        this.pluginProcessor = new SdcDevicePluginProcessor(plugins, new SdcDeviceContext() {
            @Override
            public Device getDevice() {
                return dpwsDevice;
            }

            @Override
            public LocalMdibAccess getLocalMdibAccess() {
                return mdibAccess;
            }

            @Override
            public Collection<OperationInvocationReceiver> getOperationInvocationReceivers() {
                return operationInvocationReceivers;
            }
        });
    }

    public LocalMdibAccess getMdibAccess() {
        return mdibAccess;
    }

    @Override
    public URI getEprAddress() {
        return dpwsDevice.getEprAddress();
    }

    /**
     * Gets the discovery access.
     *
     * <em>Please note that the discovery access is managed by this class.
     * Overwriting types and/or scopes can cause negative side-effects.</em>
     *
     * @return the discovery access.
     * @see Device#getDiscoveryAccess()
     * @deprecated Use plugins to get access to discovery information
     */
    @Override
    @Deprecated
    public DiscoveryAccess getDiscoveryAccess() {
        return new DiscoveryAccess() {
            @Override
            public void setTypes(Collection<QName> types) {
                ArrayList<QName> tmpTypes = new ArrayList<>();
                if (types.stream().filter(qName -> qName.equals(CommonConstants.MEDICAL_DEVICE_TYPE)).findAny().isEmpty()) {
                    tmpTypes.add(DpwsConstants.DEVICE_TYPE);
                }
                tmpTypes.addAll(types);
                dpwsDevice.getDiscoveryAccess().setTypes(tmpTypes);
            }

            @Override
            public void setScopes(Collection<URI> scopes) {
                // todo DGr track scopes from MDIB and update accordingly
                ArrayList<URI> tmpScopes = new ArrayList<>();
                if (scopes.stream().filter(scope -> scope.equals(GlueConstants.SCOPE_SDC_PROVIDER)).findAny().isEmpty()) {
                    tmpScopes.add(GlueConstants.SCOPE_SDC_PROVIDER);
                }
                tmpScopes.addAll(scopes);
                dpwsDevice.getDiscoveryAccess().setScopes(tmpScopes);
            }

            @Override
            public void sendHello() {
                dpwsDevice.getDiscoveryAccess().sendHello();
            }
        };
    }

    /**
     * Gets the hosting service access.
     *
     * @return the hosting service access.
     * @see Device#getHostingServiceAccess()
     * @deprecated Use plugins to get access to hosting service information
     */
    @Override
    @Deprecated
    public HostingServiceAccess getHostingServiceAccess() {
        return dpwsDevice.getHostingServiceAccess();
    }

    @Override
    protected void startUp() throws Exception {
        setupInvocationReceivers();
        setupHostedServices();

        pluginProcessor.beforeStartUp();

        dpwsDevice.startAsync().awaitRunning();

        pluginProcessor.afterStartUp();
    }

    @Override
    protected void shutDown() {
        pluginProcessor.beforeShutDown();
        dpwsDevice.stopAsync().awaitTerminated();
        pluginProcessor.afterShutDown();
    }

    private void setupHostedServices() {
        final ClassLoader classLoader = getClass().getClassLoader();
        InputStream highPrioWsdlStream = classLoader.getResourceAsStream("wsdl/IEEE11073-20701-HighPriority-Services.wsdl");
        assert highPrioWsdlStream != null;
        dpwsDevice.getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                "HighPriorityServices",
                Arrays.asList(
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_GET),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_SET),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_CONTAINMENT_TREE),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_CONTEXT),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_DESCRIPTION_EVENT),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_STATE_EVENT),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_WAVEFORM)),
                highPriorityServices,
                highPrioWsdlStream));
    }

    private void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        highPriorityServices.addOperationInvocationReceiver(receiver);
    }

    private void setupInvocationReceivers() {
        operationInvocationReceivers.forEach(this::addOperationInvocationReceiver);
    }

    @Override
    public void sendNotification(String action, Object payload) throws MarshallingException, TransportException {
        highPriorityServices.sendNotification(action, payload);
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) throws TransportException {
        highPriorityServices.subscriptionEndToAll(status);
    }
}
