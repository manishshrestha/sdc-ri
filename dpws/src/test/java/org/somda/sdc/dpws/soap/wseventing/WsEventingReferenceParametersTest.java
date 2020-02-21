package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.HttpServerRegistryMock;
import org.somda.sdc.dpws.LocalAddressResolverMock;
import org.somda.sdc.dpws.TransportBindingFactoryMock;
import org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.helper.ExecutorWrapperService;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.network.LocalAddressResolver;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingFaultFactory;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wseventing.model.SubscribeResponse;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class WsEventingReferenceParametersTest extends DpwsTest {

    private static final String HOST = "mock-host";
    private static final Integer PORT = 8080;
    private static final String HOSTED_SERVICE_PATH = "/hosted-service";
    private static final String ACTION = "http://action";
    private static final Duration MAX_EXPIRES = Duration.ofHours(3);
    private static final Duration FUTURE_WAIT = Duration.ofSeconds(1);

    private static final String REFERENCE = "my_secret_is_cofveve";


    private EventSink wseSink;
    private NotificationSink notificationSink;

    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(List.of(new DpwsModuleReplacements()));
        super.setUp();

        // start required thread pool(s)
        getInjector().getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>(){},
                NetworkJobThreadPool.class
        )).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();

        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        ObjectFactory dpwsFactory = getInjector().getInstance(ObjectFactory.class);
        EventSource wseSource = getInjector().getInstance(EventSource.class);
        RequestResponseServer reqResSrv = getInjector().getInstance(RequestResponseServer.class);
        reqResSrv.register(wseSource);
        notificationSink = getInjector().getInstance(NotificationSink.class);

        HttpServerRegistry httpSrvRegisty = getInjector().getInstance(HttpServerRegistry.class);

        URI uri = URI.create("http://" + HOST + ":" + PORT);
        MarshallingService marshallingService = getInjector().getInstance(MarshallingService.class);
        URI hostedServiceUri = httpSrvRegisty.registerContext(uri, HOSTED_SERVICE_PATH, (inStream, outStream, ti) ->
                marshallingService.handleRequestResponse(reqResSrv, inStream, outStream, ti));

        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.getEndpointReference().add(wsaUtil.createEprWithAddress(hostedServiceUri));

        RequestResponseClientFactory rrcFactory = getInjector().getInstance(RequestResponseClientFactory.class);
        TransportBindingFactory tbFactory = getInjector().getInstance(TransportBindingFactory.class);
        RequestResponseClient rrc = rrcFactory.createRequestResponseClient(
                tbFactory.createTransportBinding(hostedServiceUri));

        wseSink = getInjector().getInstance(WsEventingEventSinkFactory.class)
                .createWsEventingEventSink(rrc, URI.create("http://localhost:1234"));

    }

    public void verifyReferenceParam(SettableFuture<Collection<Element>> future) throws Exception {
        Collection<Element> incomingRefParm;
        incomingRefParm = future.get(FUTURE_WAIT.toSeconds(), TimeUnit.SECONDS);

        assertEquals(1, incomingRefParm.size());

        Element element = incomingRefParm.stream().findFirst().get();

        assertEquals(1, element.getChildNodes().getLength());
        assertEquals(REFERENCE, element.getTextContent());
        assertTrue(element.hasAttributeNS(
                WsAddressingConstants.IS_REFERENCE_PARAMETER.getNamespaceURI(),
                WsAddressingConstants.IS_REFERENCE_PARAMETER.getLocalPart()
                )
        );
    }

    @Test
    public void referenceParameterInUnsubscribe() throws Exception {
        assertFalse(EventSourceInterceptorMock.unsubscribeRefParam.isDone());

        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        verifyReferenceParam(EventSourceInterceptorMock.unsubscribeRefParam);
    }

    @Test
    public void referenceParameterInGetStatus() throws Exception {
        assertFalse(EventSourceInterceptorMock.getStatusRefParam.isDone());

        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        verifyReferenceParam(EventSourceInterceptorMock.getStatusRefParam);
    }

    @Test
    public void referenceParameterInRenew() throws Exception {
        assertFalse(EventSourceInterceptorMock.renewRefParam.isDone());

        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.renew(resInfo.get().getSubscriptionId(), MAX_EXPIRES).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        verifyReferenceParam(EventSourceInterceptorMock.renewRefParam);
    }

    public static class EventSourceInterceptorMock extends EventSourceInterceptor {

        public static SettableFuture<Collection<Element>> unsubscribeRefParam = SettableFuture.create();
        public static SettableFuture<Collection<Element>> getStatusRefParam = SettableFuture.create();
        public static SettableFuture<Collection<Element>> renewRefParam = SettableFuture.create();
        private final SoapUtil soapUtil;

        @Inject
        EventSourceInterceptorMock(
                @Named(WsEventingConfig.SOURCE_MAX_EXPIRES) Duration maxExpires,
                @Named(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH) String subscriptionManagerPath,
                SoapUtil soapUtil,
                WsEventingFaultFactory faultFactory,
                JaxbUtil jaxbUtil,
                WsAddressingUtil wsaUtil,
                org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory wseFactory,
                SoapMessageFactory soapMessageFactory,
                EnvelopeFactory envelopeFactory,
                HttpServerRegistry httpServerRegistry,
                Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider,
                SubscriptionRegistry subscriptionRegistry,
                SubscriptionManagerFactory subscriptionManagerFactory,
                HttpUriBuilder httpUriBuilder
        ) {
            super(maxExpires, subscriptionManagerPath, soapUtil, faultFactory, jaxbUtil, wsaUtil, wseFactory, soapMessageFactory, envelopeFactory, httpServerRegistry, rrServerHttpHandlerProvider, subscriptionRegistry, subscriptionManagerFactory, httpUriBuilder);
            this.soapUtil = soapUtil;

            // reset the futures on every instantiation to avoid side effects
            unsubscribeRefParam = SettableFuture.create();
            getStatusRefParam = SettableFuture.create();
            renewRefParam = SettableFuture.create();
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSE_ACTION_SUBSCRIBE, direction = Direction.REQUEST)
        void processSubscribe(RequestResponseObject rrObj) throws SoapFaultException {
            super.processSubscribe(rrObj);

            ReferenceParametersType referenceParameters = new ReferenceParametersType();
            // create some random child element
            var fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = fac.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
            var doc = builder.newDocument();

            var root = doc.createElementNS("ftp://namespace.example.com", "MyFunkyRoot");
            root.setTextContent(REFERENCE);

            referenceParameters.setAny(List.of(root));

            SubscribeResponse body = soapUtil.getBody(rrObj.getResponse(), SubscribeResponse.class)
                    .orElseThrow(() -> new RuntimeException("err"));

            body.getSubscriptionManager().setReferenceParameters(referenceParameters);

            soapUtil.setBody(body, rrObj.getResponse());
        }


        @Override
        @MessageInterceptor(value = WsEventingConstants.WSE_ACTION_UNSUBSCRIBE, direction = Direction.REQUEST)
        void processUnsubscribe(RequestResponseObject rrObj) throws SoapFaultException {
            super.processUnsubscribe(rrObj);

            // find any reference parameters
            var refParm = rrObj.getRequest().getWsAddressingHeader().getMappedReferenceParameters();
            refParm.ifPresent(refParmContent -> unsubscribeRefParam.set(refParmContent));
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSE_ACTION_GET_STATUS, direction = Direction.REQUEST)
        void processGetStatus(RequestResponseObject rrObj) throws SoapFaultException {
            super.processGetStatus(rrObj);

            // find any reference parameters
            var refParm = rrObj.getRequest().getWsAddressingHeader().getMappedReferenceParameters();
            refParm.ifPresent(refParmContent -> getStatusRefParam.set(refParmContent));
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSE_ACTION_RENEW, direction = Direction.REQUEST)
        void processRenew(RequestResponseObject rrObj) throws SoapFaultException {
            super.processRenew(rrObj);

            // find any reference parameters
            var refParm = rrObj.getRequest().getWsAddressingHeader().getMappedReferenceParameters();
            refParm.ifPresent(refParmContent -> renewRefParam.set(refParmContent));
        }
    }

    private class DpwsModuleReplacements extends AbstractModule {
        @Override
        protected void configure() {
            TransportBindingFactoryMock.setHandlerRegistry(HttpServerRegistryMock.getRegistry());
            bind(EventSource.class)
                    .to(EventSourceInterceptorMock.class);
            bind(Duration.class)
                    .annotatedWith(Names.named(WsEventingConfig.SOURCE_MAX_EXPIRES))
                    .toInstance(Duration.ofHours(3));
            bind(HttpServerRegistry.class)
                    .to(HttpServerRegistryMock.class);
            bind(TransportBindingFactory.class)
                    .to(TransportBindingFactoryMock.class);
            bind(LocalAddressResolver.class).to(LocalAddressResolverMock.class);
        }
    }
}
