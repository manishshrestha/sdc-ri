package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.service.helper.MetadataSectionUtil;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.GetMetadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory;

import java.util.Collections;
import java.util.List;

/**
 * Server interceptor to serve GetMetadata requests on hosted services.
 */
public class HostedServiceInterceptor implements Interceptor {
    private final HostedService hostedService;
    private final WsDiscoveryTargetService targetService;
    private final SoapUtil soapUtil;
    private final ObjectFactory mexFactory;
    private final WsAddressingUtil wsaUtil;
    private final MetadataSectionUtil metadataSectionUtil;

    @AssistedInject
    HostedServiceInterceptor(@Assisted HostedService hostedService,
                             @Assisted WsDiscoveryTargetService targetService,
                             SoapUtil soapUtil,
                             ObjectFactory mexFactory,
                             WsAddressingUtil wsaUtil,
                             MetadataSectionUtil metadataSectionUtil) {
        this.hostedService = hostedService;
        this.targetService = targetService;
        this.soapUtil = soapUtil;
        this.mexFactory = mexFactory;
        this.wsaUtil = wsaUtil;
        this.metadataSectionUtil = metadataSectionUtil;
    }

    @MessageInterceptor(value = WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_REQUEST,
            direction = Direction.REQUEST)
    void processGetMetadata(RequestResponseObject rrObj) {
        GetMetadata body = soapUtil.getBody(rrObj.getRequest(), GetMetadata.class).orElse(null);

        Metadata metadata = mexFactory.createMetadata();
        List<MetadataSection> metadataSection = metadata.getMetadataSection();

        // \todo DGr is host relationship required here?
        metadataSection.add(metadataSectionUtil.createRelationship(targetService.getEndpointReference(),
                targetService.getTypes(), Collections.singletonList(hostedService)));

        if (body == null || body.getDialect() == null || body.getDialect().isEmpty() ||
                body.getDialect().equals(WsMetadataExchangeConstants.DIALECT_WSDL)) {
            hostedService.getWsdlLocations().forEach(uri -> metadataSection.add(createWsdlMetadataSection(uri)));
        }

        rrObj.getResponse().getWsAddressingHeader().setAction(
                wsaUtil.createAttributedURIType(WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_RESPONSE));

        metadata.setMetadataSection(metadataSection);
        soapUtil.setBody(metadata, rrObj.getResponse());
    }

    private MetadataSection createWsdlMetadataSection(String uri) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(WsMetadataExchangeConstants.DIALECT_WSDL);
        metadataSection.setAny(mexFactory.createLocation(uri));
        return metadataSection;
    }
}
