package org.ieee11073.sdc.dpws.service.helper;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.DpwsConstants;
import org.ieee11073.sdc.dpws.model.HostServiceType;
import org.ieee11073.sdc.dpws.model.Relationship;
import org.ieee11073.sdc.dpws.service.HostedService;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Utility class to create metadata sections for WS-MetadataExchange.
 */
public class MetadataSectionUtil {

    private final ObjectFactory mexFactory;
    private final org.ieee11073.sdc.dpws.model.ObjectFactory dpwsFactory;

    @Inject
    MetadataSectionUtil(ObjectFactory mexFactory,
                        org.ieee11073.sdc.dpws.model.ObjectFactory dpwsFactory) {
        this.mexFactory = mexFactory;
        this.dpwsFactory = dpwsFactory;
    }

    public MetadataSection createRelationship(EndpointReferenceType hostingServiceEpr,
                                              List<QName> hostingServiceTypes,
                                              List<HostedService> hostedServices) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_RELATIONSHIP);
        Relationship relationship = dpwsFactory.createRelationship();
        relationship.setType(DpwsConstants.RELATIONSHIP_TYPE_HOST);

        HostServiceType hostServiceType = dpwsFactory.createHostServiceType();
        hostServiceType.setEndpointReference(hostingServiceEpr);
        hostServiceType.setTypes(hostingServiceTypes);
        relationship.getAny().add(dpwsFactory.createHost(hostServiceType));
        hostedServices.forEach(hostedService ->
                relationship.getAny().add(dpwsFactory.createHosted(hostedService.getType())));

        metadataSection.setAny(relationship);
        return metadataSection;
    }
}
