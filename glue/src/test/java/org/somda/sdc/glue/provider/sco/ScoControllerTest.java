package org.somda.sdc.glue.provider.sco;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ScoControllerTest {
    private static final UnitTestUtil IT = new UnitTestUtil();

    private ScoController scoController;
    private EventSourceAccess eventSourceAccessMock;
    private LocalMdibAccess mdibAccessMock;
    private Receiver receiver;

    @BeforeEach
    void beforeEach() {
        eventSourceAccessMock = mock(EventSourceAccess.class);
        mdibAccessMock = mock(LocalMdibAccess.class);
        receiver = new Receiver();
        scoController = IT.getInjector().getInstance(ScoControllerFactory.class).createScoController(eventSourceAccessMock, mdibAccessMock);
        scoController.addOperationInvocationReceiver(receiver);
    }

    @Test
    void testHandleSpecificReceiver() {
        final InstanceIdentifier expectedInstanceIdentifier = new InstanceIdentifier();
        scoController.processIncomingSetOperation(Handles.OPERATION_0, expectedInstanceIdentifier, "Test");

        assertEquals(1, receiver.getItems().size());
        assertEquals(0, receiver.getItems().get(0).getContext().getTransactionId());
        assertEquals(expectedInstanceIdentifier, receiver.getItems().get(0).getContext().getInvocationSource());
        assertEquals(Handles.OPERATION_0, receiver.getItems().get(0).getContext().getOperationHandle());

        scoController.processIncomingSetOperation(Handles.OPERATION_0, expectedInstanceIdentifier, new UnknownPayload());
        assertEquals(1, receiver.getItems().size());

        // second invocation
        scoController.processIncomingSetOperation(Handles.OPERATION_0, expectedInstanceIdentifier, "Test");
        assertEquals(2, receiver.getItems().size());
        assertEquals(2, receiver.getItems().get(1).getContext().getTransactionId());
    }

    @Test
    void testDefaultReceiver() {
        final InstanceIdentifier expectedInstanceIdentifier = new InstanceIdentifier();
        scoController.processIncomingSetOperation("any-handle", expectedInstanceIdentifier, "Test");

        assertEquals(1, receiver.getItems().size());
        assertEquals(0, receiver.getItems().get(0).getContext().getTransactionId());
        assertEquals(expectedInstanceIdentifier, receiver.getItems().get(0).getContext().getInvocationSource());

        scoController.processIncomingSetOperation("any-handle", expectedInstanceIdentifier, new UnknownPayload());
        assertEquals(1, receiver.getItems().size());
    }

    @Test
    void testSuccessfulInvocation() {
        final InstanceIdentifier expectedInstanceIdentifier = new InstanceIdentifier();
        assertEquals(InvocationState.FIN,
                scoController.processIncomingSetOperation(Handles.OPERATION_0, expectedInstanceIdentifier,
                        "Test").getInvocationState());
    }

    @Test
    void testUnsuccessfulInvocation() {
        final InstanceIdentifier expectedInstanceIdentifier = new InstanceIdentifier();
        assertEquals(InvocationState.FAIL,
                scoController.processIncomingSetOperation(Handles.OPERATION_1, expectedInstanceIdentifier,
                        Arrays.asList(new LocationContextState())).getInvocationState());
    }

    @Test
    void testMultipleReceivers() {
        SecondReceiver secondReceiver = new SecondReceiver();

        scoController.addOperationInvocationReceiver(secondReceiver);

        final InstanceIdentifier expectedInstanceIdentifier = new InstanceIdentifier();
        scoController.processIncomingSetOperation("any-handle", expectedInstanceIdentifier, "Test");
        scoController.processIncomingSetOperation("any-handle", expectedInstanceIdentifier, Arrays.asList(new NumericMetricState()));
        scoController.processIncomingSetOperation("any-handle", expectedInstanceIdentifier, Arrays.asList(new ClockState()));

        assertEquals(2, receiver.getItems().size());
        assertEquals(0, receiver.getItems().get(0).getContext().getTransactionId());
        assertEquals(String.class, receiver.getItems().get(0).getData().getClass());

        assertEquals(1, receiver.getItems().get(1).getContext().getTransactionId());
        assertTrue(receiver.getItems().get(1).getData() instanceof List);
        assertEquals(1, ((List) receiver.getItems().get(1).getData()).size());
        assertEquals(NumericMetricState.class, ((List) receiver.getItems().get(1).getData()).get(0).getClass());

        assertEquals(1, secondReceiver.getItems().size());

        assertEquals(2, secondReceiver.getItems().get(0).getContext().getTransactionId());
        assertTrue(secondReceiver.getItems().get(0).getData() instanceof List);
        assertEquals(1, ((List) secondReceiver.getItems().get(0).getData()).size());
        assertEquals(ClockState.class, ((List) secondReceiver.getItems().get(0).getData()).get(0).getClass());
    }

    private class Receiver implements OperationInvocationReceiver {
        private final List<Item> items = new ArrayList<>();

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0)
        InvocationResponse setString(Context context, String data) {
            items.add(new Item(context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_1, listType = AbstractContextState.class)
        InvocationResponse setContext(Context context, List<AbstractContextState> data) {
            items.add(new Item(context, data));
            return context.createUnsucessfulResponse(MdibVersion.create(), InvocationState.FAIL, InvocationError.UNSPEC, Collections.EMPTY_LIST);
        }

        @IncomingSetServiceRequest(listType = AbstractMetricState.class)
        InvocationResponse setMetricAll(Context context, List<AbstractMetricState> data) {
            items.add(new Item(context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest
        InvocationResponse setStringAll(Context context, String data) {
            items.add(new Item(context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        public List<Item> getItems() {
            return items;
        }
    }

    private class SecondReceiver implements OperationInvocationReceiver {
        private final List<Item> items = new ArrayList<>();

        @IncomingSetServiceRequest(listType = AbstractDeviceComponentState.class)
        InvocationResponse setComponentAll(Context context, List<AbstractDeviceComponentState> data) {
            items.add(new Item(context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        public List<Item> getItems() {
            return items;
        }
    }

    private class Item {
        private final Context context;
        private final Object data;

        public Item(Context context, Object data) {
            this.context = context;
            this.data = data;
        }

        public Context getContext() {
            return context;
        }

        public Object getData() {
            return data;
        }
    }

    private class UnknownPayload {
    }
}