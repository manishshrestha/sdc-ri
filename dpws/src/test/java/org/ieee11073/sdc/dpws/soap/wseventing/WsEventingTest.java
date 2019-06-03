package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.HttpServerRegistryMock;
import org.ieee11073.sdc.dpws.LocalAddressResolverMock;
import org.ieee11073.sdc.dpws.TransportBindingFactoryMock;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.model.ObjectFactory;
import org.ieee11073.sdc.dpws.ni.LocalAddressResolver;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.soap.*;
import org.ieee11073.sdc.dpws.soap.*;
import org.ieee11073.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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

    @Before
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

        MarshallingService marshallingService = getInjector().getInstance(MarshallingService.class);
        URI hostedServiceUri = httpSrvRegisty.registerContext(HOST, PORT, HOSTED_SERVICE_PATH, (inStream, outStream, ti) ->
                marshallingService.handleRequestResponse(reqResSrv, inStream, outStream, ti));

        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.getEndpointReference().add(wsaUtil.createEprWithAddress(hostedServiceUri));

        RequestResponseClientFactory rrcFactory = getInjector().getInstance(RequestResponseClientFactory.class);
        TransportBindingFactory tbFactory = getInjector().getInstance(TransportBindingFactory.class);
        RequestResponseClient rrc = rrcFactory.createRequestResponseClient(
                tbFactory.createTransportBinding(hostedServiceUri));

        HostedServiceProxy mockHostedServiceProxy = hostedServiceFactory.createHostedServiceProxy(hst, rrc,
                hostedServiceUri);

        wseSink = getInjector().getInstance(WsEventingEventSinkFactory.class)
                .createWsEventingEventSink(rrc, "localhost");
    }

    @Test
    public void subscribe() throws Exception {
        Duration expectedExpires = Duration.ofHours(1);

        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Subscription ID length", resInfo.get().getSubscriptionId().length(), greaterThan(0));
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        Duration tryExpires = MAX_EXPIRES.plusHours(1);
        resInfo = wseSink.subscribe(Collections.singletonList(ACTION), tryExpires, notificationSink);
        assertThat("Second subscription ID length", resInfo.get().getSubscriptionId().length(), greaterThan(0));
        assertThat("Seconds granted expires duration", resInfo.get().getGrantedExpires(), is(MAX_EXPIRES));
    }

    @Test
    public void renew() throws Exception {
        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        expectedExpires = Duration.ofHours(2);
        ListenableFuture<Duration> actualExpires = wseSink.renew(resInfo.get().getSubscriptionId(), expectedExpires);
        assertThat("Renew granted expires duration", actualExpires.get(), is(expectedExpires));
    }

    @Test
    public void getStatus() throws Exception {
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
    public void unsubscribe() throws Exception {
        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        try {
            wseSink.getStatus(resInfo.get().getSubscriptionId()).get();
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void disabledAutoRenew() throws Exception {
        Duration expectedExpires = Duration.ofSeconds(2);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        Thread.sleep(2000);

        try {
            wseSink.getStatus(resInfo.get().getSubscriptionId()).get();
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void enabledAutoRenew() throws Exception {
        Duration expectedExpires = Duration.ofSeconds(2);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(Collections.singletonList(ACTION),
                expectedExpires, notificationSink);
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.enableAutoRenew(resInfo.get().getSubscriptionId());
        Thread.sleep(2000);

        try {
            wseSink.getStatus(resInfo.get().getSubscriptionId());
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
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