package org.somda.sdc.glue.provider.services;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.sco.ScoController;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.provider.services.helper.ReportGenerator;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;
import org.somda.sdc.mdpws.model.ContextValueType;
import org.somda.sdc.mdpws.model.DualChannelValueType;

import java.util.*;
import java.util.function.Function;

/**
 * Implementation of the high-priority services.
 * <p>
 * High-priority services are all services that provide time-critical data:
 * <ul>
 * <li>Get service
 * <li>Set service
 * <li>Description event service
 * <li>State event service
 * <li>Context service
 * <li>Waveform service
 * <li>Containment tree service
 * </ul>
 * <p>
 */
public class HighPriorityServices extends WebService {
    private final LocalMdibAccess mdibAccess;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory faultFactory;
    private final MdibMapperFactory mdibMapperFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final WsAddressingUtil wsaUtil;
    private final JaxbUtil jaxbUtil;
    private final ScoController scoController;

    private final InstanceIdentifier anonymousSource;
    private final ReportGenerator reportGenerator;

    @AssistedInject
    HighPriorityServices(@Assisted LocalMdibAccess mdibAccess,
                         ReportGeneratorFactory reportGeneratorFactory,
                         SoapUtil soapUtil,
                         SoapFaultFactory faultFactory,
                         MdibMapperFactory mdibMapperFactory,
                         ScoControllerFactory scoControllerFactory,
                         MdibVersionUtil mdibVersionUtil,
                         WsAddressingUtil wsaUtil,
                         JaxbUtil jaxbUtil) {
        this.mdibAccess = mdibAccess;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.mdibMapperFactory = mdibMapperFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.wsaUtil = wsaUtil;
        this.jaxbUtil = jaxbUtil;
        this.scoController = scoControllerFactory.createScoController(this, mdibAccess);
        this.reportGenerator = reportGeneratorFactory.createReportGenerator(this);
        anonymousSource = new InstanceIdentifier();
        anonymousSource.setExtensionName("TBD");
        anonymousSource.setRootName("TBD");

        mdibAccess.registerObserver(reportGenerator);
    }

