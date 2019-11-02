package org.ieee11073.sdc.glue.provider.sco;

import org.ieee11073.sdc.biceps.model.message.InvocationError;
import org.ieee11073.sdc.biceps.model.message.InvocationState;
import org.ieee11073.sdc.biceps.model.message.OperationInvokedReport;
import org.ieee11073.sdc.biceps.model.participant.InstanceIdentifier;
import org.ieee11073.sdc.biceps.model.participant.LocalizedText;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.ieee11073.sdc.dpws.device.EventSourceAccess;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.glue.UnitTestUtil;
import org.ieee11073.sdc.glue.common.ActionConstants;
import org.ieee11073.sdc.glue.provider.sco.factory.ContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContextTest {
    private static final UnitTestUtil IT = new UnitTestUtil();

    private Context context;
    private EventSourceAccess eventSourceAccess;
    private ArgumentCaptor<String> actionCaptor;
    private ArgumentCaptor<OperationInvokedReport> reportCaptor;

    @BeforeEach
    void beforeEach() {
        eventSourceAccess = mock(EventSourceAccess.class);
        actionCaptor = ArgumentCaptor.forClass(String.class);
        reportCaptor = ArgumentCaptor.forClass(OperationInvokedReport.class);

        context = IT.getInjector().getInstance(ContextFactory.class).createContext(0, "handle",
                new InstanceIdentifier(), eventSourceAccess);
    }

    @Test
    void sendSuccessfulReport() throws MarshallingException, TransportException {
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        final InvocationState expectedInvocationState = InvocationState.FIN_MOD;
        final String expectedTarget = "target";
        context.sendSuccessfulReport(expectedMdibVersion, expectedInvocationState, expectedTarget);

        verify(eventSourceAccess).sendNotification(actionCaptor.capture(), reportCaptor.capture());

        assertEquals(ActionConstants.ACTION_OPERATION_INVOKED_REPORT, actionCaptor.getValue());
        assertEquals(expectedMdibVersion.getVersion(), reportCaptor.getValue().getMdibVersion());
        assertEquals(expectedMdibVersion.getSequenceId().toString(), reportCaptor.getValue().getSequenceId());
        assertEquals(expectedMdibVersion.getInstanceId(), reportCaptor.getValue().getInstanceId());
        assertEquals(expectedInvocationState, reportCaptor.getValue().getReportPart().get(0).getInvocationInfo().getInvocationState());
        assertEquals(expectedTarget, reportCaptor.getValue().getReportPart().get(0).getOperationTarget());
    }

    @Test
    void sendUnsucessfulReport() throws MarshallingException, TransportException {
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        final InvocationState expectedInvocationState = InvocationState.FIN_MOD;
        final InvocationError expectedInvocationError = InvocationError.INV;
        final List<LocalizedText> expectedErrorMessage = new ArrayList<>();
        context.sendUnsucessfulReport(expectedMdibVersion, expectedInvocationState, expectedInvocationError, expectedErrorMessage);

        verify(eventSourceAccess).sendNotification(actionCaptor.capture(), reportCaptor.capture());

        assertEquals(ActionConstants.ACTION_OPERATION_INVOKED_REPORT, actionCaptor.getValue());
        assertEquals(expectedMdibVersion.getVersion(), reportCaptor.getValue().getMdibVersion());
        assertEquals(expectedMdibVersion.getSequenceId().toString(), reportCaptor.getValue().getSequenceId());
        assertEquals(expectedMdibVersion.getInstanceId(), reportCaptor.getValue().getInstanceId());
        assertEquals(expectedInvocationState, reportCaptor.getValue().getReportPart().get(0).getInvocationInfo().getInvocationState());
        assertEquals(expectedInvocationError, reportCaptor.getValue().getReportPart().get(0).getInvocationInfo().getInvocationError());
        assertEquals(expectedErrorMessage, reportCaptor.getValue().getReportPart().get(0).getInvocationInfo().getInvocationErrorMessage());
    }
}