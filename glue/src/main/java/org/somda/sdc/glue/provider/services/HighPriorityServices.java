package org.somda.sdc.glue.provider.services;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.sco.ScoController;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.provider.services.helper.MdibMapper;
import org.somda.sdc.glue.provider.services.helper.MdibVersionUtil;
import org.somda.sdc.glue.provider.services.helper.factory.MdibMapperFactory;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
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
 * todo DGr tests are missing
 */
public class HighPriorityServices extends WebService {
    private final LocalMdibAccess mdibAccess;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory faultFactory;
    private final ObjectFactory messageModelFactory;
    private final org.somda.sdc.biceps.model.participant.ObjectFactory participantModelFactory;
    private final MdibMapperFactory mdibMapperFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final ScoController scoController;

    private final InstanceIdentifier anonymousSource;

    @AssistedInject
    HighPriorityServices(@Assisted LocalMdibAccess mdibAccess,
                         ReportGeneratorFactory reportGeneratorFactory,
                         SoapUtil soapUtil,
                         SoapFaultFactory faultFactory,
                         ObjectFactory messageModelFactory,
                         org.somda.sdc.biceps.model.participant.ObjectFactory participantModelFactory,
                         MdibMapperFactory mdibMapperFactory,
                         ScoControllerFactory scoControllerFactory,
                         MdibVersionUtil mdibVersionUtil) {
        this.mdibAccess = mdibAccess;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.messageModelFactory = messageModelFactory;
        this.participantModelFactory = participantModelFactory;
        this.mdibMapperFactory = mdibMapperFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.scoController = scoControllerFactory.createScoController(this, mdibAccess);

        anonymousSource = new InstanceIdentifier();
        anonymousSource.setExtensionName("TBD");
        anonymousSource.setRootName("TBD");

        mdibAccess.registerObserver(reportGeneratorFactory.createReportGenerator(this));
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

    @MessageInterceptor(ActionConstants.ACTION_GET_MDIB)
    void getMdib(RequestResponseObject requestResponseObject) throws SoapFaultException {
        getRequest(requestResponseObject, GetMdib.class);

        final GetMdibResponse getMdibResponse = messageModelFactory.createGetMdibResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final MdibMapper mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdibResponse.setMdib(mdibMapper.mapMdib());

            setResponse(requestResponseObject, getMdibResponse, transaction.getMdibVersion());
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MD_DESCRIPTION)
    void getMdDescription(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetMdDescription getMdDescription = getRequest(requestResponseObject, GetMdDescription.class);

        final GetMdDescriptionResponse getMdDescriptionResponse = messageModelFactory.createGetMdDescriptionResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final MdibMapper mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdDescriptionResponse.setMdDescription(mdibMapper.mapMdDescription(getMdDescription.getHandleRef()));
            setResponse(requestResponseObject, getMdDescriptionResponse, transaction.getMdibVersion());
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MD_STATE)
    void getMdState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetMdState getMdState = getRequest(requestResponseObject, GetMdState.class);

        final GetMdStateResponse getMdStateResponse = messageModelFactory.createGetMdStateResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final MdibMapper mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdStateResponse.setMdState(mdibMapper.mapMdState(getMdState.getHandleRef()));
            setResponse(requestResponseObject, getMdStateResponse, transaction.getMdibVersion());
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
     * todo DGr Implement missing rule
     *
     * @param requestResponseObject the request response object that contains the request data
     * @throws SoapFaultException if something went wrong during processing.
     */
    @MessageInterceptor(ActionConstants.ACTION_GET_CONTEXT_STATES)
    void getContextStates(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetContextStates getContextStates = getRequest(requestResponseObject, GetContextStates.class);

        final GetContextStatesResponse getContextStatesResponse = messageModelFactory.createGetContextStatesResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final List<AbstractContextState> contextStates = transaction.getContextStates();
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
            setResponse(requestResponseObject, getContextStatesResponse, transaction.getMdibVersion());
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
                SetValue::getRequestedNumericValue);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_STRING)
    void setString(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetString.class, SetStringResponse.class,
                SetString::getRequestedStringValue);
    }

    @MessageInterceptor(ActionConstants.ACTION_ACTIVATE)
    void activate(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, Activate.class, ActivateResponse.class,
                Activate::getArgument);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_COMPONENT_STATE)
    void setComponentState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetComponentState.class, SetComponentStateResponse.class,
                SetComponentState::getProposedComponentState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_CONTEXT_STATE)
    void setContextState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetContextState.class, SetContextStateResponse.class,
                SetContextState::getProposedContextState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_ALERT_STATE)
    void setAlertState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetAlertState.class, SetAlertStateResponse.class,
                SetAlertState::getProposedAlertState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_METRIC_STATE)
    void setMetricState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetMetricState.class, SetMetricStateResponse.class,
                SetMetricState::getProposedMetricState);
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_DESCRIPTOR)
    void getDescriptor(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetDescriptor getDescriptor = getRequest(requestResponseObject, GetDescriptor.class);

        final GetDescriptorResponse getDescriptorResponse = messageModelFactory.createGetDescriptorResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            for (String handle : getDescriptor.getHandleRef()) {
                transaction.getDescriptor(handle).ifPresent(descriptor ->
                        getDescriptorResponse.getDescriptor().add(descriptor)
                );
            }
            setResponse(requestResponseObject, getDescriptorResponse, transaction.getMdibVersion());
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
        final GetContainmentTree getContainmentTree = getRequest(requestResponseObject, GetContainmentTree.class);
        final GetContainmentTreeResponse getContainmentTreeResponse = messageModelFactory.createGetContainmentTreeResponse();
        final List<String> handleReferences = getContainmentTree.getHandleRef();
        final ContainmentTree containmentTree = participantModelFactory.createContainmentTree();

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
                final ContainmentTreeEntry entry = participantModelFactory.createContainmentTreeEntry();
                entry.setChildrenCount(entity.getChildren().size());
                entry.setHandleRef(entity.getHandle());
                entry.setType(entity.getDescriptor().getType());

                try {
                    final JAXBContext jaxbCtx = JAXBContext.newInstance(entity.getDescriptorClass());
                    final QName qname = jaxbCtx.createJAXBIntrospector().getElementName(entity.getDescriptor());
                    entry.setEntryType(qname);
                } catch (JAXBException e) {
                    throw new SoapFaultException(faultFactory.createReceiverFault(String.format(
                            "Could not resolve entry type the requested descriptor %s. Operation aborted.", entity.getHandle())));
                }

                containmentTree.getEntry().add(entry);
            }

            getContainmentTreeResponse.setContainmentTree(containmentTree);
            setResponse(requestResponseObject, getContainmentTreeResponse, transaction.getMdibVersion());
        }
    }

    private <T> T getRequest(RequestResponseObject requestResponseObject, Class<T> bodyType) throws SoapFaultException {
        return soapUtil.getBody(requestResponseObject.getRequest(), bodyType).orElseThrow(() ->
                new SoapFaultException(faultFactory.createSenderFault(String.format("%s SOAP request body is malformed",
                        bodyType.getSimpleName()))));
    }

    private <T> void setResponse(RequestResponseObject requestResponseObject, T response, MdibVersion mdibVersion) throws SoapFaultException {
        try {
            mdibVersionUtil.setMdibVersion(mdibVersion, response);
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault("Could not create MDIB version."));
        }
        soapUtil.setBody(response, requestResponseObject.getResponse());
    }

    private <T extends AbstractSetResponse> T getResponseObjectAsTypeOrThrow(InvocationResponse responseData, Class<T> type) throws SoapFaultException {
        try {
            final T response = type.getConstructor().newInstance();
            response.setSequenceId(responseData.getMdibVersion().getSequenceId().toString());
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
                    String.format("Response message could not be generated. Reason: ", e.getMessage())));
        }
    }

    private <T extends AbstractSet, V extends AbstractSetResponse> void processSetServiceRequest(RequestResponseObject requestResponseObject,
                                                                                                 Class<T> requestClass,
                                                                                                 Class<V> responseClass,
                                                                                                 Function<T, ?> getPayload) throws SoapFaultException {
        T request = getRequest(requestResponseObject, requestClass);
        final InvocationResponse invocationResponse;
        try {
            invocationResponse = scoController.processIncomingSetOperation(
                    request.getOperationHandleRef(), anonymousSource, getPayload.apply(request));
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault(
                    String.format("Error while processing set service request: ", e.getMessage())));
        }

        setResponse(requestResponseObject, getResponseObjectAsTypeOrThrow(invocationResponse, responseClass),
                invocationResponse.getMdibVersion());
    }
}
