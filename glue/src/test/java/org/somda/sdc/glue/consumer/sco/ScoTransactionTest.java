package org.somda.sdc.glue.consumer.sco;

import it.org.somda.glue.consumer.ReportListenerSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.consumer.sco.factory.ScoTransactionFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ScoTransactionTest {
    private static final UnitTestUtil UT = new UnitTestUtil();
    private ScoTransactionFactory scoTransactionFactory;
    private long expectedTransactionId;

    @BeforeEach
    void beforeEach() {
        scoTransactionFactory = UT.getInjector().getInstance(ScoTransactionFactory.class);
        expectedTransactionId = 128;
    }

    @Test
    void getTransactionId() {
        final ScoTransaction<SetValueResponse> scoTransaction = scoTransactionFactory.createScoTransaction(
                ScoArtifacts.createResponse(
                        expectedTransactionId,
                        InvocationState.FAIL,
                        SetValueResponse.class), null);
        assertEquals(expectedTransactionId, scoTransaction.getTransactionId());
    }

    @Test
    void getReports() {
        final ScoTransactionImpl<SetValueResponse> scoTransaction = scoTransactionFactory.createScoTransaction(
                ScoArtifacts.createResponse(
                        expectedTransactionId,
                        InvocationState.WAIT,
                        SetValueResponse.class), null);

        final List<OperationInvokedReport.ReportPart> expectedReports = Arrays.asList(
                ScoArtifacts.createReportPart(InvocationState.START),
                ScoArtifacts.createReportPart(InvocationState.FIN));

        expectedReports.forEach(reportPart -> scoTransaction.receiveIncomingReport(reportPart));

        // reports are copied, they shall not be equal
        assertNotEquals(expectedReports, scoTransaction.getReports());
        assertEquals(expectedReports.size(), scoTransaction.getReports().size());

        // however, the report payloads shall be equal
        for (int i = 0; i < expectedReports.size(); ++i) {
            assertEquals(expectedReports.get(i).getInvocationInfo().getTransactionId(),
                    scoTransaction.getReports().get(i).getInvocationInfo().getTransactionId());
            assertEquals(expectedReports.get(i).getInvocationInfo().getInvocationState(),
                    scoTransaction.getReports().get(i).getInvocationInfo().getInvocationState());
        }
    }

    @Test
    void getResponse() {
        final SetValueResponse expectedResponse = ScoArtifacts.createResponse(expectedTransactionId,
                InvocationState.WAIT, SetValueResponse.class);

        final ScoTransaction<SetValueResponse> scoTransaction = scoTransactionFactory.createScoTransaction(
                expectedResponse, null);

        // response is copied, it shall not be equal
        assertNotEquals(expectedResponse, scoTransaction.getResponse());

        assertEquals(expectedResponse.getInvocationInfo().getTransactionId(),
                scoTransaction.getResponse().getInvocationInfo().getTransactionId());
        assertEquals(expectedResponse.getInvocationInfo().getInvocationState(),
                scoTransaction.getResponse().getInvocationInfo().getInvocationState());
    }

    @Test
    void waitForFinalReport() {
        final ReportListenerSpy listener = new ReportListenerSpy();
        final List<OperationInvokedReport.ReportPart> expectedReportParts = Arrays.asList(
                ScoArtifacts.createReportPart(expectedTransactionId, InvocationState.START),
                ScoArtifacts.createReportPart(expectedTransactionId, InvocationState.FIN));

        final ScoTransactionImpl<SetValueResponse> scoTransaction = scoTransactionFactory.createScoTransaction(
                ScoArtifacts.createResponse(
                        expectedTransactionId,
                        InvocationState.WAIT,
                        SetValueResponse.class), listener);

        new Thread(() -> scoTransaction.receiveIncomingReport(expectedReportParts.get(0))).start();
        List<OperationInvokedReport.ReportPart> actualReportParts = scoTransaction.waitForFinalReport(Duration.ofSeconds(1));

        assertTrue(actualReportParts.isEmpty());

        new Thread(() -> scoTransaction.receiveIncomingReport(expectedReportParts.get(1))).start();
        actualReportParts = scoTransaction.waitForFinalReport(Duration.ofSeconds(5));

        assertEquals(expectedReportParts.size(), actualReportParts.size());
        assertTrue(listener.waitForReports(2, Duration.ofSeconds(5)));
        assertEquals(expectedReportParts.size(), listener.getReports().size());

        for (int i = 0; i < expectedReportParts.size(); ++i) {
            // result from waiting shall match
            assertEquals(expectedReportParts.get(i).getInvocationInfo().getTransactionId(),
                    actualReportParts.get(i).getInvocationInfo().getTransactionId());
            assertEquals(expectedReportParts.get(i).getInvocationInfo().getInvocationState(),
                    actualReportParts.get(i).getInvocationInfo().getInvocationState());

            // result from listening shall match
            assertEquals(expectedReportParts.get(i).getInvocationInfo().getTransactionId(),
                    listener.getReports().get(i).getInvocationInfo().getTransactionId());
            assertEquals(expectedReportParts.get(i).getInvocationInfo().getInvocationState(),
                    listener.getReports().get(i).getInvocationInfo().getInvocationState());
        }
    }

    private class Listener implements Consumer<OperationInvokedReport.ReportPart> {
        private List<OperationInvokedReport.ReportPart> reportParts = new ArrayList<>();

        @Override
        public void accept(OperationInvokedReport.ReportPart reportPart) {
            reportParts.add(reportPart);
        }

        public List<OperationInvokedReport.ReportPart> getReportParts() {
            return reportParts;
        }
    }
}