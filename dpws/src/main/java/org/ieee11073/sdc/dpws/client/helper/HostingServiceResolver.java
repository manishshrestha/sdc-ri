package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.DpwsConstants;
import org.ieee11073.sdc.dpws.TransportBinding;
import org.ieee11073.sdc.dpws.client.DeviceProxy;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.guice.NetworkJobThreadPool;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.*;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.service.WritableHostedServiceProxy;
import org.ieee11073.sdc.dpws.service.WritableHostingServiceProxy;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.service.factory.HostingServiceFactory;
import org.ieee11073.sdc.dpws.service.helper.HostResolver;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.exception.MalformedSoapMessageException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.ieee11073.sdc.dpws.soap.wstransfer.TransferGetClient;
import org.ieee11073.sdc.common.helper.JaxbUtil;
import org.ieee11073.sdc.dpws.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.*;

/**
 * Helper class to resolve hosting service and hosted service information from {@link DeviceProxy} objects.
 */
public class HostingServiceResolver {
    private static final Logger LOG = LoggerFactory.getLogger(HostingServiceResolver.class);

    private final HostingServiceRegistry hostingServiceRegistry;
    private final ListeningExecutorService networkJobExecutor;
    private final HostResolver hostResolver;
    private final TransportBindingFactory transportBindingFactory;
    private final RequestResponseClientFactory requestResponseClientFactory;
    private final TransferGetClient transferGetClient;
    private final JaxbUtil jaxbUtil;
    private final SoapUtil soapUtil;
    private final WsAddressingUtil wsaUtil;
    private final HostingServiceFactory hostingServiceFactory;
    private final HostedServiceFactory hostedServiceFactory;

    @AssistedInject
    HostingServiceResolver(@Assisted HostingServiceRegistry hostingServiceRegistry,
                           @NetworkJobThreadPool ListeningExecutorService networkJobExecutor,
                           HostResolver hostResolver,
                           TransportBindingFactory transportBindingFactory,
                           RequestResponseClientFactory requestResponseClientFactory,
                           TransferGetClient transferGetClient,
                           JaxbUtil jaxbUtil,
                           SoapUtil soapUtil,
                           WsAddressingUtil wsaUtil,
                           HostingServiceFactory hostingServiceFactory,
                           HostedServiceFactory hostedServiceFactory) {
        this.hostingServiceRegistry = hostingServiceRegistry;
        this.networkJobExecutor = networkJobExecutor;
        this.hostResolver = hostResolver;
        this.transportBindingFactory = transportBindingFactory;
        this.requestResponseClientFactory = requestResponseClientFactory;
        this.transferGetClient = transferGetClient;
        this.jaxbUtil = jaxbUtil;
        this.soapUtil = soapUtil;
        this.wsaUtil = wsaUtil;
        this.hostingServiceFactory = hostingServiceFactory;
        this.hostedServiceFactory = hostedServiceFactory;
    }

    /**
     * Resolve hosting service and hosted service information.
     *
     * Use given {@link DeviceProxy} object to retrieve device UUID and metadata version of a device. If the device
     * exists in the registry already and there is no new metadata version, the method returns with cached information.
     *
     * If the device is not registered in the registry, or the stored metadata version is out-dated, then the method
     * requests hosting service and hosted service information by using WS-Transfer Get, stores the information in the
     * registry, and returns it.
     *
     * @param deviceProxy A well-populated {@link DeviceProxy} object.
     * @return Future with resolved hosting service and hosted service information.
     */
    public ListenableFuture<HostingServiceProxy> resolveHostingService(DeviceProxy deviceProxy) {
        return networkJobExecutor.submit(() -> {
            URI deviceEprAddress = deviceProxy.getEprAddress();
            long metadataVersion = deviceProxy.getMetadataVersion();

            // Check if hosting service was already seen
            Optional<WritableHostingServiceProxy> hostingService = hostingServiceRegistry.get(deviceEprAddress);
            if (hostingService.isPresent()) {
                // If metadata has not changed, return from registry directly
                if (metadataVersion <= hostingService.get().getMetadataVersion()) {
                    return hostingService.get();
                }
            }

            // Hosting service not seen before, or metadata changed. Hence, resolve from device
            PeerInformation peerInfo = hostResolver.deriveFirstResolvable(deviceProxy.getXAddrs())
                    .orElseThrow(() -> new TransportException(
                            String.format("Host not found: %s", Arrays.toString(deviceProxy.getXAddrs().toArray()))));

            RequestResponseClient rrClient = createRequestResponseClient(peerInfo.getRemoteAddress());

            ListenableFuture<SoapMessage> transferGetFuture = transferGetClient.sendTransferGet(rrClient, peerInfo
                    .getRemoteAddress().toString());

            Metadata deviceMetadata = soapUtil.getBody(transferGetFuture.get(), Metadata.class).orElseThrow(() ->
                    new MalformedSoapMessageException("Could not get metadata element from TransferGet response."));

            if (deviceMetadata.getMetadataSection().isEmpty()) {
                throw new MalformedSoapMessageException("No metadata sections in TransferGet response.");
            }

            WritableHostingServiceProxy resolvedProxy = extractHostingServiceProxy(deviceMetadata, rrClient, peerInfo,
                    deviceEprAddress, metadataVersion).orElseThrow(() -> new MalformedSoapMessageException(
                    String.format("Could not resolve hosting service proxy information for {}",
                            deviceEprAddress)));

            return hostingServiceRegistry.registerOrUpdate(resolvedProxy);
        });
    }

