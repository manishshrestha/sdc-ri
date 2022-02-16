package org.somda.sdc.glue.consumer.localization;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractGetResponse;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetLocalizedTextResponse;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.LocalizationServiceFilterUtil;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;
import org.somda.sdc.glue.consumer.sco.InvocationException;
import org.somda.sdc.glue.guice.Consumer;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller class that is responsible for invoking localization requests.
 */
public class LocalizationServiceProxy implements LocalizationServiceAccess {
    private static final Logger LOG = LogManager.getLogger(LocalizationServiceProxy.class);
    private final HostedServiceProxy localizationServiceProxy;
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final SoapUtil soapUtil;
    private final Logger instanceLogger;
    /**
     * Representation of Map<Version, Table<Row, Column, Value>>,
     * where row = ref, column = lang, value = LocalizedText
     */
    private final Map<BigInteger, Table<String, String, LocalizedText>> localizationCache = new HashMap<>();
    private final Set<String> cachedLanguages = new HashSet<>();


    @AssistedInject
    LocalizationServiceProxy(@Assisted HostingServiceProxy hostingServiceProxy,
                             @Assisted("localizationServiceProxy") @Nullable HostedServiceProxy localizationServiceProxy,
                             @Consumer ExecutorWrapperService<ListeningExecutorService> executorService,
                             SoapUtil soapUtil,
                             @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
        this.localizationServiceProxy = localizationServiceProxy;
        this.executorService = executorService;
        this.soapUtil = soapUtil;
    }

    @Override
    public ListenableFuture<GetLocalizedTextResponse> getLocalizedText(GetLocalizedText getLocalizedText) {
        return executorService.get().submit(() -> {
            instanceLogger.debug("Invoke GetLocalizedText with payload: {}",
                    getLocalizedText.toString());

            //check if texts cache available for requested version && languages
            if (localizationCache.containsKey(getLocalizedText.getVersion()) &&
                    cachedLanguages.containsAll(getLocalizedText.getLang())) {
                return getLocalizedTextFromCache(getLocalizedText);
            }
            // no cache available, request texts from SOAP
            final SoapMessage request = soapUtil.createMessage(ActionConstants.ACTION_GET_LOCALIZED_TEXT,
                    getLocalizedText);
            return sendMessage(request, GetLocalizedTextResponse.class);
        });
    }

    @Override
    public ListenableFuture<GetSupportedLanguagesResponse> getSupportedLanguages(
            GetSupportedLanguages getSupportedLanguages) {
        return executorService.get().submit(() -> {
            instanceLogger.debug("Invoke GetSupportedLanguages");
            final SoapMessage request = soapUtil.createMessage(ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES,
                    getSupportedLanguages);
            return sendMessage(request, GetSupportedLanguagesResponse.class);
        });
    }

    @Override
    public void cachePrefetch(BigInteger version) throws InvocationException {
        cachePrefetch(version, Collections.emptyList());
    }

    @Override
    public void cachePrefetch(BigInteger version, List<String> lang) throws InvocationException {
        var cacheReports = fetchLocalizedTextCache(version, lang);
        if (!localizationCache.containsKey(version)) {
            localizationCache.put(version, cacheReports);
        } else {
            // in case this version was already cached for some languages, but we want to cache additional ones
            // TODO is it needed? It will duplicate records if method is called multiple times with same languages
            localizationCache.get(version).putAll(cacheReports);
        }
    }

    private Table<String, String, LocalizedText> fetchLocalizedTextCache(BigInteger version,
                                                                         List<String> lang) throws InvocationException {
        var request = new GetLocalizedText();
        request.setVersion(version);
        request.setLang(lang);
        var requestMsg = soapUtil.createMessage(ActionConstants.ACTION_GET_LOCALIZED_TEXT, request);
        var response = sendMessage(requestMsg, GetLocalizedTextResponse.class);
        Table<String, String, LocalizedText> localizedTextTable = HashBasedTable.create();
        if (response != null && response.getText() != null && !response.getText().isEmpty()) {
            response.getText().forEach(localizedText -> {
                localizedTextTable.put(localizedText.getRef(), localizedText.getLang(), localizedText);
            });
        }
        // add languages to cache too for easier check later if cache was hit by specific language or not
        // if no languages was provided to the cache method used all available languages from the response result
        cachedLanguages.addAll(!lang.isEmpty() ? lang : localizedTextTable.columnKeySet());

        return localizedTextTable;
    }

    private GetLocalizedTextResponse getLocalizedTextFromCache(GetLocalizedText getLocalizedText) {
        GetLocalizedTextResponse response = new GetLocalizedTextResponse();

        Multimap<String, LocalizedText> refToValueMap = LocalizationServiceFilterUtil.filterByLanguage(
                localizationCache.get(getLocalizedText.getVersion()), getLocalizedText.getLang());

        // if references not provided, return all records, otherwise filter by reference
        var references = getLocalizedText.getRef();
        var texts = references.isEmpty() ? new ArrayList<>(refToValueMap.values()) :
                LocalizationServiceFilterUtil.filterByReferences(references, refToValueMap);

        response.setText(texts);

        return response;
    }

    private <T extends AbstractGetResponse> T sendMessage(SoapMessage request,
                                                          Class<T> expectedResponseClass) throws InvocationException {
        try {
            if (localizationServiceProxy == null) {
                throw new InvocationException("Request could not be sent: no localization service available");
            }

            final SoapMessage response =
                    localizationServiceProxy.getRequestResponseClient().sendRequestResponse(request);
            return soapUtil.getBody(response, expectedResponseClass).orElseThrow(() ->
                    new InvocationException("Received unexpected response"));
        } catch (InterceptorException | SoapFaultException | MarshallingException | TransportException e) {
            throw new InvocationException(String.format("Request to %s failed: %s",
                    localizationServiceProxy.getActiveEprAddress(), e.getMessage()), e);
        }
    }
}
