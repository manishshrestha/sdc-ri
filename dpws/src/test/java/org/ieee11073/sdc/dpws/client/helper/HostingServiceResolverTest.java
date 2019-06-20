package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import org.ieee11073.sdc.dpws.*;
import org.ieee11073.sdc.dpws.client.DiscoveredDevice;
import org.ieee11073.sdc.dpws.service.EventSinkAccess;
import org.ieee11073.sdc.dpws.client.helper.factory.ClientHelperFactory;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.ni.LocalAddressResolver;
import org.ieee11073.sdc.dpws.service.*;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.service.factory.HostingServiceFactory;
import org.ieee11073.sdc.dpws.service.helper.MetadataSectionUtil;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wseventing.EventSink;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.ieee11073.sdc.dpws.soap.wstransfer.TransferGetClient;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HostingServiceResolverTest extends DpwsTest {
    private MockTransferGetClient mockTransferGetClient;
    private MockGetMetadataClient mockGetMetadataClient;

    private URI expectedDeviceEprAddress;
    private List<QName> expectedHostingServiceQNameTypes;
    private String expectedSerialNumber;
    private ThisDeviceType expectedDeviceType;
    private String expectedModelNumber;
    private ThisModelType expectedModelType;

    private String expectedServiceId;
    private List<EndpointReferenceType> expectedHostedServiceEprs;
    private List<QName> expectedHostedServiceQNameTypes;

    private HostingServiceFactory hostingServiceFactory;
    private org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory mexFactory;
    private org.ieee11073.sdc.dpws.model.ObjectFactory dpwsFactory;
    private MetadataSectionUtil metadataSectionUtil;
    private WsAddressingUtil wsaUtil;
    private SoapUtil soapUtil;
    private HostedServiceFactory hostedServiceFactory;
    private HostedServiceType expectedHostedServiceType;

    @Override
    @Before
    public void setUp() throws Exception {
        mockTransferGetClient = new MockTransferGetClient();
        mockGetMetadataClient = new MockGetMetadataClient();
        overrideBindings(new AbstractModule() {
            @Override
            protected void configure() {
                bind(RequestResponseClientFactory.class).toInstance(callback -> mock(RequestResponseClient.class));
                bind(TransferGetClient.class).toInstance(mockTransferGetClient);
                bind(GetMetadataClient.class).toInstance(mockGetMetadataClient);
                bind(LocalAddressResolver.class).to(LocalAddressResolverMock.class);
            }
        });
        super.setUp();

        wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        soapUtil = getInjector().getInstance(SoapUtil.class);
        metadataSectionUtil = getInjector().getInstance(MetadataSectionUtil.class);
        dpwsFactory = getInjector().getInstance(org.ieee11073.sdc.dpws.model.ObjectFactory.class);
        mexFactory = getInjector().getInstance(org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory.class);

        hostingServiceFactory = getInjector().getInstance(HostingServiceFactory.class);
        hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);

        expectedDeviceEprAddress = URI.create("urn:uuid:71c219ae-3b55-404f-803b-1e72390f73ba");
        expectedHostingServiceQNameTypes = Arrays.asList(new QName("http://device", "Type1"),
                new QName("http://device", "Type2"));

        expectedSerialNumber = "1234-5678-9101-2131";
        ThisDeviceBuilder tdBuilder = getInjector().getInstance(ThisDeviceBuilder.class);
        expectedDeviceType = tdBuilder.setSerialNumber(expectedSerialNumber).get();
        expectedModelNumber = "0815";
        ThisModelBuilder tmBuilder = getInjector().getInstance(ThisModelBuilder.class);
        expectedModelType = tmBuilder.setModelNumber(expectedModelNumber).get();

        expectedServiceId = "Service1";
        expectedHostedServiceEprs = Arrays.asList(wsaUtil.createEprWithAddress("http://hosted-service-epr1"),
                wsaUtil.createEprWithAddress("http://hosted-service-epr2"));
        expectedHostedServiceQNameTypes = Arrays.asList(new QName("http://service", "Type1"),
                new QName("http://service", "Type2"));
        expectedHostedServiceType = dpwsFactory.createHostedServiceType();
        expectedHostedServiceType.setEndpointReference(expectedHostedServiceEprs);
        expectedHostedServiceType.setServiceId(expectedServiceId.toString());
        expectedHostedServiceType.setTypes(expectedHostedServiceQNameTypes);
    }

    @Test
    public void resolveHostingService() throws Exception {
        Map<String, HostedServiceProxy> hostedServiceProxies = new HashMap<>();
        hostedServiceProxies.put(expectedServiceId, hostedServiceFactory.createHostedServiceProxy(
                expectedHostedServiceType,
                mock(RequestResponseClient.class),
                URI.create("http://mock-apr-address"),
                mock(EventSink.class)));

        // When no existing service is found in registry on resolving
        // Then expect the resolver to resolve the service according to the following message
        mockTransferGetClient.setTransferGetMessages(Arrays.asList(createTransferGetMessage(
                expectedDeviceEprAddress,
                expectedHostingServiceQNameTypes,
                expectedModelType,
                expectedDeviceType,
                Arrays.asList(createHostedService(expectedServiceId,
                        expectedHostedServiceQNameTypes,
                        expectedHostedServiceEprs))
        )));

        mockGetMetadataClient.setGetMetadataMessages(
                Arrays.asList(createGetMetadataMessage(), createGetMetadataMessage()));

        HostingServiceResolver hostingServiceResolver = getInjector().getInstance(HostingServiceResolver.class);
        long expectedMetadataVersion = 100;
        DiscoveredDevice expectedDiscoveredDevice = createDiscoveredDevice(expectedDeviceEprAddress, Arrays.asList("http://xAddr"),
                expectedMetadataVersion);
        ListenableFuture<HostingServiceProxy> hsF = hostingServiceResolver.resolveHostingService(expectedDiscoveredDevice);
        try {
            HostingServiceProxy actualHsp = hsF.get();
            assertEquals(expectedDeviceEprAddress, actualHsp.getEndpointReferenceAddress());
            assertEquals(expectedMetadataVersion, actualHsp.getMetadataVersion());
            assertTrue(actualHsp.getThisDevice().isPresent());
            assertEquals(expectedSerialNumber, actualHsp.getThisDevice().get().getSerialNumber());
            assertTrue(actualHsp.getThisModel().isPresent());
            assertEquals(expectedModelNumber, actualHsp.getThisModel().get().getModelNumber());
            assertEquals(1, actualHsp.getHostedServices().size());
            HostedServiceProxy actualHostedServiceProxy = actualHsp.getHostedServices().get(expectedServiceId);
            assertNotNull(actualHostedServiceProxy);
            assertEquals(expectedHostedServiceQNameTypes.toString(),
                    actualHostedServiceProxy.getType().getTypes().toString());
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    private HostedService createHostedService(String serviceId, List<QName> types, List<EndpointReferenceType> eprs) {
        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.setTypes(types);
        hst.setServiceId(serviceId.toString());
        hst.setEndpointReference(eprs);
        HostedService hs = mock(HostedService.class);
        when(hs.getType()).thenReturn(hst);
        return hs;
    }

    private SoapMessage createTransferGetMessage(URI eprAddress,
                                                 List<QName> types,
                                                 ThisModelType thisModel,
                                                 ThisDeviceType thisDevice,
                                                 List<HostedService> hostedServices) {
        Metadata metadata = mexFactory.createMetadata();
        List<MetadataSection> metadataSection = metadata.getMetadataSection();

        metadataSection.add(createThisModel(thisModel));
        metadataSection.add(createThisDevice(thisDevice));

        metadataSection.add(metadataSectionUtil.createRelationship(
                wsaUtil.createEprWithAddress(eprAddress),
                types,
                hostedServices));

        metadata.setMetadataSection(metadataSection);

        SoapMessage msg = soapUtil.createMessage();
        soapUtil.setBody(metadata, msg);
        return msg;
    }

    private SoapMessage createGetMetadataMessage() {
        Metadata metadata = mexFactory.createMetadata();
        SoapMessage msg = soapUtil.createMessage();
        soapUtil.setBody(metadata, msg);
        return msg;
    }

    private MetadataSection createThisModel(ThisModelType modelType) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_THIS_MODEL);
        metadataSection.setAny(modelType);
        return metadataSection;
    }

    private MetadataSection createThisDevice(ThisDeviceType deviceType) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_THIS_DEVICE);
        metadataSection.setAny(deviceType);
        return metadataSection;
    }

    private DiscoveredDevice createDiscoveredDevice(URI deviceUuid, List<String> xAddrs, long version) {
        return new DiscoveredDevice(
                deviceUuid,
                mock(List.class),
                mock(List.class),
                xAddrs,
                version);
    }

    private HostingServiceProxy createHostingServiceProxy(URI deviceUuid, long version) {
        return hostingServiceFactory.createHostingServiceProxy(
                deviceUuid,
                mock(List.class),
                null,
                null,
                mock(Map.class),
                version,
                mock(RequestResponseClient.class),
                mock(URI.class));
    }

    class MockTransferGetClient implements TransferGetClient {
        private Stack<SoapMessage> transferGetMessages = new Stack<>();

        public void setTransferGetMessages(Collection<SoapMessage> transferGetMessages) {
            this.transferGetMessages.addAll(transferGetMessages);
        }

        @Override
        public ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo) {
            try {
                return Futures.immediateFuture(transferGetMessages.pop());
            } catch (EmptyStackException e) {
                throw new RuntimeException("TransferGet message stack empty");
            }
        }
    }

    class MockGetMetadataClient implements GetMetadataClient {
        private Stack<SoapMessage> getMetadataMessages = new Stack<>();

        public void setGetMetadataMessages(Collection<SoapMessage> getMetadataMessages) {
            this.getMetadataMessages.addAll(getMetadataMessages);
        }

        @Override
        public ListenableFuture<SoapMessage> sendGetMetadata(RequestResponseClient requestResponseClient) {
            try {
                return Futures.immediateFuture(getMetadataMessages.pop());
            } catch (EmptyStackException e) {
                throw new RuntimeException("GetMetadata message stack empty");
            }
        }
    }
}