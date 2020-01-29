package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.HttpServerRegistryMock;
import org.somda.sdc.dpws.LocalAddressResolverMock;
import org.somda.sdc.dpws.TransportBindingFactoryMock;
import org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.network.LocalAddressResolver;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wseventing.factory.NotificationWorkerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingFaultFactory;
import org.somda.sdc.dpws.soap.wseventing.helper.EventSourceTransportManager;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wseventing.model.SubscribeResponse;

import javax.xml.bind.JAXBElement;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class WsEventingReferenceParametersTest extends DpwsTest {

    public static final String IDENTIFIER = "covfefe";

    private static final String HOST = "mock-host";
    private static final Integer PORT = 8080;
    private static final String HOSTED_SERVICE_PATH = "/hosted-service";
    private static final String ACTION = "http://action";
    private static final Duration MAX_EXPIRES = Duration.ofHours(3);

    private EventSink wseSink;
    private NotificationSink notificationSink;

    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(new DpwsModuleReplacements());
        super.setUp();

        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();

        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        ObjectFactory dpwsFactory = getInjector().getInstance(ObjectFactory.class);
        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);
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

    @Test
    public void referenceParameterInUnsubscribe() throws Exception {

        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        ReferenceParametersType incomingRefParm;
        incomingRefParm = EventSourceInterceptorMock.refParm.get(Duration.ofSeconds(2).toSeconds(), TimeUnit.SECONDS);

        assertEquals(1, incomingRefParm.getAny().size());
        assertTrue(incomingRefParm.getAny().get(0) instanceof JAXBElement);

        JAXBElement<String> o = (JAXBElement<String>) incomingRefParm.getAny().get(0);
        assertEquals(IDENTIFIER, o.getValue());
    }

    public static class EventSourceInterceptorMock extends EventSourceInterceptor  {

        public static SettableFuture<ReferenceParametersType> refParm = SettableFuture.create();
        private final SoapUtil soapUtil;
        private org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory wseFactory;

        @Inject
        EventSourceInterceptorMock(
                @Named(WsEventingConfig.SOURCE_MAX_EXPIRES) Duration maxExpires,
                @Named(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH) String subscriptionManagerPath,
                SoapUtil soapUtil,
                WsEventingFaultFactory faultFactory,
                JaxbUtil jaxbUtil,
                WsAddressingUtil wsaUtil,
                org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory wseFactory,
                EventSourceTransportManager eventSourceTransportManager,
                SoapMessageFactory soapMessageFactory,
                EnvelopeFactory envelopeFactory,
                HttpServerRegistry httpServerRegistry,
                Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider,
                SubscriptionRegistry subscriptionRegistry,
                NotificationWorkerFactory notificationWorkerFactory,
                SubscriptionManagerFactory subscriptionManagerFactory,
                HttpUriBuilder httpUriBuilder
        ) {
            super(maxExpires, subscriptionManagerPath, soapUtil, faultFactory, jaxbUtil, wsaUtil, wseFactory, eventSourceTransportManager, soapMessageFactory, envelopeFactory, httpServerRegistry, rrServerHttpHandlerProvider, subscriptionRegistry, notificationWorkerFactory, subscriptionManagerFactory, httpUriBuilder);
            this.soapUtil = soapUtil;
            refParm = SettableFuture.create();
            this.wseFactory = wseFactory;
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIBE, direction = Direction.REQUEST)
        void processSubscribe(RequestResponseObject rrObj) throws SoapFaultException {
            super.processSubscribe(rrObj);

            ReferenceParametersType referenceParameters = new ReferenceParametersType();
            var identifier = wseFactory.createIdentifier(IDENTIFIER);

            referenceParameters.setAny(List.of(identifier));

            SubscribeResponse body = soapUtil.getBody(rrObj.getResponse(), SubscribeResponse.class)
                    .orElseThrow(() -> new RuntimeException("err"));

            body.getSubscriptionManager().setReferenceParameters(referenceParameters);

            soapUtil.setBody(body, rrObj.getResponse());
        }


        @Override
        @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_UNSUBSCRIBE, direction = Direction.REQUEST)
        void processUnsubscribe(RequestResponseObject rrObj) throws SoapFaultException {
            super.processUnsubscribe(rrObj);

            var incRefParm = rrObj.getRequest().getWsAddressingHeader().getReferenceParameters();
            incRefParm.ifPresent(referenceParametersType -> refParm.set(referenceParametersType));
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_GET_STATUS, direction = Direction.REQUEST)
        void processGetStatus(RequestResponseObject rrObj) throws SoapFaultException {
            super.processGetStatus(rrObj);
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