    private Optional<WritableHostingServiceProxy> extractHostingServiceProxy(Metadata deviceMetadata,
                                                                             RequestResponseClient rrClient,
                                                                             PeerInformation peerInformation,
                                                                             URI deviceUuid,
                                                                             long metadataVersion) {
        Optional<ThisDeviceType> thisDevice = Optional.empty();
        Optional<ThisModelType> thisModel = Optional.empty();
        Optional<RelationshipData> relationshipData = Optional.empty();

        for (Object potentialMetadataSection : deviceMetadata.getMetadataSection()) {
            MetadataSection metadataSection = jaxbUtil.extractElement(potentialMetadataSection,
                    MetadataSection.class).orElse(null);
            if (metadataSection == null) {
                continue;
            }

            String dialect = metadataSection.getDialect();

            if (dialect.equals(DpwsConstants.MEX_DIALECT_THIS_DEVICE)) {
                try {
                    thisDevice = jaxbUtil.extractElement(metadataSection.getAny(), ThisDeviceType.class);
                    continue;
                } catch (Exception e) {
                    LOG.info("Resolve dpws:ThisDevice from {} failed.", deviceUuid);
                    continue;
                }
            }

            if (dialect.equals(DpwsConstants.MEX_DIALECT_THIS_MODEL)) {
                try {
                    thisModel = jaxbUtil.extractElement(metadataSection.getAny(), ThisModelType.class);
                    continue;
                } catch (Exception e) {
                    LOG.info("Resolve dpws:ThisModel from {} failed.", deviceUuid);
                    continue;
                }
            }

            if (dialect.equals(DpwsConstants.MEX_DIALECT_RELATIONSHIP)) {
                try {
                    Relationship rs = jaxbUtil.extractElement(metadataSection.getAny(), Relationship.class)
                            .orElseThrow(Exception::new);

                    if (!rs.getType().equals(DpwsConstants.RELATIONSHIP_TYPE_HOST)) {
                        LOG.info("Incompatible dpws:Relationship type found for {}: {}.", deviceUuid, rs.getType());
                        continue;
                    }

                    relationshipData = extractRelationshipData(rs, deviceUuid);
                } catch (Exception e) {
                    LOG.info("Resolve dpws:Relationship from {} failed.", deviceUuid);
                }
            }
        }

        if (!thisDevice.isPresent()) {
            LOG.info("No dpws:ThisDevice found for {}.", deviceUuid);
        }

        if (!thisModel.isPresent()) {
            LOG.info("No dpws:ThisModel found for {}.", deviceUuid);
        }

        RelationshipData rsDataFromOptional = relationshipData.orElseThrow(() ->
                new MalformedSoapMessageException(String.format("No dpws:Relationship found for %s, but required",
                        deviceUuid)));

        WritableHostingServiceProxy hsp = hostingServiceFactory.createHostingServiceProxy(
                rsDataFromOptional.getDeviceUuid(),
                rsDataFromOptional.getTypes(),
                thisDevice.orElse(null),
                thisModel.orElse(null),
                rsDataFromOptional.getHostedServices(),
                metadataVersion,
                rrClient,
                peerInformation);
        return Optional.of(hsp);
    }

    private Optional<RelationshipData> extractRelationshipData(Relationship relationship, URI deviceUuid) {
        RelationshipData result = new RelationshipData();

        for (Object potentialRelationship : relationship.getAny()) {
            jaxbUtil.extractElement(potentialRelationship, HostServiceType.class).ifPresent(host -> {
                result.setDeviceUuid(wsaUtil.getAddressUri(host.getEndpointReference()).orElse(null));
                result.setTypes(host.getTypes());
            });

            jaxbUtil.extractElement(potentialRelationship, HostedServiceType.class).ifPresent(hsType ->
                    extractHostedServiceProxy(hsType, deviceUuid)
                            .ifPresent(hsProxy -> result.getHostedServices()
                                    .put(URI.create(hsProxy.getType().getServiceId()), hsProxy)));
        }

        if (result.getDeviceUuid() == null) {
            LOG.info("Found no valid dpws:Host for {}", deviceUuid);
            return Optional.empty();
        }

        if (result.getHostedServices().isEmpty()) {
            LOG.info("Found no dpws:Hosted for {}", deviceUuid);
        }

        return Optional.of(result);
    }

    private Optional<WritableHostedServiceProxy> extractHostedServiceProxy(HostedServiceType host, URI deviceUuid) {
        Optional<PeerInformation> peerInformation = hostResolver.deriveFirstResolvable(host);
        if (!peerInformation.isPresent()) {
            LOG.info("Failed to resolve hosted service with ServiceId {} for {}", host.getServiceId(), deviceUuid);
            return Optional.empty();
        }

        RequestResponseClient rrClient = createRequestResponseClient(peerInformation.get().getRemoteAddress());

        return Optional.of(hostedServiceFactory.createHostedServiceProxy(host, rrClient, peerInformation.get()));
    }

    private RequestResponseClient createRequestResponseClient(URI endpointAddress) {
        TransportBinding tBinding = transportBindingFactory.createTransportBinding(endpointAddress);
        return requestResponseClientFactory.createRequestResponseClient(tBinding);
    }

    private class RelationshipData {
        private URI deviceUuid = null;
        private List<QName> types = null;
        private Map<URI, WritableHostedServiceProxy> hostedServices = new HashMap<>();

        public URI getDeviceUuid() {
            return deviceUuid;
        }

        public void setDeviceUuid(URI deviceUuid) {
            this.deviceUuid = deviceUuid;
        }

        public List<QName> getTypes() {
            return types;
        }

        public void setTypes(List<QName> types) {
            this.types = types;
        }

        public Map<URI, WritableHostedServiceProxy> getHostedServices() {
            return hostedServices;
        }
    }
}
