package org.ieee11073.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.service.helper.MetadataSectionUtil;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.GetMetadata;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Interceptor to serve GetMetadata requests on hosted services.
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
    InterceptorResult processGetMetadata(RequestResponseObject rrObj) throws SoapFaultException {
        GetMetadata body = soapUtil.getBody(rrObj.getRequest(), GetMetadata.class).orElse(null);

        Metadata metadata = mexFactory.createMetadata();
        List<MetadataSection> metadataSection = metadata.getMetadataSection();

        // \todo Is host relationship required here? - probably not
        metadataSection.add(metadataSectionUtil.createRelationship(targetService.getEndpointReference(),
                targetService.getTypes(), Arrays.asList(hostedService)));

        if (body == null || body.getDialect() == null || body.getDialect().isEmpty() ||
                body.getDialect().equals(WsMetadataExchangeConstants.DIALECT_WSDL)) {
            hostedService.getWsdlLocations().forEach(uri -> metadataSection.add(createWsdlMetadataSection(uri)));
        }

        rrObj.getResponse().getWsAddressingHeader().setAction(
                wsaUtil.createAttributedURIType(WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_RESPONSE));

        metadata.setMetadataSection(metadataSection);
        soapUtil.setBody(metadata, rrObj.getResponse());

        return InterceptorResult.PROCEED;
    }

    private MetadataSection createWsdlMetadataSection(URI uri) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(WsMetadataExchangeConstants.DIALECT_WSDL);
        metadataSection.setAny(mexFactory.createLocation(uri.toString()));
        return metadataSection;
    }
}
