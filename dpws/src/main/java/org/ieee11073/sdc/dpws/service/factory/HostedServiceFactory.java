package org.ieee11073.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.device.WebService;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.service.HostedService;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.WritableHostedServiceProxy;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Factory to create {@link HostedService} related} and {@link HostedServiceProxy} instances.
 */
public interface HostedServiceFactory {
    /**
     * Create hosted service metadata instance.
     *
     * @param serviceId The service id. It is good practice to use a relative URL part (e.g., "SampleService").
     * @param types QName type list that matches at least the QName of the WSDL port type.
     * @param eprAddresses URLs the hosted service is requestable from, e.g. HTTP address with IPv4 and IPv6 host.
     * @param webService Web Service interceptor.
     * @param wsdlDocument Input stream to expose as WSDL description.
     */
    HostedService createHostedService(@Assisted String serviceId,
                                      @Assisted List<QName> types,
                                      @Assisted List<URI> eprAddresses,
                                      @Assisted WebService webService,
                                      @Assisted InputStream wsdlDocument);

    /**
     * Create hosted service metadata instance without available EPR addresses.
     *
     * This constructor can be used if EPR addresses shall be assigned automatically based on, e.g., a hosting service.
     *
     * @param serviceId The service id. Please use a relative URL part (e.g., "SampleService")
     * @param types QName type list that matches at least the QName(s) of the WSDL port type(s).
     * @param webService Web Service interceptor.
     */
    HostedService createHostedService(@Assisted String serviceId,
                                      @Assisted List<QName> types,
                                      @Assisted WebService webService,
                                      @Assisted InputStream wsdlDocument);

    /**
     * \todo documentation and implementation
     */
    WritableHostedServiceProxy createHostedServiceProxy(@Assisted HostedServiceType hostedServiceType,
                                                        @Assisted RequestResponseClient rrClient,
                                                        @Assisted PeerInformation peerInformation);
}
