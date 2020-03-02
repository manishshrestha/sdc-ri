package org.somda.sdc.dpws.client.helper;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.model.HostServiceType;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.Relationship;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.network.LocalAddressResolver;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MalformedSoapMessageException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.EventSink;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;
import org.somda.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.somda.sdc.dpws.soap.wstransfer.TransferGetClient;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to resolve hosting service and hosted service information from {@link DiscoveredDevice} objects.
 */
public class HostingServiceResolver {
    private static final Logger LOG = LoggerFactory.getLogger(HostingServiceResolver.class);

    private final ExecutorWrapperService<ListeningExecutorService> networkJobExecutor;
    private final LocalAddressResolver localAddressResolver;
    private final TransportBindingFactory transportBindingFactory;
    private final RequestResponseClientFactory requestResponseClientFactory;
    private final TransferGetClient transferGetClient;
    private final JaxbUtil jaxbUtil;
    private final SoapUtil soapUtil;
    private final WsAddressingUtil wsaUtil;
    private final HostingServiceFactory hostingServiceFactory;
    private final HostedServiceFactory hostedServiceFactory;
    private final WsEventingEventSinkFactory eventSinkFactory;
    private final HttpUriBuilder uriBuilder;
    private final GetMetadataClient getMetadataClient;
    private final Duration maxWaitForFutures;

    @Inject
    HostingServiceResolver(@Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWaitForFutures,
                           @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> networkJobExecutor,
                           LocalAddressResolver localAddressResolver,
                           TransportBindingFactory transportBindingFactory,
                           RequestResponseClientFactory requestResponseClientFactory,
                           TransferGetClient transferGetClient,
                           GetMetadataClient getMetadataClient,
                           JaxbUtil jaxbUtil,
                           SoapUtil soapUtil,
                           WsAddressingUtil wsaUtil,
                           HostingServiceFactory hostingServiceFactory,
                           HostedServiceFactory hostedServiceFactory,
                           WsEventingEventSinkFactory eventSinkFactory,
                           HttpUriBuilder uriBuilder) {
        this.maxWaitForFutures = maxWaitForFutures;
        this.networkJobExecutor = networkJobExecutor;
        this.localAddressResolver = localAddressResolver;
        this.transportBindingFactory = transportBindingFactory;
        this.requestResponseClientFactory = requestResponseClientFactory;
        this.transferGetClient = transferGetClient;
        this.getMetadataClient = getMetadataClient;
        this.jaxbUtil = jaxbUtil;
        this.soapUtil = soapUtil;
        this.wsaUtil = wsaUtil;
        this.hostingServiceFactory = hostingServiceFactory;
        this.hostedServiceFactory = hostedServiceFactory;
        this.eventSinkFactory = eventSinkFactory;
        this.uriBuilder = uriBuilder;
    }

    /**
     * Resolve hosting service and hosted service information.
     * <p>
     * Use given {@link DiscoveredDevice} object to retrieve device UUID and metadata version of a device. If the device
     * exists in the registry already and there is no new metadata version, the method returns with cached information.
     * <p>
     * If the device is not registered in the registry, or the stored metadata version is out-dated, then the method
     * requests hosting service and hosted service information by using WS-Transfer Get, stores the information in the
     * registry, and returns it.
     *
     * @param discoveredDevice A well-populated {@link DiscoveredDevice} object, i.e., including XAddrs.
     * @return Future with resolved hosting service and hosted service information.
     */
    public ListenableFuture<HostingServiceProxy> resolveHostingService(DiscoveredDevice discoveredDevice) {
        return networkJobExecutor.get().submit(() -> {
            if (discoveredDevice.getXAddrs().isEmpty()) {
                throw new IllegalArgumentException("Given device proxy has no XAddrs. Connection aborted.");
            }

            RequestResponseClient rrClient = null;
            SoapMessage transferGetResponse = null;
            String activeXAddr = null;
            for (String xAddr : discoveredDevice.getXAddrs()) {
                try {
                    activeXAddr = xAddr;
                    rrClient = createRequestResponseClient(activeXAddr);
                    transferGetResponse = transferGetClient.sendTransferGet(rrClient, xAddr)
                            .get(maxWaitForFutures.toMillis(), TimeUnit.MILLISECONDS);
                    break;
                } catch (Exception e) {
                    LOG.debug("TransferGet to {} failed", xAddr, e);
                }
            }

            if (transferGetResponse == null) {
                throw new TransportException(String.format("None of the %s XAddr URL(s) responded with a valid TransferGet response",
                        discoveredDevice.getXAddrs().size()));
            }

            Metadata deviceMetadata = soapUtil.getBody(transferGetResponse, Metadata.class).orElseThrow(() ->
                    new MalformedSoapMessageException("Could not get metadata element from TransferGet response"));

            if (deviceMetadata.getMetadataSection().isEmpty()) {
                throw new MalformedSoapMessageException("No metadata sections in TransferGet response");
            }

            String deviceEprAddress = discoveredDevice.getEprAddress();
            long metadataVersion = discoveredDevice.getMetadataVersion();
            return extractHostingServiceProxy(deviceMetadata, rrClient,
                    deviceEprAddress, metadataVersion, activeXAddr).orElseThrow(() -> new MalformedSoapMessageException(
                    String.format("Could not resolve hosting service proxy information for %s",
                            deviceEprAddress)));
        });
    }

