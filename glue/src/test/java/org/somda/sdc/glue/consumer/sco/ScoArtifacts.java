package org.somda.sdc.glue.consumer.sco;

import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;

import java.util.Collection;
import java.util.Map;

public class ScoArtifacts {
    public static OperationInvokedReport createReport(InvocationState invocationState) {
        return createReport(0, invocationState);
    }

    public static OperationInvokedReport createReport(Collection<Map.Entry<Long, InvocationState>> parts) {
        OperationInvokedReport report = new OperationInvokedReport();

        for (Map.Entry<Long, InvocationState> part : parts) {
            report.getReportPart().add(createReportPart(part.getKey(), part.getValue()));
        }

        return report;
    }

    public static OperationInvokedReport.ReportPart createReportPart(InvocationState invocationState) {
        return createReportPart(0, invocationState);
    }

    public static OperationInvokedReport createReport(long transactionId, InvocationState invocationState) {
        return OperationInvokedReport.builder()
            .addReportPart(createReportPart(transactionId, invocationState))
            .build();
    }

    public static OperationInvokedReport.ReportPart createReportPart(long transactionId, InvocationState invocationState) {
        var invocationInfo = InvocationInfo.builder()
            .withInvocationState(invocationState)
            .withTransactionId(transactionId);

        var reportPart = OperationInvokedReport.ReportPart.builder();
        reportPart.withInvocationInfo(invocationInfo.build());
        return reportPart.build();
    }

    public static <T extends AbstractSetResponse.Builder<?>> T createResponse(long transactionId,
                                                                   InvocationState invocationState,
                                                                   T responseClassBuilder) {
        try {
            var invocationInfo = InvocationInfo.builder()
                .withInvocationState(invocationState)
                .withTransactionId(transactionId);

            return (T) responseClassBuilder.withInvocationInfo(invocationInfo.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
