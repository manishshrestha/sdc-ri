package org.somda.sdc.glue.consumer.sco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import test.org.somda.common.LoggingTestWatcher;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ScoUtilTest {
    private ScoUtil scoUtil;

    @BeforeEach
    void beforeEach() {
        this.scoUtil = new ScoUtil();
    }

    @Test
    void hasFinalReport() {
        assertFalse(scoUtil.hasFinalReport(Arrays.asList(
                ScoArtifacts.createReportPart(InvocationState.WAIT),
                ScoArtifacts.createReportPart(InvocationState.START))));

        assertTrue(scoUtil.hasFinalReport(Arrays.asList(
                ScoArtifacts.createReportPart(InvocationState.WAIT),
                ScoArtifacts.createReportPart(InvocationState.FAIL),
                ScoArtifacts.createReportPart(InvocationState.START))));

        assertTrue(scoUtil.hasFinalReport(Arrays.asList(
                ScoArtifacts.createReportPart(InvocationState.WAIT),
                ScoArtifacts.createReportPart(InvocationState.START),
                ScoArtifacts.createReportPart(InvocationState.CNCLLD_MAN))));
    }

    @Test
    void getFinalReport() {
        final OperationInvokedReport.ReportPart expectedReport = ScoArtifacts.createReportPart(InvocationState.FAIL);
        final Optional<OperationInvokedReport.ReportPart> actualReport = scoUtil.getFinalReport(Arrays.asList(
                ScoArtifacts.createReportPart(InvocationState.WAIT),
                expectedReport,
                ScoArtifacts.createReportPart(InvocationState.START)));
        assertTrue(actualReport.isPresent());
        assertEquals(expectedReport, actualReport.get());
    }

    @Test
    void isFinalReport() {
        assertFalse(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.WAIT)));
        assertFalse(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.START)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.FIN)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.FAIL)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.CNCLLD_MAN)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.CNCLLD)));
    }
}