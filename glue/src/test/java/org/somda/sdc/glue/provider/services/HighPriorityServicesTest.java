package org.somda.sdc.glue.provider.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.common.CommonConstants;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.GetContextStates;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.message.GetMdDescription;
import org.somda.sdc.biceps.model.message.GetMdDescriptionResponse;
import org.somda.sdc.biceps.model.message.GetMdState;
import org.somda.sdc.biceps.model.message.GetMdStateResponse;
import org.somda.sdc.biceps.model.message.GetMdib;
import org.somda.sdc.biceps.model.message.GetMdibResponse;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.SetAlertState;
import org.somda.sdc.biceps.model.message.SetAlertStateResponse;
import org.somda.sdc.biceps.model.message.SetComponentState;
import org.somda.sdc.biceps.model.message.SetComponentStateResponse;
import org.somda.sdc.biceps.model.message.SetContextState;
import org.somda.sdc.biceps.model.message.SetContextStateResponse;
import org.somda.sdc.biceps.model.message.SetMetricState;
import org.somda.sdc.biceps.model.message.SetMetricStateResponse;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.biceps.model.message.SetStringResponse;
import org.somda.sdc.biceps.model.message.SetValue;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
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
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.MustUnderstandAppender;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;
import org.somda.sdc.mdpws.consumer.safety.SafetyInfoBuilder;
import org.somda.sdc.mdpws.model.SafetyInfoType;
import org.somda.sdc.mdpws.provider.safety.SafetyXPath;

