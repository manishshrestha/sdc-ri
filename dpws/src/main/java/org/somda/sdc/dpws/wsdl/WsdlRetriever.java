package org.somda.sdc.dpws.wsdl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.http.HttpResponse;
import org.somda.sdc.dpws.http.factory.HttpClientFactory;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wstransfer.TransferGetClient;
import org.somda.sdc.dpws.wsdl.model.TDefinitions;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Utility to retrieve and handle WSDLs.
 * <p>
 * Three methods of retrieval are supported:
 * <ol>
 * <li> wsx:Location entries can retrieved via {@linkplain org.somda.sdc.dpws.http.HttpClient}
 * <li> WSDLs embedded in wsx:MetadataSection elements can be extracted
 * <li> WSDLs to be requested via wsx:MetadataReference can be requested via transfer get
 * </ol>
 * <em>WSDLs embedded in wsx:MetadataReference entries are currently unsupported.</em>
 */
public class WsdlRetriever {
    private static final Logger LOG = LogManager.getLogger();

    private final Logger instanceLogger;
    private final TransportBindingFactory transportBindingFactory;
    private final RequestResponseClientFactory requestResponseClientFactory;
    private final GetMetadataClient getMetadataClient;
    private final TransferGetClient transferGetClient;
    private final HttpClientFactory httpClientFactory;
    private final Duration maxWait;
    private final WsdlMarshalling wsdlMarshalling;

    @Inject
    WsdlRetriever(TransportBindingFactory transportBindingFactory,
                  RequestResponseClientFactory requestResponseClientFactory,
                  GetMetadataClient getMetadataClient,
                  TransferGetClient transferGetClient,
                  HttpClientFactory httpClientFactory,
                  WsdlMarshalling wsdlMarshalling,
                  @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWait,
                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.transportBindingFactory = transportBindingFactory;
        this.requestResponseClientFactory = requestResponseClientFactory;
        this.getMetadataClient = getMetadataClient;
        this.transferGetClient = transferGetClient;
        this.httpClientFactory = httpClientFactory;
        this.wsdlMarshalling = wsdlMarshalling;
        this.maxWait = maxWait;
    }

