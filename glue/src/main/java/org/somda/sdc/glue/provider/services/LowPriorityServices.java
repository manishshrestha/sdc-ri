package org.somda.sdc.glue.provider.services;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetLocalizedTextResponse;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.provider.localization.LocalizationService;
import org.somda.sdc.glue.provider.localization.factory.LocalizationServiceFactory;

import java.math.BigInteger;
import java.util.List;

/**
 * Implementation of the low-priority services.
 * <p>
 * Low-priority services are all services that are effective without time-constraints:
 * <ul>
 * <li>Archive service
 * <li>Localization service
 * </ul>
 * <p>
 * todo DGr implementation of LowPriorityServices is missing
 */
public class LowPriorityServices extends WebService {
    private final LocalMdibAccess mdibAccess;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory faultFactory;
    private final ObjectFactory messageModelFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final WsAddressingUtil wsaUtil;
    private final LocalizationService localizationService;

    @AssistedInject
    LowPriorityServices(@Assisted LocalMdibAccess mdibAccess,
                        SoapUtil soapUtil,
                        SoapFaultFactory faultFactory,
                        ObjectFactory messageModelFactory,
                        MdibVersionUtil mdibVersionUtil,
                        WsAddressingUtil wsaUtil,
                        LocalizationServiceFactory localizationServiceFactory) {
        this.mdibAccess = mdibAccess;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.messageModelFactory = messageModelFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.wsaUtil = wsaUtil;
        localizationService = localizationServiceFactory.createLocalizationService();
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_LOCALIZED_TEXT)
    void getLocalizedText(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetLocalizedText getLocalizedText = getRequest(requestResponseObject, GetLocalizedText.class);
        final GetLocalizedTextResponse getLocalizedTextResponse = messageModelFactory.createGetLocalizedTextResponse();
        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            getLocalizedTextResponse.setText(localizationService.getLocalizedText(
                    getLocalizedText.getRef(),
                    getLocalizedText.getVersion(),
                    getLocalizedText.getLang(),
                    getLocalizedText.getTextWidth(),
                    getLocalizedText.getNumberOfLines()));

            setResponse(requestResponseObject, getLocalizedTextResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_LOCALIZED_TEXT));
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES)
    void getSupportedLanguages(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetSupportedLanguages getSupportedLanguages = getRequest(requestResponseObject,
                GetSupportedLanguages.class);
        final GetSupportedLanguagesResponse getSupportedLanguagesResponse =
                messageModelFactory.createGetSupportedLanguagesResponse();
        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            getSupportedLanguagesResponse.setLang(localizationService.getSupportedLanguages());
            setResponse(requestResponseObject, getSupportedLanguagesResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES));
        }
    }

    private <T> T getRequest(RequestResponseObject requestResponseObject, Class<T> bodyType) throws SoapFaultException {
        return soapUtil.getBody(requestResponseObject.getRequest(), bodyType).orElseThrow(() ->
                new SoapFaultException(faultFactory.createSenderFault(String.format("%s SOAP request body is malformed",
                        bodyType.getSimpleName())),
                        requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));
    }

    private <T> void setResponse(RequestResponseObject requestResponseObject,
                                 T response,
                                 MdibVersion mdibVersion,
                                 String responseAction) throws SoapFaultException {
        try {
            mdibVersionUtil.setMdibVersion(mdibVersion, response);
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault("Could not create MDIB version."),
                    requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
        }
        requestResponseObject.getResponse().getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(
                responseAction));
        soapUtil.setBody(response, requestResponseObject.getResponse());
    }
}