import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void setValuePlusSafetyInformation() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        var expectedValue = BigDecimal.ONE;
        var expectedDualChannels = Map.of("dc1", "32d22ac3423bdeef1b99311530cd4ad753f0eff7",
                "dc2", "453496f7220805b888b375a34635a3f9c1226cc9");
        var expectedSafetyContexts = Map.of("sc1", "Test", "sc2", BigDecimal.TEN);
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0)
            InvocationResponse process(Context context, BigDecimal actualValue) {
                called.set(true);
                assertEquals(expectedValue, actualValue);
                assertEquals(2, context.getDualChannelValues().size());
                assertEquals("dc1", context.getDualChannelValues().get("dc1")
                        .getReferencedSelector());
                assertEquals(expectedDualChannels.get("dc1"), context.getDualChannelValues().get("dc1")
                        .getValue());
                assertEquals("dc2", context.getDualChannelValues().get("dc2")
                        .getReferencedSelector());
                assertEquals(expectedDualChannels.get("dc2"), context.getDualChannelValues().get("dc2")
                        .getValue());

                assertEquals(2, context.getSafetyContextValues().size());
                assertEquals("sc1", context.getSafetyContextValues().get("sc1")
                        .getReferencedSelector());
                assertEquals(expectedSafetyContexts.get("sc1"), context.getSafetyContextValues().get("sc1")
                        .getValue());
                assertEquals("sc2", context.getSafetyContextValues().get("sc2")
                        .getReferencedSelector());
                assertEquals(expectedSafetyContexts.get("sc2"), context.getSafetyContextValues().get("sc2")
                        .getValue());

                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new SetValue();
        request.setRequestedNumericValue(expectedValue);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        var builder = SafetyInfoBuilder.create();
        builder.addDualChannelValue("dc1", expectedDualChannels.get("dc1"));
        builder.addDualChannelValue("dc2", expectedSafetyContexts.get("dc2"));
        builder.addSafetyContextValue("sc1", expectedSafetyContexts.get("sc1"));
        builder.addSafetyContextValue("sc2", expectedSafetyContexts.get("sc2"));

        assertDoesNotThrow(() -> rrObj.getRequest().getOriginalEnvelope().getHeader().getAny()
                .add(MustUnderstandAppender.append(builder.get(), true)));

        // Check if must understand has been attached
        var headers = rrObj.getRequest().getOriginalEnvelope().getHeader().getAny();
        var safetyInfoElement = (JAXBElement<SafetyInfoType>) headers.get(headers.size() - 1);
        assertEquals("true",
                safetyInfoElement.getValue().getOtherAttributes().get(CommonConstants.QNAME_MUST_UNDERSTAND_ATTRIBUTE));

        highPriorityServices.setValue(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), SetValueResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
    }

    @Test
    void setString() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        var expectedValue = "aString";
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0)
            InvocationResponse process(Context context, String actualValue) {
                called.set(true);
                assertEquals(expectedValue, actualValue);
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new SetString();
        request.setRequestedStringValue(expectedValue);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        highPriorityServices.setString(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), SetStringResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
    }

    @Test
    void activate() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        var expectedArgs = List.of("A", "B", "C").stream().map(s -> {
            var argument = new Activate.Argument();
            argument.setArgValue(s);
            return argument;
        }).collect(Collectors.toList());
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0, listType = Activate.Argument.class)
            InvocationResponse process(Context context, List<Activate.Argument> actualArgs) {
                called.set(true);
                assertArrayEquals(expectedArgs.toArray(), actualArgs.toArray());
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new Activate();
        request.setArgument(expectedArgs);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        highPriorityServices.activate(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), ActivateResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
    }

    @Test
    void setComponentState() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        AbstractDeviceComponentState state = new ChannelState();
        state.setActivationState(ComponentActivation.FAIL);
        var expectedStates = List.of(state);
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0, listType =
                    AbstractDeviceComponentState.class)
            InvocationResponse process(Context context, List<AbstractDeviceComponentState> actualStates) {
                called.set(true);
                assertEquals(expectedStates.get(0).getActivationState(), actualStates.get(0).getActivationState());
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new SetComponentState();
        request.setProposedComponentState(expectedStates);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        highPriorityServices.setComponentState(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), SetComponentStateResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
    }

    @Test
    void setContextState() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        AbstractContextState state = new PatientContextState();
        state.setContextAssociation(ContextAssociation.DIS);
        var expectedStates = List.of(state);
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0, listType =
                    AbstractContextState.class)
            InvocationResponse process(Context context, List<AbstractContextState> actualStates) {
                called.set(true);
                assertEquals(expectedStates.get(0).getContextAssociation(),
                        actualStates.get(0).getContextAssociation());
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new SetContextState();
        request.setProposedContextState(expectedStates);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        highPriorityServices.setContextState(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), SetContextStateResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
    }

    @Test
    void setAlertState() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        var expectedState = new LimitAlertConditionState();
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0)
            InvocationResponse process(Context context, LimitAlertConditionState actualStates) {
                called.set(true);
                assertEquals(expectedState.getActivationState(), actualStates.getActivationState());
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new SetAlertState();
        request.setProposedAlertState(expectedState);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        highPriorityServices.setAlertState(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), SetAlertStateResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
    }

    @Test
    void setMetricState() throws SoapFaultException {
        var called = new AtomicBoolean(false);
        AbstractMetricState state = new NumericMetricState();
        state.setStateVersion(BigInteger.TEN);
        var expectedStates = List.of(state);
        highPriorityServices.addOperationInvocationReceiver(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0, listType =
                    AbstractMetricState.class)
            InvocationResponse process(Context context, List<AbstractMetricState> actualStates) {
                called.set(true);
                assertEquals(expectedStates.get(0).getStateVersion(),
                        actualStates.get(0).getStateVersion());
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        });

        var request = new SetMetricState();
        request.setProposedMetricState(expectedStates);
        request.setOperationHandleRef(Handles.OPERATION_0);
        var rrObj = createRequestResponseObject(request);

        highPriorityServices.setMetricState(rrObj);

        var actualResponse = soapUtil.getBody(rrObj.getResponse(), SetMetricStateResponse.class);
        assertTrue(actualResponse.isPresent());
        assertTrue(called.get());
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