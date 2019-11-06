package org.ieee11073.sdc.glue.provider;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccess;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.glue.common.WsdlConstants;
import org.ieee11073.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.ieee11073.sdc.glue.provider.services.HighPriorityServices;
import org.ieee11073.sdc.glue.provider.services.factory.ServicesFactory;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

/**
 * Adds SDC services to a DPWS device and manages incoming set service requests.
 * <p>
 * The purpose of the {@linkplain SdcServices} class is to provide SDC data on the network.
 */
public class SdcServices {
    private final Device dpwsDevice;
    private final HostedServiceFactory hostedServiceFactory;
    private final HighPriorityServices highPriorityServices;

    @AssistedInject
    SdcServices(@Assisted Device dpwsDevice,
                @Assisted LocalMdibAccess mdibAccess,
                @Assisted Collection<OperationInvocationReceiver> operationInvocationReceivers,
                ServicesFactory servicesFactory,
                HostedServiceFactory hostedServiceFactory) {
        this.dpwsDevice = dpwsDevice;
        this.hostedServiceFactory = hostedServiceFactory;
        this.highPriorityServices = servicesFactory.createHighPriorityServices(mdibAccess);

        operationInvocationReceivers.forEach(receiver -> addOperationInvocationReceiver(receiver));

        setupHostedServices();
    }

    private void setupHostedServices() {
        final ClassLoader classLoader = getClass().getClassLoader();
        InputStream highPrioWsdl = classLoader.getResourceAsStream("wsdl/IEEE11073-20701-HighPriority-Services.wsdl");
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
                highPrioWsdl));
    }

    private void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        highPriorityServices.addOperationInvocationReceiver(receiver);
    }
}