    private Optional<HostingServiceProxy> extractHostingServiceProxy(Metadata deviceMetadata,
                                                                     RequestResponseClient rrClient,
                                                                     String eprAddress,
                                                                     long metadataVersion,
                                                                     String xAddr) {
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
                    LOG.info("Resolve dpws:ThisDevice from {} failed", eprAddress);
                    continue;
                }
            }

            if (dialect.equals(DpwsConstants.MEX_DIALECT_THIS_MODEL)) {
                try {
                    thisModel = jaxbUtil.extractElement(metadataSection.getAny(), ThisModelType.class);
                    continue;
                } catch (Exception e) {
                    LOG.info("Resolve dpws:ThisModel from {} failed", eprAddress);
                    continue;
                }
            }

            if (dialect.equals(DpwsConstants.MEX_DIALECT_RELATIONSHIP)) {
                try {
                    Relationship rs = jaxbUtil.extractElement(metadataSection.getAny(), Relationship.class)
                            .orElseThrow(Exception::new);

                    if (!rs.getType().equals(DpwsConstants.RELATIONSHIP_TYPE_HOST)) {
                        LOG.debug("Incompatible dpws:Relationship type found for {}: {}", eprAddress, rs.getType());
                        continue;
                    }

                    relationshipData = extractRelationshipData(rs, eprAddress);
                } catch (Exception e) {
                    LOG.info("Resolve dpws:Relationship from {} failed", eprAddress);
                }
            }
        }

        if (thisDevice.isEmpty()) {
            LOG.info("No dpws:ThisDevice found for {}", eprAddress);
        }

        if (thisModel.isEmpty()) {
            LOG.info("No dpws:ThisModel found for {}", eprAddress);
        }

        RelationshipData rsDataFromOptional = relationshipData.orElseThrow(() ->
                new MalformedSoapMessageException(String.format("No dpws:Relationship found for %s, but required",
                        eprAddress)));

        final String epr = rsDataFromOptional.getEprAddress().orElseThrow(() ->
                new MalformedSoapMessageException(String.format("Malformed relationship data. Missing expected EPR: %s",
                        eprAddress)));
        return Optional.of(hostingServiceFactory.createHostingServiceProxy(
                epr,
                rsDataFromOptional.getTypes(),
                thisDevice.orElse(null),
                thisModel.orElse(null),
                rsDataFromOptional.getHostedServices(),
                metadataVersion,
                rrClient,
                xAddr));
    }

    private Optional<RelationshipData> extractRelationshipData(Relationship relationship, String eprAddress) {
        RelationshipData result = new RelationshipData();

        for (Object potentialRelationship : relationship.getAny()) {
            jaxbUtil.extractElement(potentialRelationship, HostServiceType.class).ifPresent(host -> {
                result.setEprAddress(wsaUtil.getAddressUri(host.getEndpointReference()).orElse(null));
                result.setTypes(host.getTypes());
            });

            jaxbUtil.extractElement(potentialRelationship, HostedServiceType.class).ifPresent(hsType ->
                    extractHostedServiceProxy(hsType)
                            .ifPresent(hsProxy -> result.getHostedServices()
                                    .put(hsProxy.getType().getServiceId(), hsProxy)));
        }

        if (result.getEprAddress() == null) {
            LOG.info("Found no valid dpws:Host for {}", eprAddress);
            return Optional.empty();
        }

        if (result.getHostedServices().isEmpty()) {
            LOG.info("Found no dpws:Hosted for {}", eprAddress);
        }

        return Optional.of(result);
    }

    private Optional<HostedServiceProxy> extractHostedServiceProxy(HostedServiceType host) {
        String activeHostedServiceEprAddress = null;
        RequestResponseClient rrClient = null;
        SoapMessage getMetadataResponse = null;
        for (EndpointReferenceType eprType : host.getEndpointReference()) {
            try {
                activeHostedServiceEprAddress = eprType.getAddress().getValue();
                rrClient = createRequestResponseClient(activeHostedServiceEprAddress);
                getMetadataResponse = getMetadataClient.sendGetMetadata(rrClient)
                        .get(maxWaitForFutures.toMillis(), TimeUnit.MILLISECONDS);
                break;
            } catch (Exception e) {
                LOG.debug("GetMetadata to {} failed", eprType.getAddress().getValue(), e);
            }
        }

        if (getMetadataResponse == null) {
            LOG.info("None of the {} hosted service EPR addresses responded with a valid GetMetadata response",
                    host.getEndpointReference().size());
            return Optional.empty();
        }

        final Optional<String> localAddress = localAddressResolver.getLocalAddress(activeHostedServiceEprAddress);
        if (localAddress.isEmpty()) {
            return Optional.empty();
        }

        String httpBinding = uriBuilder.buildUri(URI.create(activeHostedServiceEprAddress).getScheme(), localAddress.get(), 0);

        final EventSink eventSink = eventSinkFactory.createWsEventingEventSink(rrClient, httpBinding);
        return Optional.of(hostedServiceFactory.createHostedServiceProxy(host, rrClient,
                activeHostedServiceEprAddress, eventSink));
    }

    private RequestResponseClient createRequestResponseClient(String endpointAddress) {
        TransportBinding tBinding = transportBindingFactory.createTransportBinding(endpointAddress);
        return requestResponseClientFactory.createRequestResponseClient(tBinding);
    }

    private static class RelationshipData {
        private String eprAddress = null;
        private List<QName> types = null;
        private final Map<String, HostedServiceProxy> hostedServices = new HashMap<>();

        Optional<String> getEprAddress() {
            return Optional.ofNullable(eprAddress);
        }

        void setEprAddress(@Nullable String eprAddress) {
            this.eprAddress = eprAddress;
        }

        List<QName> getTypes() {
            return types == null ? Collections.emptyList() : types;
        }

        void setTypes(@Nullable List<QName> types) {
            this.types = types;
        }

        Map<String, HostedServiceProxy> getHostedServices() {
            return hostedServices;
        }
    }
}