    /**
     * Retrieves all WSDLs of the Provider the client is connected to.
     *
     * @param hostingServiceProxy to retrieve WSDLs from
     * @return Map with hosted service name as key and all associated WSDLs as values
     * @throws IOException        in case WSDLs could not be retrieved
     * @throws TransportException in case a transport related error occurs (connection refused, ...)
     */
    public Map<String, List<String>> retrieveWsdls(HostingServiceProxy hostingServiceProxy) throws IOException, TransportException {
        instanceLogger.debug("Retrieving WSDLs for {}", hostingServiceProxy.getActiveXAddr());
        var wsdlMap = new HashMap<String, List<String>>();

        var hostedServices = hostingServiceProxy.getHostedServices();
        for (Map.Entry<String, HostedServiceProxy> entry : hostedServices.entrySet()) {

            String serviceName = entry.getKey();
            HostedServiceProxy hostedServiceProxy = entry.getValue();
            SoapMessage metadataResponse;
            LOG.debug("Retrieving WSDL for service {}", serviceName);
            try {
                metadataResponse = getMetadataClient.sendGetMetadata(hostedServiceProxy.getRequestResponseClient())
                        .get(maxWait.toSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new IOException("Could not retrieve Metadata from device for service " + serviceName);
            }

            var bodyElements = metadataResponse.getOriginalEnvelope().getBody().getAny();
            var getMetadataResponse = (Metadata) bodyElements.get(0);

            try {
                var wsdl = retrieveWsdlFromMetadata(getMetadataResponse);
                wsdlMap.put(serviceName, wsdl);
            } catch (IOException | TransportException e) {
                LOG.error("Could not retrieve WSDL for service {}", serviceName);
                LOG.trace("Could not retrieve WSDL for service {}", serviceName, e);
                throw e;
            }
        }

        return wsdlMap;
    }

    List<String> retrieveWsdlFromMetadata(Metadata metadata) throws TransportException, IOException {
        // find metadata section with wsdl
        var wsdlSections = metadata.getMetadataSection()
                .stream()
                .filter(section -> WsMetadataExchangeConstants.DIALECT_WSDL.equals(section.getDialect()))
                .collect(Collectors.toList());
        if (wsdlSections.isEmpty()) {
            throw new IOException(
                    "Metadata for service did not contain any wsdl references"
            );
        }

        var collectedWsdls = new ArrayList<String>();

        for (var wsdlSection : wsdlSections) {
            // only one of these is allowed to exist, so verify that is true
            var location = wsdlSection.getLocation();
            var reference = wsdlSection.getMetadataReference();
            var any = wsdlSection.getAny();
            boolean hasLocation = location != null;
            boolean hasReference = reference != null;
            boolean hasAny = any != null;

            if (!BooleanUtils.xor(new boolean[]{hasLocation, hasReference, hasAny})) {
                throw new IOException(
                        "Metadata for service has too many possible wsdl locations. It had"
                                + " DialectSpecificElement: " + hasAny
                                + " MetadataReference: " + hasReference
                                + " Location: " + hasLocation
                                + ". Only one is allowed."
                );
            }

            if (hasLocation) {
                LOG.debug("WSDL is stored at location {}", location);
                collectedWsdls.add(retrieveWsdlFromLocation(location));
            } else if (hasReference) {
                LOG.debug("WSDL stored as MetadataReference {}", reference);
                collectedWsdls.addAll(retrieveWsdlFromMetadataReference(reference));
            } else if (hasAny) {
                LOG.debug("WSDL is stored in metadata response");
                collectedWsdls.add(retrieveWsdlFromAny(any));
            } else {
                throw new IOException("Unexpected error while trying to retrieve WSDL: No match for any method.");
            }
        }

        return collectedWsdls;
    }

    @SuppressWarnings("unchecked")
    private String retrieveWsdlFromAny(Object any) throws IOException {
        try {
            var definition = ((JAXBElement<TDefinitions>) any);
            return retrieveEmbeddedWsdlFromMetadata(definition.getValue());
        } catch (ClassCastException e) {
            LOG.error(
                    "Any node in metadata was not of instance JAXBElement<TDefinitions>,"
                            + " was {} instead, cannot handle it",
                    any.getClass().getSimpleName()
            );
            throw new IOException("Any node in metadata was not of instance JAXBElement<TDefinitions>, cannot handle it");
        }
    }

    private String retrieveWsdlFromLocation(String location) throws TransportException {
        var client = httpClientFactory.createHttpClient();
        HttpResponse response = client.sendGet(location);
        if (response.getStatusCode() >= 300) {
            throw new TransportException("Unexpected HTTP status code. Was " + response.getStatusCode());
        }
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    private List<String> retrieveWsdlFromMetadataReference(EndpointReferenceType reference)
            throws TransportException, IOException {
        var address = reference.getAddress().getValue();
        var referenceParameters = reference.getReferenceParameters();

        var transportBinding = transportBindingFactory.createTransportBinding(address);
        var rrClient = requestResponseClientFactory.createRequestResponseClient(transportBinding);
        SoapMessage getResponse;
        try {
            if (referenceParameters != null) {
                getResponse = transferGetClient
                        .sendTransferGet(rrClient, address, referenceParameters)
                        .get(maxWait.toSeconds(), TimeUnit.SECONDS);
            } else {
                getResponse = transferGetClient
                        .sendTransferGet(rrClient, address)
                        .get(maxWait.toSeconds(), TimeUnit.SECONDS);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            instanceLogger.error("Could not retrieve Metadata from device. Message: {}", e.getMessage());
            instanceLogger.trace("Could not retrieve Metadata from device", e);
            throw new TransportException("Could not retrieve Metadata from device", e);
        }

        var bodyElements = getResponse.getOriginalEnvelope().getBody().getAny();
        var getMetadataResponse = (Metadata) bodyElements.get(0);

        return retrieveWsdlFromMetadata(getMetadataResponse);
    }

    private String retrieveEmbeddedWsdlFromMetadata(TDefinitions wsdlNode) throws IOException {
        var output = new ByteArrayOutputStream();
        try {
            wsdlMarshalling.marshal(wsdlNode, output);
        } catch (JAXBException e) {
            instanceLogger.error("Error while marshalling embedded wsdl. Message: {}", e.getMessage());
            instanceLogger.error("Error while marshalling embedded wsdl", e);
            throw new IOException("Error while marshalling embedded wsdl", e);
        }

        return output.toString(StandardCharsets.UTF_8);
    }
}
