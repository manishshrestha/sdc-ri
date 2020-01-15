package org.somda.sdc.dpws.service;

import com.google.common.io.ByteStreams;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.common.util.ObjectUtil;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@linkplain HostedService}.
 */
public class HostedServiceImpl implements HostedService {
    private final String serviceId;
    private final List<QName> types;
    private final List<EndpointReferenceType> eprs;
    private final WebService webService;
    private final InputStream wsdlDocument;
    private final ObjectFactory dpwsFactory;
    private final ObjectUtil objectUtil;
    private final List<URI> wsdlLocations;

    @AssistedInject
    HostedServiceImpl(@Assisted String serviceId,
                      @Assisted List<QName> types,
                      @Assisted List<URI> eprAddresses,
                      @Assisted WebService webService,
                      @Assisted InputStream wsdlDocumentStream,
                      ObjectFactory dpwsFactory,
                      ObjectUtil objectUtil,
                      WsAddressingUtil wsaUtil) throws IOException {
        this.serviceId = serviceId;
        this.types = types;
        this.eprs = eprAddresses.parallelStream()
                .map(wsaUtil::createEprWithAddress)
                .collect(Collectors.toList());
        this.webService = webService;
        this.wsdlDocument = new ByteArrayInputStream(ByteStreams.toByteArray(wsdlDocumentStream));
        this.dpwsFactory = dpwsFactory;
        this.objectUtil = objectUtil;
        this.wsdlLocations = new ArrayList<>();
    }

    @AssistedInject
    HostedServiceImpl(@Assisted String serviceId,
                      @Assisted List<QName> types,
                      @Assisted WebService webService,
                      @Assisted InputStream wsdlDocumentStream,
                      ObjectFactory dpwsFactory,
                      ObjectUtil objectUtil,
                      WsAddressingUtil wsaUtil) throws IOException {
        this(serviceId, types, new ArrayList<>(), webService, wsdlDocumentStream, dpwsFactory, objectUtil, wsaUtil);
    }

    @Override
    public HostedServiceType getType() {
        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.setServiceId(serviceId);
        hst.setEndpointReference(objectUtil.deepCopy(eprs));
        hst.setTypes(new ArrayList<>(types));
        return hst;
    }

    @Override
    public WebService getWebService() {
        return webService;
    }

    @Override
    public InputStream getWsdlDocument() {
        return wsdlDocument;
    }

    @Override
    public List<URI> getWsdlLocations() {
        return wsdlLocations;
    }
}
