package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.HttpServerRegistryMock;
import org.somda.sdc.dpws.LocalAddressResolverMock;
import org.somda.sdc.dpws.TransportBindingFactoryMock;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.network.LocalAddressResolver;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.factory.NotificationSinkFactory;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Round trip test for WS-Eventing (Source+Sink).
 */
public class WsEventingTest extends DpwsTest {
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

        // start required thread pool(s)
        getInjector().getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                },
                NetworkJobThreadPool.class
        )).startAsync().awaitRunning();

        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();

        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        ObjectFactory dpwsFactory = getInjector().getInstance(ObjectFactory.class);
        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);
        EventSource wseSource = getInjector().getInstance(EventSource.class);
        RequestResponseServer reqResSrv = getInjector().getInstance(RequestResponseServer.class);
        reqResSrv.register(wseSource);
        notificationSink = getInjector().getInstance(NotificationSinkFactory.class).createNotificationSink(
                getInjector().getInstance(WsAddressingServerInterceptor.class));

        HttpServerRegistry httpSrvRegisty = getInjector().getInstance(HttpServerRegistry.class);

        var uri = "http://" + HOST + ":" + PORT;
        var hostedServiceUri = httpSrvRegisty.registerContext(uri, HOSTED_SERVICE_PATH, new HttpHandler() {
            @Override
            public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) throws HttpException {
                MarshallingHelper.handleRequestResponse(getInjector(), reqResSrv, inStream, outStream, communicationContext);
            }
        });

        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.getEndpointReference().add(wsaUtil.createEprWithAddress(hostedServiceUri));

        RequestResponseClientFactory rrcFactory = getInjector().getInstance(RequestResponseClientFactory.class);
        TransportBindingFactory tbFactory = getInjector().getInstance(TransportBindingFactory.class);
        RequestResponseClient rrc = rrcFactory.createRequestResponseClient(
                tbFactory.createTransportBinding(hostedServiceUri));

        wseSink = getInjector().getInstance(WsEventingEventSinkFactory.class)
                .createWsEventingEventSink(rrc, "http://localhost:1234");
    }

    @Test
    void subscribe() throws Exception {
        Duration expectedExpires = MAX_EXPIRES;

        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Subscription ID length", resInfo.get().getSubscriptionId().length(), greaterThan(0));
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        Duration tryExpires = MAX_EXPIRES.plusHours(1);
        resInfo = wseSink.subscribe(Collections.singletonList(ACTION), tryExpires, notificationSink);
        assertThat("Second subscription ID length", resInfo.get().getSubscriptionId().length(), greaterThan(0));
        assertThat("Seconds granted expires duration", resInfo.get().getGrantedExpires(), is(MAX_EXPIRES));

        resInfo = wseSink.subscribe(Collections.singletonList(ACTION), null, notificationSink);
        assertThat("Second subscription ID length", resInfo.get().getSubscriptionId().length(), greaterThan(0));
        assertThat("Seconds granted expires duration", resInfo.get().getGrantedExpires(), is(MAX_EXPIRES));
    }

    @Test
    void renew() throws Exception {
        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        expectedExpires = Duration.ofHours(2);
        ListenableFuture<Duration> actualExpires = wseSink.renew(resInfo.get().getSubscriptionId(), expectedExpires);
        assertThat("Renew granted expires duration", actualExpires.get(), is(expectedExpires));
    }

    @Test
    void getStatus() throws Exception {
        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        Thread.sleep(1000);

        ListenableFuture<Duration> actualExpires = wseSink.getStatus(resInfo.get().getSubscriptionId());
        assertThat("GetStatus retrieved expires duration", actualExpires.get(), lessThan(expectedExpires));
        assertThat("GetStatus retrieved expires duration", actualExpires.get(), greaterThan(Duration.ZERO));
    }

    @Test
    void unsubscribe() throws Exception {
        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        try {
            wseSink.getStatus(resInfo.get().getSubscriptionId()).get();
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    private class DpwsModuleReplacements extends AbstractModule {
        @Override
        protected void configure() {
            TransportBindingFactoryMock.setHandlerRegistry(HttpServerRegistryMock.getRegistry());
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