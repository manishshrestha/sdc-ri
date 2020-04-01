package org.somda.sdc.glue.provider.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.common.CommonConstants;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;
import org.somda.sdc.mdpws.model.DualChannelType;
import org.somda.sdc.mdpws.model.DualChannelValueType;
import org.somda.sdc.mdpws.model.ObjectFactory;
import org.somda.sdc.mdpws.model.SafetyInfoType;
import org.somda.sdc.mdpws.provider.safety.SafetyXPath;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class HighPriorityServicesTest {
    private static final UnitTestUtil UT = new UnitTestUtil();
    private static final String EXTRA_CONTEXT_STATE_HANDLE = "extra-context-state-handle";

    private HighPriorityServices highPriorityServices;
    private SoapUtil soapUtil;
    private LocalMdibAccess mdibAccess;
    private SafetyXPath safetyXPath;


    @BeforeEach
    void beforeEach() throws PreprocessingException {
        var injector = UT.getInjector();
        soapUtil = injector.getInstance(SoapUtil.class);
        mdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();

        var baseTreeMods = new BaseTreeModificationsSet(new MockEntryFactory(injector.getInstance(MdibTypeValidator.class)));
        mdibAccess.writeDescription(baseTreeMods.createBaseTree());
        // append extra context state
        var statesModification = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                .add(MockModelFactory.createContextState(EXTRA_CONTEXT_STATE_HANDLE, Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class));
        mdibAccess.writeStates(statesModification);

        var servicesFactory = injector.getInstance(ServicesFactory.class);
        highPriorityServices = servicesFactory.createHighPriorityServices(mdibAccess);

        safetyXPath = injector.getInstance(SafetyXPath.class);
    }

    @Test
    void getMdib() throws SoapFaultException {
        var rrObj = createRequestResponseObject(new GetMdib());
        highPriorityServices.getMdib(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdibResponse.class);
        assertTrue(actualResponse.isPresent());

        // Plausibility test
        assertFalse(actualResponse.get().getMdib().getMdDescription().getMds().isEmpty());
        assertFalse(actualResponse.get().getMdib().getMdState().getState().isEmpty());
    }

    @Test
    void getMdDescription() throws SoapFaultException {
        {
            // Check with empty filter

            var rrObj = createRequestResponseObject(new GetMdDescription());
            highPriorityServices.getMdDescription(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdDescriptionResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(2, actualResponse.get().getMdDescription().getMds().size());
            assertEquals(Handles.MDS_0, actualResponse.get().getMdDescription().getMds().get(0).getHandle());
            assertEquals(Handles.MDS_1, actualResponse.get().getMdDescription().getMds().get(1).getHandle());
        }

        {
            // Check non-existing descriptor

            var request = new GetMdDescription();
            request.getHandleRef().add(UUID.randomUUID().toString());
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getMdDescription(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdDescriptionResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertTrue(actualResponse.get().getMdDescription().getMds().isEmpty());
        }

        {
            // Check with filter that points to a specific MDS

            var request = new GetMdDescription();
            request.getHandleRef().add(Handles.MDS_1);
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getMdDescription(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdDescriptionResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(1, actualResponse.get().getMdDescription().getMds().size());
            assertEquals(Handles.MDS_1, actualResponse.get().getMdDescription().getMds().get(0).getHandle());
        }

        {
            // Check with filter that points to a child of an MDS

            var request = new GetMdDescription();
            request.getHandleRef().add(Handles.VMD_0);
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getMdDescription(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdDescriptionResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(1, actualResponse.get().getMdDescription().getMds().size());
            assertEquals(Handles.MDS_0, actualResponse.get().getMdDescription().getMds().get(0).getHandle());
        }
    }

    @Test
    void getMdState() throws SoapFaultException {
        {
            // Check with empty filter

            var rrObj = createRequestResponseObject(new GetMdState());
            highPriorityServices.getMdState(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdStateResponse.class);
            assertTrue(actualResponse.isPresent());

            int expectedStateCount = 0;
            for (MdibEntity rootEntity : mdibAccess.getRootEntities()) {
                expectedStateCount += getRecursiveStateCount(mdibAccess, rootEntity);
            }

            // Plausibility test
            assertFalse(actualResponse.get().getMdState().getState().isEmpty());
            assertEquals(expectedStateCount, actualResponse.get().getMdState().getState().size());
        }

        {
            // Check non-existing state

            var request = new GetMdState();
            request.getHandleRef().add(UUID.randomUUID().toString());
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getMdState(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdStateResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertTrue(actualResponse.get().getMdState().getState().isEmpty());
        }

        {
            // Check with single state filter

            var request = new GetMdState();
            request.getHandleRef().add(Handles.VMD_0);
            request.getHandleRef().add(Handles.CHANNEL_0);
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getMdState(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdStateResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(2, actualResponse.get().getMdState().getState().size());
            assertEquals(Handles.VMD_0, actualResponse.get().getMdState().getState().get(0).getDescriptorHandle());
            assertEquals(Handles.CHANNEL_0, actualResponse.get().getMdState().getState().get(1).getDescriptorHandle());
        }

        {
            // Check with multi state filter
            var request = new GetMdState();
            request.getHandleRef().add(Handles.CONTEXT_0);
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getMdState(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdStateResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(1, actualResponse.get().getMdState().getState().size());
            assertEquals(Handles.CONTEXTDESCRIPTOR_0, actualResponse.get().getMdState().getState().get(0).getDescriptorHandle());
            assertTrue(actualResponse.get().getMdState().getState().get(0) instanceof PatientContextState);
            assertEquals(Handles.CONTEXT_0, ((PatientContextState) actualResponse.get().getMdState().getState().get(0)).getHandle());
        }

        {
            // Check with multi state filter
            var request = new GetMdState();
            request.getHandleRef().add(Handles.CONTEXTDESCRIPTOR_0);
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getMdState(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetMdStateResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(2, actualResponse.get().getMdState().getState().size());
            assertEquals(Handles.CONTEXTDESCRIPTOR_0, actualResponse.get().getMdState().getState().get(0).getDescriptorHandle());
            assertTrue(actualResponse.get().getMdState().getState().get(0) instanceof PatientContextState);
            assertEquals(Handles.CONTEXT_0, ((PatientContextState) actualResponse.get().getMdState().getState().get(0)).getHandle());
            assertEquals(Handles.CONTEXTDESCRIPTOR_0, actualResponse.get().getMdState().getState().get(1).getDescriptorHandle());
            assertTrue(actualResponse.get().getMdState().getState().get(1) instanceof PatientContextState);
            assertEquals(EXTRA_CONTEXT_STATE_HANDLE, ((PatientContextState) actualResponse.get().getMdState().getState().get(1)).getHandle());
        }
    }

    @Test
    void getContextStates() throws SoapFaultException {
        {
            // Check with empty filter

            var rrObj = createRequestResponseObject(new GetContextStates());
            highPriorityServices.getContextStates(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetContextStatesResponse.class);
            assertTrue(actualResponse.isPresent());

            int expectedStateCount = mdibAccess.getContextStates().size();

            // Plausibility test
            assertEquals(expectedStateCount, actualResponse.get().getContextState().size());
        }

        {
            // Check with non-existing handles
            var request = new GetContextStates();
            request.getHandleRef().add(UUID.randomUUID().toString());
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getContextStates(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetContextStatesResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertTrue(actualResponse.get().getContextState().isEmpty());
        }

        {
            // Check with context state handle filter

            var request = new GetContextStates();
            request.getHandleRef().addAll(List.of(Handles.CONTEXT_0, Handles.CONTEXT_2));
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getContextStates(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetContextStatesResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(2, actualResponse.get().getContextState().size());
            assertEquals(Handles.CONTEXT_0, actualResponse.get().getContextState().get(0).getHandle());
            assertEquals(Handles.CONTEXT_2, actualResponse.get().getContextState().get(1).getHandle());
        }

        {
            // Check with context descriptor handle filter
            var request = new GetContextStates();
            request.getHandleRef().addAll(List.of(Handles.CONTEXT_0, Handles.CONTEXT_2));
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getContextStates(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetContextStatesResponse.class);
            assertTrue(actualResponse.isPresent());

            // Plausibility test
            assertEquals(2, actualResponse.get().getContextState().size());
            assertEquals(Handles.CONTEXT_0, actualResponse.get().getContextState().get(0).getHandle());
            assertEquals(Handles.CONTEXT_2, actualResponse.get().getContextState().get(1).getHandle());
        }
    }

    @Test
    void setValue() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        var expectedArgs = List.of(mock(Activate.Argument.class), mock(Activate.Argument.class));
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0, listType = Activate.Argument.class)
            InvocationResponse process(Context context, List<Activate.Argument> actualArgs) {
                called.set(true);
                assertArrayEquals(expectedArgs.toArray(), actualArgs.toArray());
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new Activate();
        request.getArgument().addAll(expectedArgs);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        var safetyInfo = new SafetyInfoType();
        DualChannelValueType v;
        safetyInfo.getDualChannel().getValue().add()
        ObjectFactory factory = new ObjectFactory();
        factory.
        safetyInfo.setDualChannel(new DualChannelType());
        safetyInfo.setDualChannel(new DualChannelType());

//        rrObj.getRequest().getOriginalEnvelope().getHeader().getAny()
//                .add()

        highPriorityServices.activate(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), ActivateResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
    }

    @Test
    void setString() {
    }

    @Test
    void activate() {
    }

    @Test
    void setComponentState() {
    }

    @Test
    void setContextState() {
    }

    @Test
    void setAlertState() {
    }

    @Test
    void setMetricState() {
    }

    @Test
    void getDescriptor() throws SoapFaultException {
        {
            // Check with empty filter

            var rrObj = createRequestResponseObject(new GetDescriptor());
            highPriorityServices.getDescriptor(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetDescriptorResponse.class);
            assertTrue(actualResponse.isPresent());

            assertTrue(actualResponse.get().getDescriptor().isEmpty());
        }

        {
            // Check with filter

            var request = new GetDescriptor();
            request.getHandleRef().addAll(List.of(Handles.VMD_0, UUID.randomUUID().toString()));
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getDescriptor(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetDescriptorResponse.class);
            assertTrue(actualResponse.isPresent());
            assertEquals(1, actualResponse.get().getDescriptor().size());
            assertEquals(Handles.VMD_0, actualResponse.get().getDescriptor().get(0).getHandle());
        }
    }

    @Test
    void getContainmentTree() throws SoapFaultException {
        {
            // Check with empty filter

            var rrObj = createRequestResponseObject(new GetContainmentTree());
            highPriorityServices.getContainmentTree(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetContainmentTreeResponse.class);
            assertTrue(actualResponse.isPresent());

            assertNull(actualResponse.get().getContainmentTree().getChildrenCount());
            assertNull(actualResponse.get().getContainmentTree().getEntryType());
            assertNull(actualResponse.get().getContainmentTree().getHandleRef());
            assertNull(actualResponse.get().getContainmentTree().getParentHandleRef());
            assertEquals(2, actualResponse.get().getContainmentTree().getEntry().size());
            assertEquals(Handles.MDS_0, actualResponse.get().getContainmentTree().getEntry().get(0).getHandleRef());
            assertEquals(Handles.MDS_1, actualResponse.get().getContainmentTree().getEntry().get(1).getHandleRef());
        }

        {
            // Check with filter

            var request = new GetContainmentTree();
            request.getHandleRef().addAll(List.of(Handles.VMD_0, Handles.VMD_1));
            var rrObj = createRequestResponseObject(request);
            highPriorityServices.getContainmentTree(rrObj);

            var actualResponse = soapUtil.getBody(rrObj.getResponse(), GetContainmentTreeResponse.class);
            assertTrue(actualResponse.isPresent());

            assertNull(actualResponse.get().getContainmentTree().getChildrenCount());
            assertNull(actualResponse.get().getContainmentTree().getEntryType());
            assertNull(actualResponse.get().getContainmentTree().getHandleRef());
            assertNull(actualResponse.get().getContainmentTree().getParentHandleRef());

            assertEquals(2, actualResponse.get().getContainmentTree().getEntry().size());
            assertEquals(Handles.VMD_0, actualResponse.get().getContainmentTree().getEntry().get(0).getHandleRef());
            assertEquals(2, actualResponse.get().getContainmentTree().getEntry().get(0).getChildrenCount());
            assertEquals(new QName(CommonConstants.NAMESPACE_PARTICIPANT, "VmdDescriptor"), actualResponse.get().getContainmentTree().getEntry().get(0).getEntryType());
            assertEquals(Handles.MDS_0, actualResponse.get().getContainmentTree().getEntry().get(0).getParentHandleRef());

            assertEquals(Handles.VMD_1, actualResponse.get().getContainmentTree().getEntry().get(1).getHandleRef());
            assertEquals(0, actualResponse.get().getContainmentTree().getEntry().get(1).getChildrenCount());
            assertEquals(new QName(CommonConstants.NAMESPACE_PARTICIPANT, "VmdDescriptor"), actualResponse.get().getContainmentTree().getEntry().get(1).getEntryType());
            assertEquals(Handles.MDS_0, actualResponse.get().getContainmentTree().getEntry().get(1).getParentHandleRef());
        }

        {
            // Check violation of biceps:R5030 (different parent handles)

            var request = new GetContainmentTree();
            request.getHandleRef().addAll(List.of(Handles.MDS_0, Handles.VMD_1));
            var rrObj = createRequestResponseObject(request);
            assertThrows(SoapFaultException.class, () -> highPriorityServices.getContainmentTree(rrObj));
        }
    }

    private int getRecursiveStateCount(MdibAccess mdibAccess, MdibEntity entity) {
        int sum = entity.getStates().size();
        for (String childHandle : entity.getChildren()) {
            var childEntity = mdibAccess.getEntity(childHandle);
            if (childEntity.isPresent()) {
                sum += getRecursiveStateCount(mdibAccess, childEntity.get());
            }
        }
        return sum;
    }

    private RequestResponseObject createRequestResponseObject(Object bodyElement) {
        return new RequestResponseObject(
                soapUtil.createMessage("mock:request:" + bodyElement.getClass().getSimpleName(), bodyElement),
                soapUtil.createMessage(),
                mock(CommunicationContext.class));
    }
}