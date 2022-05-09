package org.somda.sdc.dpws.service.helper;

import com.google.inject.Inject;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.model.HostServiceType;
import org.somda.sdc.dpws.model.Relationship;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Utility class to create metadata sections for WS-MetadataExchange.
 */
public class MetadataSectionUtil {

    private final org.somda.sdc.dpws.model.ObjectFactory dpwsFactory;

    @Inject
    MetadataSectionUtil(org.somda.sdc.dpws.model.ObjectFactory dpwsFactory) {
        this.dpwsFactory = dpwsFactory;
    }

    /**
     * Creates a metadata section for a DPWS Host.
     *
     * @param hostingServiceEpr of the host
     * @param hostingServiceTypes of the services
     * @param hostedServices of the host
     * @return a new metadata section containing host and services.
     */
    public MetadataSection createRelationship(EndpointReferenceType hostingServiceEpr,
                                              List<QName> hostingServiceTypes,
                                              List<HostedService> hostedServices) {
        HostServiceType hostServiceType = HostServiceType.builder()
            .withEndpointReference(hostingServiceEpr)
            .withTypes(hostingServiceTypes)
            .build();

        var relationshipBuilder = Relationship.builder()
            .withType(DpwsConstants.RELATIONSHIP_TYPE_HOST)
            .addAny(dpwsFactory.createHost(hostServiceType));

        hostedServices.forEach(hostedService ->
            relationshipBuilder.addAny(dpwsFactory.createHosted(hostedService.getType())));

        return MetadataSection.builder()
            .withDialect(DpwsConstants.MEX_DIALECT_RELATIONSHIP)
            .withAny(relationshipBuilder.build()).build();
    }
}
