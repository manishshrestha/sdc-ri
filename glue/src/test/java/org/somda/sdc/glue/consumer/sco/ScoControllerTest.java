package org.somda.sdc.glue.consumer.sco;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher;

import java.net.URI;
import java.util.Collections;

import static org.mockito.Mockito.mock;

class ScoControllerTest {
    private HostingServiceProxy hostingServiceProxy;
    private OperationInvocationDispatcher dispatcher;
    private HostedServiceProxy setServiceMock;
    private HostedServiceProxy contextServiceMock;

    @BeforeEach
    void beforeEach() {
        setServiceMock = mock(HostedServiceProxy.class);
        contextServiceMock = mock(HostedServiceProxy.class);


        Injector injector = new UnitTestUtil().createInjectorWithOverrides(new AbstractModule() {
            @Override
            protected void configure() {
                bind(OperationInvocationDispatcherFactory.class).to(DispatcherFactory.class);
            }
        });

        // bind OperationInvocationDispatcherFactory to invocation dispatcher instance



//        hostingServiceProxy = injector.getInstance(HostingServiceFactory.class).createHostingServiceProxy(
//                URI.create("urn:uuid:441dfbea-40e5-406e-b2c4-154d3b8430bf"),
//                Collections.emptyList(),
//                mock(ThisDeviceType.class),
//                mock(ThisModelType.class),
//                Collections.emptyMap(),
//                0,
//                mock(RequestResponseClient.class),
//                URI.create("http://xAddr/"));

    }

    @Test
    void invoke() {
    }

    @Test
    void invoke1() {
    }

    @Test
    void processOperationInvokedReport() {
        // as the operation invocation dispatcher is responsible of dispatching events,
        // this test is only checking for correct forwarding
    }

    private static class DispatcherFactory implements OperationInvocationDispatcherFactory {
        static final OperationInvocationDispatcher INVOCATION_DISPATCHER = mock(OperationInvocationDispatcher.class);

        @Override
        public OperationInvocationDispatcher createOperationInvocationDispatcher(HostingServiceProxy hostingServiceProxy) {
            return INVOCATION_DISPATCHER;
        }
    }
}