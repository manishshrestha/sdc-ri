package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * {@link HostingServiceProxy} that allows field updates.
 */
public interface WritableHostingServiceProxy extends HostingServiceProxy {

    /**
     * Get hosted service map as {@link WritableHostedServiceProxy} instances.
     */
    Map<String, WritableHostedServiceProxy> getWritableHostedServices();

    /**
     * Update all fields from given {@link WritableHostedServiceProxy} instance.
     */
    void updateProxyInformation(WritableHostingServiceProxy hostingServiceProxy);

    /**
     * Update all fields by separated parameters.
     * 
     */
    void updateProxyInformation(URI endpointReferenceAddress,
                                List<QName> types,
                                @Nullable ThisDeviceType thisDevice,
                                @Nullable ThisModelType thisModel,
                                Map<String, WritableHostedServiceProxy> hostedServices,
                                long metadataVersion,
                                RequestResponseClient requestResponseClient,
                                URI activeXAddr);
}
