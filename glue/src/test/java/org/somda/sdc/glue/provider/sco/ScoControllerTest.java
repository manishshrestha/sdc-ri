package org.somda.sdc.glue.provider.sco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class ScoControllerTest {
    private static final UnitTestUtil IT = new UnitTestUtil();

    private ScoController scoController;
    private EventSourceAccess eventSourceAccessMock;
    private Receiver receiver;
    private ArgumentCaptor<String> actionCaptor;
    private ArgumentCaptor<OperationInvokedReport> reportCaptor;

    @BeforeEach
    void beforeEach() {
        eventSourceAccessMock = mock(EventSourceAccess.class);
        actionCaptor = ArgumentCaptor.forClass(String.class);
        reportCaptor = ArgumentCaptor.forClass(OperationInvokedReport.class);

        LocalMdibAccess mdibAccessMock = mock(LocalMdibAccess.class);
        when(mdibAccessMock.getMdibVersion()).thenReturn(new MdibVersion("ABC"));
        receiver = new Receiver();
        scoController = IT.getInjector().getInstance(ScoControllerFactory.class).createScoController(eventSourceAccessMock, mdibAccessMock);
        scoController.addOperationInvocationReceiver(receiver);
    }

    @Test
    void testHandleSpecificReceiver() {
        final var instanceIdentifier = new InstanceIdentifier();

        var operation = Handles.OPERATION_0;
        var callbackId = Receiver.ID_SET_STRING_SPECIFIC;
        var itemSize = 1;
        var itemIndex = 0;
        var transactionId = 0;
        {
            final var expectedPayload = "Test";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);
            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(receiver.getItems().get(itemIndex).getData(), instanceOf(String.class));
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());

        }

        operation = Handles.OPERATION_1;
        callbackId = Receiver.ID_ACTIVATE_SPECIFIC;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var contextState1 = new PatientContextState();
            contextState1.setHandle("TestHandle1");
            final var contextState2 = new LocationContextState();
            contextState2.setHandle("TestHandle2");
            final var expectedPayload = Arrays.asList(contextState1, contextState2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(receiver.getItems().get(itemIndex).getData(), instanceOf(List.class));
            final var list = (List<?>) receiver.getItems().get(itemIndex).getData();
            assertEquals(2, list.size());

            assertThat(list.get(0), instanceOf(PatientContextState.class));
            assertEquals(contextState1.getHandle(), ((AbstractContextState) list.get(0)).getHandle());
            assertThat(list.get(1), instanceOf(LocationContextState.class));
            assertEquals(contextState2.getHandle(), ((AbstractContextState) list.get(1)).getHandle());
        }

        operation = Handles.OPERATION_2;
        callbackId = Receiver.ID_SET_CONTEXT_SPECIFIC;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var arg1 = "Test";
            final var arg2 = Integer.valueOf(100);
            final var expectedPayload = Arrays.asList(arg1, arg2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(receiver.getItems().get(itemIndex).getData(), instanceOf(List.class));
            final var list = (List<?>) receiver.getItems().get(itemIndex).getData();
            assertEquals(2, list.size());

            assertThat(list.get(0), instanceOf(String.class));
            assertEquals(arg1, list.get(0));
            assertThat(list.get(1), instanceOf(Integer.class));
            assertEquals(arg2, list.get(1));
        }
    }

    @Test
    void testHandleDefaultReceiver() {
        final var instanceIdentifier = new InstanceIdentifier();

        final var operation = "<any handle>";
        var callbackId = Receiver.ID_SET_STRING_ALL;
        var itemSize = 1;
        var itemIndex = 0;
        var transactionId = 0;
        {
            final var expectedPayload = "Test";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);
            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(receiver.getItems().get(itemIndex).getData(), instanceOf(String.class));
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());

        }

        callbackId = Receiver.ID_LIST_ALL;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var contextState1 = new PatientContextState();
            contextState1.setHandle("TestHandle1");
            final var contextState2 = new LocationContextState();
            contextState2.setHandle("TestHandle2");
            final var expectedPayload = Arrays.asList(contextState1, contextState2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(receiver.getItems().get(itemIndex).getData(), instanceOf(List.class));
            final var list = (List<?>) receiver.getItems().get(itemIndex).getData();
            assertEquals(2, list.size());

            assertThat(list.get(0), instanceOf(PatientContextState.class));
            assertEquals(contextState1.getHandle(), ((AbstractContextState) list.get(0)).getHandle());
            assertThat(list.get(1), instanceOf(LocationContextState.class));
            assertEquals(contextState2.getHandle(), ((AbstractContextState) list.get(1)).getHandle());
        }

        // callbackId = Receiver.ID_LIST_ALL;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var arg1 = "Test";
            final var arg2 = Integer.valueOf(100);
            final var expectedPayload = Arrays.asList(arg1, arg2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(receiver.getItems().get(itemIndex).getData(), instanceOf(List.class));
            final var list = (List<?>) receiver.getItems().get(itemIndex).getData();
            assertEquals(2, list.size());

            assertThat(list.get(0), instanceOf(String.class));
            assertEquals(arg1, list.get(0));
            assertThat(list.get(1), instanceOf(Integer.class));
            assertEquals(arg2, list.get(1));
        }
    }

    @Test
    void testHandleMultipleReceivers() {
        SecondReceiver secondReceiver = new SecondReceiver();

        scoController.addOperationInvocationReceiver(secondReceiver);

        final var instanceIdentifier = new InstanceIdentifier();

        var operation = Handles.OPERATION_0;
        var callbackId = Receiver.ID_SET_STRING_SPECIFIC;
        var itemSize = 1;
        var itemIndex = 0;
        var transactionId = 0;
        {
            final var expectedPayload = "Test";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);
            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(receiver.getItems().get(itemIndex).getData(), instanceOf(String.class));
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
        }

        operation = Handles.OPERATION_3;
        callbackId = SecondReceiver.ID_SET_STRING_SPECIFIC;
        // itemSize = 1;
        // itemIndex = 0;
        transactionId++;
        {
            final var expectedPayload = "Test";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);
            assertEquals(itemSize, secondReceiver.getItems().size());
            assertEquals(callbackId, secondReceiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, secondReceiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, secondReceiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, secondReceiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertThat(secondReceiver.getItems().get(itemIndex).getData(), instanceOf(String.class));
            assertEquals(expectedPayload, secondReceiver.getItems().get(itemIndex).getData());

        }
    }

    @Test
    void testInvocationForUnknownReceiver() {
        final var instanceIdentifier = new InstanceIdentifier();
        final var operation = "<any handle>";
        final var expectedPayload = Integer.valueOf(100);
        final var response = scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload);
        assertEquals(InvocationState.FAIL, response.getInvocationState());
    }

    @Test
    void testImplicitOperationInvokedReportOnErrorGeneratedInScoController() throws Exception {
        final var expectedInstanceIdentifier = new InstanceIdentifier();
        final var expectedInvocationState = InvocationState.FAIL;
        final var operationHandle = "<unknown operation handle>";

        // pass a big decimal to make sure no receiver will be found
        assertEquals(expectedInvocationState,
                scoController.processIncomingSetOperation(operationHandle, expectedInstanceIdentifier,
                        BigDecimal.ZERO).getInvocationState());

        // make sure the error result is also sent as report
        verify(eventSourceAccessMock).sendNotification(actionCaptor.capture(), reportCaptor.capture());

        assertEquals(ActionConstants.ACTION_OPERATION_INVOKED_REPORT, actionCaptor.getValue());
        assertEquals(expectedInvocationState, reportCaptor.getValue().getReportPart().get(0).getInvocationInfo().getInvocationState());
        assertEquals(operationHandle, reportCaptor.getValue().getReportPart().get(0).getOperationHandleRef());
    }

    private static class Receiver implements OperationInvocationReceiver {
        private final List<Item> items = new ArrayList<>();

        public final static String ID_SET_STRING_SPECIFIC = "setStringForSpecificHandle";
        public final static String ID_ACTIVATE_SPECIFIC = "activateForSpecificHandle";
        public final static String ID_SET_CONTEXT_SPECIFIC = "setContextForSpecificHandle";
        public final static String ID_SET_STRING_ALL = "setStringCatchAll";
        public final static String ID_LIST_ALL = "listCatchAll";

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0)
        InvocationResponse setStringForSpecificHandle(Context context, String data) {
            items.add(new Item(ID_SET_STRING_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_1)
        InvocationResponse activateForSpecificHandle(Context context, List<Object> data) {
            items.add(new Item(ID_ACTIVATE_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_2)
        InvocationResponse setContextForSpecificHandle(Context context, List<AbstractContextState> data) {
            items.add(new Item(ID_SET_CONTEXT_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest
        InvocationResponse setStringCatchAll(Context context, String data) {
            items.add(new Item(ID_SET_STRING_ALL, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest
        InvocationResponse listCatchAll(Context context, List<Object> data) {
            items.add(new Item(ID_LIST_ALL, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        public List<Item> getItems() {
            return items;
        }
    }

    private static class SecondReceiver implements OperationInvocationReceiver {
        private final List<Item> items = new ArrayList<>();
        public final static String ID_SET_STRING_SPECIFIC = "secondSetStringForSpecificHandle";

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_3)
        InvocationResponse setStringForSpecificHandle(Context context, String data) {
            items.add(new Item(ID_SET_STRING_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        public List<Item> getItems() {
            return items;
        }
    }

    private static class Item {
        private final String callbackId;
        private final Context context;
        private final Object data;

        public Item(String callbackId, Context context, Object data) {
            this.callbackId = callbackId;
            this.context = context;
            this.data = data;
        }

        public String getCallbackId() {
            return callbackId;
        }

        public Context getContext() {
            return context;
        }

        public Object getData() {
            return data;
        }
    }
}