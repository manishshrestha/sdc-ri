package org.somda.sdc.glue.provider.services;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class HighPriorityServicesTest {
    private static final int MDS_0_CONTEXT_STATES_COUNT = 6;
    private static final int MDS_1_CONTEXT_STATES_COUNT = 4;
    private static final int CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT = 1;
    private static final int CONTEXTDESCRIPTOR_7_STATES_COUNT = 3;

    private static final UnitTestUtil IT = new UnitTestUtil();

    private final Injector injector = IT.getInjector();
    private final MockEntryFactory mockEntryFactory = new MockEntryFactory(injector.getInstance(MdibTypeValidator.class));
    private final org.somda.sdc.biceps.model.message.ObjectFactory messageModelFactory =
            injector.getInstance(org.somda.sdc.biceps.model.message.ObjectFactory.class);
    private final SoapUtil soapUtil = injector.getInstance(SoapUtil.class);
    private HighPriorityServices highPriorityServices;

    @BeforeEach
    void setUp() throws Exception {
        var tree = new BaseTreeModificationsSet(mockEntryFactory);
        var mods = tree.createBaseTree();
        mods.insert(mockEntryFactory.entry(Handles.SYSTEMCONTEXT_1, SystemContextDescriptor.class, Handles.MDS_1))
                .insert(mockEntryFactory.contextEntry(Handles.CONTEXTDESCRIPTOR_6, Handles.CONTEXT_6,
                        PatientContextDescriptor.class, Handles.SYSTEMCONTEXT_1))
                .insert(mockEntryFactory.contextEntry(Handles.CONTEXTDESCRIPTOR_7,
                        List.of(Handles.CONTEXT_7, Handles.CONTEXT_8, Handles.CONTEXT_9),
                        EnsembleContextDescriptor.class, Handles.SYSTEMCONTEXT_1));

        var mdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        mdibAccess.writeDescription(mods);

        highPriorityServices = injector.getInstance(ServicesFactory.class).createHighPriorityServices(mdibAccess);
    }

    @Test
    void getContextStatesAll() throws SoapFaultException {
        var response = invokeGetContextStates(List.of());
        assertEquals(MDS_0_CONTEXT_STATES_COUNT + MDS_1_CONTEXT_STATES_COUNT, response.getContextState().size());
    }

    @Test
    void getContextStatesFilterWithMds() throws SoapFaultException {
        var response = invokeGetContextStates(List.of(Handles.MDS_1));
        assertEquals(MDS_1_CONTEXT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.MDS_0));
        assertEquals(MDS_0_CONTEXT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.MDS_0, Handles.MDS_1));
        assertEquals(MDS_0_CONTEXT_STATES_COUNT + MDS_1_CONTEXT_STATES_COUNT, response.getContextState().size());
    }

    @Test
    void getContextStatesFilterWithDescriptorHandle() throws SoapFaultException {
        var response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_0));
        assertEquals(CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_1));
        assertEquals(CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_7));
        assertEquals(CONTEXTDESCRIPTOR_7_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_1, Handles.CONTEXTDESCRIPTOR_7));
        assertEquals(CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT + CONTEXTDESCRIPTOR_7_STATES_COUNT,
                response.getContextState().size());
    }

    @Test
    void getContextStatesFilterWithStateHandle() throws SoapFaultException {
        var someContextStateHandles = List.of(
                Handles.CONTEXT_0, Handles.CONTEXT_1, Handles.CONTEXT_6, Handles.CONTEXT_8, Handles.CONTEXT_9);
        var response = invokeGetContextStates(someContextStateHandles);

        // exemplary check if result contains the right context states
        assertEquals(someContextStateHandles.size(), response.getContextState().stream().filter(state ->
                someContextStateHandles.contains(state.getHandle())
        ).count());

        response = invokeGetContextStates(List.of(Handles.CONTEXT_2));
        assertEquals(1, response.getContextState().size());
    }

    private SoapMessage createGetContextStatesRequest(List<String> handleRefs) {
        var getContextStates = messageModelFactory.createGetContextStates();
        getContextStates.setHandleRef(handleRefs);
        return soapUtil.createMessage(
                ActionConstants.ACTION_GET_CONTEXT_STATES,
                getContextStates
        );
    }

    private SoapMessage createGetContextStatesResponse() {
        return soapUtil.createMessage(ActionConstants.getResponseAction(ActionConstants.ACTION_GET_CONTEXT_STATES));
    }

    private RequestResponseObject createGetContextStatesRequestResponseObject(List<String> handleRefs) {
        return new RequestResponseObject(
                createGetContextStatesRequest(handleRefs),
                createGetContextStatesResponse(),
                mock(CommunicationContext.class)
        );
    }

    private GetContextStatesResponse invokeGetContextStates(List<String> handleRefs) throws SoapFaultException {
        var rr = createGetContextStatesRequestResponseObject(handleRefs);
        highPriorityServices.getContextStates(rr);
        return soapUtil.getBody(rr.getResponse(), GetContextStatesResponse.class).orElseThrow();
    }
}