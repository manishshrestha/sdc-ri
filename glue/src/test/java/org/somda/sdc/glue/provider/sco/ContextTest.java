package org.somda.sdc.glue.provider.sco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.provider.sco.factory.ContextFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContextTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private Context context;
    private EventSourceAccess eventSourceAccess;
    private ArgumentCaptor<String> actionCaptor;
    private ArgumentCaptor<OperationInvokedReport> reportCaptor;

    @BeforeEach
    void beforeEach() {
        eventSourceAccess = mock(EventSourceAccess.class);
        actionCaptor = ArgumentCaptor.forClass(String.class);
        reportCaptor = ArgumentCaptor.forClass(OperationInvokedReport.class);

        context = UT.getInjector().getInstance(ContextFactory.class).createContext(0, "handle",
                new InstanceIdentifier(), eventSourceAccess, mock(LocalMdibAccess.class));
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
    void sendUnsuccessfulReport() throws MarshallingException, TransportException {
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        final InvocationState expectedInvocationState = InvocationState.FIN_MOD;
        final InvocationError expectedInvocationError = InvocationError.INV;
        final List<LocalizedText> expectedErrorMessage = new ArrayList<>();
        context.sendUnsuccessfulReport(expectedMdibVersion, expectedInvocationState, expectedInvocationError, expectedErrorMessage);

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