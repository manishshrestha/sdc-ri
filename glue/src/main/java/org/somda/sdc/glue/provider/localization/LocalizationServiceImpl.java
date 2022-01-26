package org.somda.sdc.glue.provider.localization;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;
import org.somda.sdc.glue.provider.localization.factory.LocalizationStorageFactory;
import org.somda.sdc.glue.provider.localization.helper.LocalizationDataProviderHelper;

import java.math.BigInteger;
import java.util.List;

/**
 * Default implementation of {@linkplain LocalizationService}
 */
public class LocalizationServiceImpl extends AbstractIdleService implements LocalizationService {
    private final LocalizationStorage localizationStorage;

    @Inject
    public LocalizationServiceImpl(LocalizationStorageFactory localizationStorageFactory) {
        localizationStorage =
                localizationStorageFactory.createLocalizationStorage(new LocalizationDataProviderHelper());
    }

    @Override
    protected void startUp() throws Exception {
        localizationStorage.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        localizationStorage.stopAsync().awaitTerminated();
    }

    @Override
    public List<LocalizedText> getLocalizedText(List<String> ref,
                                                BigInteger version,
                                                List<String> lang,
                                                List<LocalizedTextWidth> textWidth,
                                                List<BigInteger> numberOfLines) {
        // textWidth and numberOfLines parameters are currently ignored.
        return localizationStorage.getLocalizedText(ref, version, lang);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return localizationStorage.getSupportedLanguages();
    }
}
