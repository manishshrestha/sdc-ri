package org.somda.sdc.glue.provider.sco;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.ObjectFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.provider.sco.factory.ContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Manages callbacks for incoming set service requests.
 */
public class ScoController {
    private static final Logger LOG = LoggerFactory.getLogger(ScoController.class);

    private final Map<String, ReflectionInfo> invocationReceivers;
    private final List<ReflectionInfo> defaultInvocationReceivers;
    private final EventSourceAccess eventSourceAccess;
    private final LocalMdibAccess mdibAccess;
    private final ContextFactory contextFactory;
    private final ObjectFactory participantModelFactory;

    private long transactionCounter;

    @AssistedInject
    ScoController(@Assisted EventSourceAccess eventSourceAccess,
                  @Assisted LocalMdibAccess mdibAccess,
                  ContextFactory contextFactory,
                  ObjectFactory participantModelFactory) {
        this.eventSourceAccess = eventSourceAccess;
        this.mdibAccess = mdibAccess;
        this.contextFactory = contextFactory;
        this.participantModelFactory = participantModelFactory;
        this.invocationReceivers = new HashMap<>();
        this.defaultInvocationReceivers = new ArrayList<>();

        this.transactionCounter = 0;
    }

    /**
     * Invokes processing of an incoming network set service call.
     *
     * @param handle  the handle of the operation that was called.
     * @param source  the instance identifier that represents the calling client.
     * @param payload the request data.
     * @param <T>     type of the request data.
     * @return the initial invocation info required for the response message. The initial invocation info is
     * requested by the callback that is going to be invoked. In case that no callback can be found, a fail state is
     * returned.
     */
    public <T> InvocationResponse processIncomingSetOperation(String handle, InstanceIdentifier source, T payload) {
        final Context context = contextFactory.createContext(transactionCounter++, handle, source, eventSourceAccess, mdibAccess);

        final LocalizedText localizedText = participantModelFactory.createLocalizedText();
        localizedText.setLang("en");
        localizedText.setValue(String.format("There is no ultimate invocation processor available for operation %s", handle));

        try {
            final ReflectionInfo reflectionInfo = invocationReceivers.get(handle);
            if (reflectionInfo != null) {
                if (reflectionInfo.getCallbackMethod().getParameters()[1].getType().isAssignableFrom(payload.getClass())) {
                    return (InvocationResponse) reflectionInfo.getCallbackMethod().invoke(reflectionInfo.getReceiver(),
                            context, payload);
                }
            }

            for (ReflectionInfo receiver : defaultInvocationReceivers) {
                if (payload instanceof List) {
                    final List<?> payloadList = (List<?>) payload;
                    if (payloadList.isEmpty()) {
                        throw new Exception();
                    }

                    if (receiver.getAnnotation().listType().equals(IncomingSetServiceRequest.NoList.class)) {
                        LOG.warn("For default invocation receivers each method annotation requires a listType attribute." +
                                        " Callback for method {} on object {} ignored.",
                                receiver.getCallbackMethod().getName(), receiver.getReceiver());
                        continue;
                    }

                    if (!receiver.getAnnotation().listType().isAssignableFrom(payloadList.get(0).getClass())) {
                        continue;
                    }
                }

                if (receiver.getCallbackMethod().getParameters()[1].getType().isAssignableFrom(payload.getClass())) {
                    var response = (InvocationResponse) receiver.getCallbackMethod().invoke(receiver.getReceiver(),
                            context, payload);
                    if (!response.getInvocationState().equals(context.getCurrentReportInvocationState())) {
                        LOG.debug(
                                "No matching OperationInvokedReport was sent before sending response." +
                                        " TransactionId: {} - InvocationState: {}",
                                response.getTransactionId(), response.getInvocationState()
                        );
                        context.sendUnsuccessfulReport(
                                response.getMdibVersion(),
                                response.getInvocationState(),
                                response.getInvocationError(),
                                response.getInvocationErrorMessage()
                        );
                    }
                    return response;
                }
            }
        } catch (Exception e) {
            LOG.error("The invocation request could not be forwarded to or processed by the ultimate invocation processor.");
            LOG.trace("The invocation request could not be forwarded to or processed by the ultimate invocation processor.", e);
            localizedText.setValue("The invocation request could not be forwarded to or processed by the ultimate invocation processor");
        }

        // send error report
        return context.createUnsuccessfulResponse(
                mdibAccess.getMdibVersion(),
                InvocationState.FAIL,
                InvocationError.UNSPEC,
                Arrays.asList(localizedText));
    }

    /**
     * Registers an object that possesses callback functions for incoming set service requests.
     * <p>
     * This class is designed to handle exactly one receiver per handle or data type.
     * <em>Adding multiple receivers for one handle or data type causes undefined behavior!</em>
     *
     * @param receiver the object that includes methods annotated with {@link IncomingSetServiceRequest}.
     */
    public void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        boolean annotationFound = false;
        for (Method method : receiver.getClass().getDeclaredMethods()) {
            final IncomingSetServiceRequest annotation = method.getDeclaredAnnotation(IncomingSetServiceRequest.class);
            if (annotation == null) {
                continue;
            }

            if (method.getReturnType() == null || !method.getReturnType().equals(InvocationResponse.class)) {
                continue;
            }

            if (method.getParameterCount() != 2) {
                continue;
            }

            final Parameter[] parameters = method.getParameters();
            if (!parameters[0].getType().equals(Context.class)) {
                continue;
            }

            String key = annotation.operationHandle();
            if (!key.isEmpty() && invocationReceivers.containsKey(key)) {
                LOG.warn("Ignore callback registration for key {} as there is a receiver already", key);
                continue;
            }

            method.setAccessible(true);
            final ReflectionInfo reflectionInfo = new ReflectionInfo(receiver, method, annotation);
            if (key.isEmpty()) {
                defaultInvocationReceivers.add(reflectionInfo);
            } else {
                invocationReceivers.put(key, reflectionInfo);
            }

            annotationFound = true;
        }

        if (!annotationFound) {
            LOG.warn("No callback function found in object {} of type {}", receiver.toString(), receiver.getClass().getName());
        }
    }

    private static class ReflectionInfo {
        private final OperationInvocationReceiver receiver;
        private final Method callbackMethod;
        private final IncomingSetServiceRequest annotation;

        public ReflectionInfo(OperationInvocationReceiver receiver, Method callbackMethod, IncomingSetServiceRequest annotation) {
            this.receiver = receiver;
            this.callbackMethod = callbackMethod;
            this.annotation = annotation;
        }

        public OperationInvocationReceiver getReceiver() {
            return receiver;
        }

        public Method getCallbackMethod() {
            return callbackMethod;
        }

        public IncomingSetServiceRequest getAnnotation() {
            return annotation;
        }
    }
}
