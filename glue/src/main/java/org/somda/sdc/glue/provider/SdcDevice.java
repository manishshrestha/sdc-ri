package org.somda.sdc.glue.provider;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.*;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.somda.sdc.glue.common.WsdlConstants;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.services.HighPriorityServices;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

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

    @AssistedInject
    SdcDevice(@Assisted DeviceSettings deviceSettings,
              @Assisted LocalMdibAccess mdibAccess,
              @Assisted Collection<OperationInvocationReceiver> operationInvocationReceivers,
              DeviceFactory deviceFactory,
              ServicesFactory servicesFactory,
              HostedServiceFactory hostedServiceFactory) {
        this.dpwsDevice = deviceFactory.createDevice(deviceSettings);
        this.highPriorityServices = servicesFactory.createHighPriorityServices(mdibAccess);
        this.hostedServiceFactory = hostedServiceFactory;
        this.operationInvocationReceivers = operationInvocationReceivers;
    }

    /**
     * Gets the discovery access.
     *
     * @return the discovery access.
     * @see Device#getDiscoveryAccess()
     */
    @Override
    public DiscoveryAccess getDiscoveryAccess() {
        return dpwsDevice.getDiscoveryAccess();
    }

    /**
     * Gets the hosting service access.
     *
     * @return the hosting service access.
     * @see Device#getHostingServiceAccess()
     */
    @Override
    public HostingServiceAccess getHostingServiceAccess() {
        return dpwsDevice.getHostingServiceAccess();
    }

    @Override
    protected void startUp() {
        setupInvocationReceivers();
        setupHostedServices();
        dpwsDevice.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
    }

    private void setupHostedServices() {
        final ClassLoader classLoader = getClass().getClassLoader();
        InputStream highPrioWsdlStream = classLoader.getResourceAsStream("wsdl/IEEE11073-20701-HighPriority-Services.wsdl");
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
        operationInvocationReceivers.forEach(receiver -> addOperationInvocationReceiver(receiver));
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
