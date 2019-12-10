package org.somda.sdc.glue.consumer.sco;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetContextState;
import org.somda.sdc.biceps.model.message.SetContextStateResponse;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import org.somda.sdc.glue.consumer.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.consumer.sco.factory.ScoTransactionFactory;
import org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;

class ScoControllerTest {
    private HostingServiceProxy hostingServiceProxy;
    private OperationInvocationDispatcher dispatcher;
    private HostedServiceProxy setServiceMock;
    private HostedServiceProxy contextServiceMock;
    private ScoController scoController;

    @BeforeEach
    void beforeEach() {
        setServiceMock = mock(HostedServiceProxy.class);
        contextServiceMock = mock(HostedServiceProxy.class);

        // bind OperationInvocationDispatcherFactory to invocation dispatcher instance
        Injector injector = new UnitTestUtil().createInjectorWithOverrides(new AbstractModule() {
            @Override
            protected void configure() {
                bind(OperationInvocationDispatcherFactory.class).to(DispatcherFactory.class);
                bind(ScoTransactionFactory.class).to(TransactionFactory.class);
            }
        });

        hostingServiceProxy = injector.getInstance(HostingServiceFactory.class).createHostingServiceProxy(
                URI.create("urn:uuid:441dfbea-40e5-406e-b2c4-154d3b8430bf"),
                Collections.emptyList(),
                mock(ThisDeviceType.class),
                mock(ThisModelType.class),
                Collections.emptyMap(), // no services needed as inject in SCO controller separately
                0,
                mock(RequestResponseClient.class),
                URI.create("http://xAddr/"));

        scoController = injector.getInstance(ScoControllerFactory.class).createScoController(hostingServiceProxy,
                setServiceMock,
                contextServiceMock);
    }

    @Test
    void invokeSetContext() {
        SetContextState setContextState = new SetContextState();
        ReportConsumer reportConsumer = new ReportConsumer();
        ListenableFuture<ScoTransaction<SetContextStateResponse>> future = scoController.invoke(setContextState, reportConsumer, SetContextStateResponse.class);
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

    private static class TransactionFactory extends ScoTransactionFactory {
        static ScoTransactionImpl<?> lastCreatedTransaction = null;

        @Override
        public <T extends AbstractSetResponse> ScoTransactionImpl<T> createScoTransaction(@Assisted T response,
                                                                                          @Assisted @Nullable Consumer<OperationInvokedReport.ReportPart> reportListener) {
            final ScoTransactionImpl<T> scoTransaction = super.createScoTransaction(response, reportListener);
            lastCreatedTransaction = scoTransaction;
            return scoTransaction;
        }
    }

    private class ReportConsumer implements Consumer<OperationInvokedReport.ReportPart> {
        final List<OperationInvokedReport.ReportPart> reportParts = new ArrayList<>();

        @Override
        public void accept(OperationInvokedReport.ReportPart reportPart) {
            reportParts.add(reportPart);
        }
    }
}