    /**
     * Registers an object that possesses callback functions for incoming set service requests.
     *
     * @param receiver the object that includes methods annotated with {@link IncomingSetServiceRequest}.
     * @see ScoController
     */
    public void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        scoController.addOperationInvocationReceiver(receiver);
    }

    /**
     * Sends a periodic state report.
     * <p>
     * This function does not control periodicity.
     * Periodicity has to be controlled by the calling function.
     *
     * @param states      the states that are supposed to be notified.
     * @param mdibVersion the MDIB version the report belongs to.
     * @param <T>         the state type.
     */
    public <T extends AbstractState> void sendPeriodicStateReport(List<T> states, MdibVersion mdibVersion) {
        reportGenerator.sendPeriodicStateReport(states, mdibVersion);
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MDIB)
    void getMdib(RequestResponseObject requestResponseObject) throws SoapFaultException {
        getRequest(requestResponseObject, GetMdib.class);

        var getMdibResponse = new GetMdibResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            var mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdibResponse.setMdib(mdibMapper.mapMdib());

            setResponse(requestResponseObject, getMdibResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_MDIB));
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MD_DESCRIPTION)
    void getMdDescription(RequestResponseObject requestResponseObject) throws SoapFaultException {
        var getMdDescription = getRequest(requestResponseObject, GetMdDescription.class);

        var getMdDescriptionResponse = new GetMdDescriptionResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            var mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdDescriptionResponse.setMdDescription(mdibMapper.mapMdDescription(getMdDescription.getHandleRef()));
            setResponse(requestResponseObject, getMdDescriptionResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_MD_DESCRIPTION));
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MD_STATE)
    void getMdState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        var getMdState = getRequest(requestResponseObject, GetMdState.class);

        var getMdStateResponse = new GetMdStateResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            var mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdStateResponse.setMdState(mdibMapper.mapMdState(getMdState.getHandleRef()));
            setResponse(requestResponseObject, getMdStateResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_MD_STATE));
        }
    }

    /**
     * Answers the incoming GetContextStates request.
     * <p>
     * Filters context states according to the followind rules:
     * <ul>
     * <li>If the msg:GetContextStates/msg:HandleRef list is empty, all context states in the MDIB SHALL be included in the result list.
     * <li>If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list does match a context descriptor HANDLE, then all context states that belong to the corresponding context descriptor SHALL be included in the result list.
     * <li>If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list does match a context state HANDLE, then the corresponding context state SHALL be included in the result list.
     * </ul>
     * <p>
     * The following rule is currently not supported: If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list does match an MDS descriptor, then all context states that are part of this MDS SHALL be included in the result list.
     * <p>
     * todo DGr Implement missing rule once multi-MDS MDIBs are supported
     *
     * @param requestResponseObject the request response object that contains the request data
     * @throws SoapFaultException if something went wrong during processing.
     */
    @MessageInterceptor(ActionConstants.ACTION_GET_CONTEXT_STATES)
    void getContextStates(RequestResponseObject requestResponseObject) throws SoapFaultException {
        var getContextStates = getRequest(requestResponseObject, GetContextStates.class);

        var getContextStatesResponse = new GetContextStatesResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            var contextStates = transaction.getContextStates();
            List<AbstractContextState> filteredContextStates = new ArrayList<>();
            if (getContextStates.getHandleRef().isEmpty()) {
                filteredContextStates = contextStates;
            } else {
                Set<String> filterSet = new HashSet<>(getContextStates.getHandleRef());
                for (AbstractContextState contextState : contextStates) {
                    if (filterSet.contains(contextState.getHandle()) ||
                            filterSet.contains(contextState.getDescriptorHandle())) {
                        filteredContextStates.add(contextState);
                    }
                }
            }

            getContextStatesResponse.setContextState(filteredContextStates);
            setResponse(requestResponseObject, getContextStatesResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_CONTEXT_STATES));
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_CONTEXT_STATES_BY_FILTER)
    void getContextStatesByFilter(RequestResponseObject requestResponseObject) throws SoapFaultException {
        // todo DGr implement getContextStatesByFilter
        throw new SoapFaultException(faultFactory.createReceiverFault(("GetContextStatesByFilter is not available on this device")));
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_CONTEXT_STATES_BY_IDENTIFICATION)
    void getContextStatesByIdentification(RequestResponseObject requestResponseObject) throws SoapFaultException {
        // todo DGr implement getContextStatesByIdentification
        throw new SoapFaultException(faultFactory.createReceiverFault(("GetContextStatesByIdentification is not available on this device")));
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_VALUE)
    void setValue(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetValue.class, SetValueResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_VALUE),
                SetValue::getRequestedNumericValue);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_STRING)
    void setString(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetString.class, SetStringResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_STRING),
                SetString::getRequestedStringValue);
    }

    @MessageInterceptor(ActionConstants.ACTION_ACTIVATE)
    void activate(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, Activate.class, ActivateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_ACTIVATE),
                Activate::getArgument);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_COMPONENT_STATE)
    void setComponentState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetComponentState.class, SetComponentStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_COMPONENT_STATE),
                SetComponentState::getProposedComponentState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_CONTEXT_STATE)
    void setContextState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetContextState.class, SetContextStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_CONTEXT_STATE),
                SetContextState::getProposedContextState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_ALERT_STATE)
    void setAlertState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetAlertState.class, SetAlertStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_ALERT_STATE),
                SetAlertState::getProposedAlertState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_METRIC_STATE)
    void setMetricState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetMetricState.class, SetMetricStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_METRIC_STATE),
                SetMetricState::getProposedMetricState);
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_DESCRIPTOR)
    void getDescriptor(RequestResponseObject requestResponseObject) throws SoapFaultException {
        var getDescriptor = getRequest(requestResponseObject, GetDescriptor.class);

        var getDescriptorResponse = new GetDescriptorResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            for (String handle : getDescriptor.getHandleRef()) {
                transaction.getDescriptor(handle).ifPresent(descriptor ->
                        getDescriptorResponse.getDescriptor().add(descriptor)
                );
            }
            setResponse(requestResponseObject, getDescriptorResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_DESCRIPTOR));
        }
    }

    /**
     * Answers the incoming GetContainmentTree request.
     * <p>
     * The folowing rules apply:
     * <ul>
     * <li>The result shall contain containment tree information of all elements that are matched by msg:GetContainmentTree/msg:HandleRef.
     * <li>If no handle reference is provided, all containment tree elements on MDS level SHALL be returned.
     * </ul>
     *
     * @param requestResponseObject the request response object that contains the request data
     * @throws SoapFaultException if something went wrong during processing.
     */
    @MessageInterceptor(ActionConstants.ACTION_GET_CONTAINMENT_TREE)
    void getContainmentTree(RequestResponseObject requestResponseObject) throws SoapFaultException {
        var getContainmentTree = getRequest(requestResponseObject, GetContainmentTree.class);
        var getContainmentTreeResponse = new GetContainmentTreeResponse();
        var handleReferences = getContainmentTree.getHandleRef();
        var containmentTree = new ContainmentTree();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            List<MdibEntity> filteredEntities;
            if (handleReferences.isEmpty()) {
                filteredEntities = transaction.getRootEntities();
            } else {
                filteredEntities = new ArrayList<>(handleReferences.size());
                Optional<String> parent = null;
                for (String handleReference : handleReferences) {
                    final Optional<MdibEntity> entity = transaction.getEntity(handleReference);
                    if (entity.isPresent()) {
                        if (parent == null) {
                            parent = entity.get().getParent();
                        } else {
                            if (!parent.equals(entity.get().getParent())) {
                                throw new SoapFaultException(faultFactory.createSenderFault(
                                        "Multiple parent handle references found for " +
                                                "requested containment tree, which violates biceps:R5030"));
                            }
                        }

                        filteredEntities.add(entity.get());
                    }
                }
            }

            for (MdibEntity entity : filteredEntities) {
                var entry = new ContainmentTreeEntry();
                entry.setChildrenCount(entity.getChildren().size());
                entry.setParentHandleRef(entity.getParent().orElse(null));
                entry.setHandleRef(entity.getHandle());
                entry.setType(entity.getDescriptor().getType());
                entry.setEntryType(jaxbUtil.getQualifiedName(entity.getDescriptor()).orElse(null));
                containmentTree.getEntry().add(entry);
            }

            getContainmentTreeResponse.setContainmentTree(containmentTree);
            setResponse(requestResponseObject, getContainmentTreeResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_CONTAINMENT_TREE));
        }
    }

    private <T> T getRequest(RequestResponseObject requestResponseObject, Class<T> bodyType) throws SoapFaultException {
        return soapUtil.getBody(requestResponseObject.getRequest(), bodyType).orElseThrow(() ->
                new SoapFaultException(faultFactory.createSenderFault(String.format("%s SOAP request body is malformed",
                        bodyType.getSimpleName()))));
    }

    private <T> void setResponse(RequestResponseObject requestResponseObject,
                                 T response,
                                 MdibVersion mdibVersion,
                                 String responseAction) throws SoapFaultException {
        try {
            mdibVersionUtil.setMdibVersion(mdibVersion, response);
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault("Could not create MDIB version."));
        }
        requestResponseObject.getResponse().getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(
                responseAction));
        soapUtil.setBody(response, requestResponseObject.getResponse());
    }

    private <T extends AbstractSetResponse> T getResponseObjectAsTypeOrThrow(InvocationResponse responseData, Class<T> type) throws SoapFaultException {
        try {
            final T response = type.getConstructor().newInstance();
            response.setSequenceId(responseData.getMdibVersion().getSequenceId());
            response.setInstanceId(responseData.getMdibVersion().getInstanceId());
            response.setMdibVersion(responseData.getMdibVersion().getVersion());

            InvocationInfo invocationInfo = new InvocationInfo();
            invocationInfo.setTransactionId(responseData.getTransactionId());
            invocationInfo.setInvocationState(responseData.getInvocationState());
            invocationInfo.setInvocationError(responseData.getInvocationError());
            invocationInfo.setInvocationErrorMessage(responseData.getInvocationErrorMessage());
            response.setInvocationInfo(invocationInfo);
            return response;
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault(
                    String.format("Response message could not be generated. Reason: %s", e.getMessage())));
        }
    }

    private <T extends AbstractSet, V extends AbstractSetResponse> void processSetServiceRequest(RequestResponseObject requestResponseObject,
                                                                                                 Class<T> requestClass,
                                                                                                 Class<V> responseClass,
                                                                                                 String responseAction,
                                                                                                 Function<T, ?> getPayload) throws SoapFaultException {
        T request = getRequest(requestResponseObject, requestClass);
        var dualChannelValues = resolveDualChannelValues(requestResponseObject);
        var safetyContextValues = resolveSaferyContextValues(requestResponseObject);

        final InvocationResponse invocationResponse;
        try {
            invocationResponse = scoController.processIncomingSetOperation(
                    request.getOperationHandleRef(),
                    anonymousSource,
                    getPayload.apply(request),
                    dualChannelValues,
                    safetyContextValues);
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault(
                    String.format("Error while processing set service request: %s", e.getMessage())));
        }

        setResponse(requestResponseObject, getResponseObjectAsTypeOrThrow(invocationResponse, responseClass),
                invocationResponse.getMdibVersion(), responseAction);
    }

    private Map<String, DualChannelValueType> resolveDualChannelValues(RequestResponseObject requestResponseObject) {
        var values = new HashMap<String, DualChannelValueType>();
        for (var object : requestResponseObject.getRequest().getOriginalEnvelope().getHeader().getAny()) {
            if (object instanceof DualChannelValueType) {
                var dualChannelValueType = (DualChannelValueType) object;
                values.put(dualChannelValueType.getReferencedSelector(), dualChannelValueType);
            }
        }
        return values;
    }

    private Map<String, ContextValueType> resolveSaferyContextValues(RequestResponseObject requestResponseObject) {
        var values = new HashMap<String, ContextValueType>();
        for (var object : requestResponseObject.getRequest().getOriginalEnvelope().getHeader().getAny()) {
            if (object instanceof ContextValueType) {
                var contextValueType = (ContextValueType) object;
                values.put(contextValueType.getReferencedSelector(), contextValueType);
            }
        }
        return values;
    }
}
