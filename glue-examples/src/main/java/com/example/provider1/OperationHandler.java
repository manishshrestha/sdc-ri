package com.example.provider1;

import com.example.ProviderMdibConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * This class provides a handler for incoming operations on the sdc provider.
 * <p>
 * It implements generic handlers for some operations, which enables handling operations easily, although
 * a real application should be a little more specialized in its handling.
 */
public class OperationHandler implements OperationInvocationReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(OperationHandler.class);

    private final LocalMdibAccess mdibAccess;

    public OperationHandler(LocalMdibAccess mdibAccess) {
        this.mdibAccess = mdibAccess;
    }

    LocalizedText createLocalizedText(String text) {
        return createLocalizedText(text, "en");
    }

    LocalizedText createLocalizedText(String text, String lang) {
        var localizedText = new LocalizedText();
        localizedText.setValue(text);
        localizedText.setLang(lang);
        return localizedText;
    }


    InvocationResponse genericSetValue(Context context, BigDecimal data) {
        // TODO: Check if state is modifiable
        context.sendSuccessfulReport(InvocationState.START);
        String operationHandle = context.getOperationHandle();
        LOG.debug("Received SetValue request for {}: {}", operationHandle, data);

        // find operation target
        var setNumeric = mdibAccess.getDescriptor(operationHandle, SetValueOperationDescriptor.class).orElseThrow(() -> {
                    var errorMessage = createLocalizedText("Operation target cannot be found");
                    context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
                    throw new RuntimeException(
                            String.format("Operation descriptor %s missing", operationHandle)
                    );
                }
        );
        String operationTargetHandle = setNumeric.getOperationTarget();

        var targetDesc = mdibAccess.getDescriptor(operationTargetHandle, NumericMetricDescriptor.class).orElseThrow(() -> {
            var errorMessage = createLocalizedText("Operation target cannot be found");
            context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
            throw new RuntimeException(
                    String.format("Operation target descriptor %s missing", operationTargetHandle)
            );
        });

        // find allowed range for descriptor and verify it's within
        targetDesc.getTechnicalRange().forEach(range -> {
                    if (range.getLower() != null && range.getLower().compareTo(data) > 0) {
                        // value too small
                        var errorMessage = createLocalizedText("Value too small");
                        context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
                        throw new RuntimeException(
                                String.format("Operation set value below lower limit of %s, was %s", range.getLower(), data)
                        );
                    }
                    if (range.getUpper() != null && range.getUpper().compareTo(data) < 0) {
                        // value too big
                        var errorMessage = createLocalizedText("Value too big");
                        context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
                        throw new RuntimeException(
                                String.format("Operation set value below lower limit of %s, was %s", range.getLower(), data)
                        );
                    }
                }
        );

        var targetState = mdibAccess.getState(operationTargetHandle, NumericMetricState.class).orElseThrow(() -> {
            var errorMessage = createLocalizedText("Operation target state cannot be found");
            context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
            throw new RuntimeException(
                    String.format("Operation target descriptor %s missing", operationTargetHandle)
            );
        });

        if (targetState.getMetricValue() == null) {
            targetState.setMetricValue(new NumericMetricValue());
        }
        targetState.getMetricValue().setValue(data);
        targetState.getMetricValue().setDeterminationTime(Instant.now());
        ProviderUtil.addMetricQualityDemo(targetState.getMetricValue());

        final MdibStateModifications mod = MdibStateModifications.create(MdibStateModifications.Type.METRIC);

        mod.add(targetState);

        try {
            mdibAccess.writeStates(mod);
            context.sendSuccessfulReport(InvocationState.FIN);
            return context.createSuccessfulResponse(InvocationState.FIN);
        } catch (PreprocessingException e) {
            LOG.error("Error while writing states", e);
            var errorMessage = createLocalizedText("Error while writing states");
            context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.UNSPEC, List.of(errorMessage));
            return context.createUnsuccessfulResponse(InvocationState.FAIL, InvocationError.UNSPEC, List.of(errorMessage));
        }
    }

    InvocationResponse genericSetString(Context context, String data, boolean isEnumString) {
        // TODO: Check if state is modifiable
        context.sendSuccessfulReport(InvocationState.START);
        String operationHandle = context.getOperationHandle();
        LOG.debug("Received SetString for {}: {}", operationHandle, data);

        // find operation target
        var setString = mdibAccess.getDescriptor(operationHandle, SetStringOperationDescriptor.class).orElseThrow(() -> {
                    var errorMessage = createLocalizedText("Operation target cannot be found");
                    context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
                    throw new RuntimeException(
                            String.format("Operation descriptor %s missing", operationHandle)
                    );
                }
        );
        String operationTargetHandle = setString.getOperationTarget();

        // verify if new data is allowed for enum strings
        if (isEnumString) {
            var targetDesc = mdibAccess.getDescriptor(operationTargetHandle, EnumStringMetricDescriptor.class).orElseThrow(() -> {
                var errorMessage = createLocalizedText("Operation target descriptor cannot be found");
                context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
                throw new RuntimeException(
                        String.format("Operation target descriptor %s missing", operationTargetHandle)
                );
            });

            // validate data is allowed
            Optional<EnumStringMetricDescriptor.AllowedValue> first = targetDesc.getAllowedValue().stream().filter(x -> x.getValue().equals(data)).findFirst();
            if (first.isEmpty()) {
                // not allowed value, bye bye
                var errormessage = createLocalizedText("Value is not allowed here");
                return context.createUnsuccessfulResponse(mdibAccess.getMdibVersion(), InvocationState.FAIL, InvocationError.UNSPEC, List.of(errormessage));
            }
        }

        var targetState = mdibAccess.getState(operationTargetHandle, StringMetricState.class).orElseThrow(() -> {
            var errorMessage = createLocalizedText("Operation target state cannot be found");
            context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, List.of(errorMessage));
            throw new RuntimeException(
                    String.format("Operation target descriptor %s missing", operationTargetHandle)
            );
        });

        if (targetState.getMetricValue() == null) {
            targetState.setMetricValue(new StringMetricValue());
        }
        targetState.getMetricValue().setValue(data);
        targetState.getMetricValue().setDeterminationTime(Instant.now());
        ProviderUtil.addMetricQualityDemo(targetState.getMetricValue());

        final MdibStateModifications mod = MdibStateModifications.create(MdibStateModifications.Type.METRIC);
        mod.add(targetState);

        try {
            mdibAccess.writeStates(mod);
            context.sendSuccessfulReport(InvocationState.FIN);
            return context.createSuccessfulResponse(InvocationState.FIN);
        } catch (PreprocessingException e) {
            LOG.error("Error while writing states", e);
            var errorMessage = createLocalizedText("Error while writing states");
            context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.UNSPEC, List.of(errorMessage));
            return context.createUnsuccessfulResponse(InvocationState.FAIL, InvocationError.UNSPEC, List.of(errorMessage));
        }
    }

    @IncomingSetServiceRequest(operationHandle = ProviderMdibConstants.HANDLE_SET_VALUE)
    InvocationResponse setSettableNumericMetric(Context context, BigDecimal data) {
        return genericSetValue(context, data);
    }

    @IncomingSetServiceRequest(operationHandle = ProviderMdibConstants.HANDLE_SET_STRING)
    InvocationResponse setSettableStringMetric(Context context, String data) {
        return genericSetString(context, data, false);
    }

    @IncomingSetServiceRequest(operationHandle = ProviderMdibConstants.HANDLE_SET_STRING_ENUM)
    InvocationResponse setSettableEnumMetric(Context context, String data) {
        return genericSetString(context, data, true);
    }

    @IncomingSetServiceRequest(operationHandle = ProviderMdibConstants.HANDLE_ACTIVATE, listType = String.class)
    InvocationResponse activateExample(Context context, List<String> args) {
        context.sendSuccessfulReport(InvocationState.START);
        LOG.debug("Received Activate for {}", ProviderMdibConstants.HANDLE_ACTIVATE);

        context.sendSuccessfulReport(InvocationState.FIN);
        return context.createSuccessfulResponse(mdibAccess.getMdibVersion(), InvocationState.FIN);
    }

}
