package org.somda.sdc.glue.consumer.sco.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.consumer.sco.ScoArtifacts;
import org.somda.sdc.glue.consumer.sco.ScoTransactionImpl;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(LoggingTestWatcher.class)
class OperationInvocationDispatcherTest {
    private static final UnitTestUtil UT = new UnitTestUtil();
    private HostingServiceProxy hostingServiceProxy;
    private OperationInvocationDispatcher dispatcher;

    @BeforeEach
    void beforeEach() {
        hostingServiceProxy = UT.getInjector().getInstance(HostingServiceFactory.class).createHostingServiceProxy(
                "urn:uuid:441dfbea-40e5-406e-b2c4-154d3b8430bf",
                Collections.emptyList(),
                ThisDeviceType.builder().build(),
                ThisModelType.builder().build(),
                Collections.emptyMap(),
                0,
                mock(RequestResponseClient.class),
                "http://xAddr/");

        dispatcher = UT.getInjector()
                .getInstance(OperationInvocationDispatcherFactory.class)
                .createOperationInvocationDispatcher(hostingServiceProxy);
    }

    @Test
    void dispatchReportWithoutMatchingTransaction() {
        ScoTransactionImpl<SetValueResponse> scoTransaction = (ScoTransactionImpl<SetValueResponse>) mock(ScoTransactionImpl.class);
        long expectedTransactionId = 5;
        OperationInvokedReport report = ScoArtifacts.createReport(expectedTransactionId, InvocationState.CNCLLD);

        dispatcher.registerTransaction(scoTransaction);
        dispatcher.dispatchReport(report);

        verify(scoTransaction, times(0))
                .receiveIncomingReport(any(OperationInvokedReport.ReportPart.class));
        verify(scoTransaction, atLeast(1)).getTransactionId();
    }

    @Test
    void dispatchReportWithMatchingTransaction() {
        ScoTransactionImpl<SetValueResponse> scoTransaction = (ScoTransactionImpl<SetValueResponse>) mock(ScoTransactionImpl.class);
        long expectedTransactionId = 5;
        when(scoTransaction.getTransactionId()).thenReturn(expectedTransactionId);

        OperationInvokedReport report = ScoArtifacts.createReport(expectedTransactionId, InvocationState.CNCLLD);

        dispatcher.registerTransaction(scoTransaction);
        dispatcher.dispatchReport(report);

        verify(scoTransaction, times(1))
                .receiveIncomingReport(report.getReportPart().get(0));
        verify(scoTransaction, atLeast(1)).getTransactionId();
    }

    @Test
    void dispatchReportWithMultipleReportParts() {
        final List<ScoTransactionImpl<? extends AbstractSetResponse>> transactions = new ArrayList<>();
        long maxReports = 5;
        var reportBuilder = OperationInvokedReport.builder();
        for (long i = 0; i < maxReports; ++i) {
            ScoTransactionImpl<SetValueResponse> transaction = (ScoTransactionImpl<SetValueResponse>) mock(ScoTransactionImpl.class);
            when(transaction.getTransactionId()).thenReturn(i);
            transactions.add(transaction);
            reportBuilder.addReportPart(ScoArtifacts.createReportPart(i, InvocationState.FIN));
            dispatcher.registerTransaction(transaction);
        }

        var report = reportBuilder.build();

        dispatcher.dispatchReport(report);

        for (int i = 0; i < maxReports; ++i) {
            ScoTransactionImpl<? extends AbstractSetResponse> scoTransaction = transactions.get(i);
            verify(scoTransaction, times(1))
                    .receiveIncomingReport(report.getReportPart().get(i));
            verify(scoTransaction, atLeast(1)).getTransactionId();
        }
    }
}