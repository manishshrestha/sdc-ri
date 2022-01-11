package org.somda.sdc.glue.provider.localization;

import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;
import org.somda.sdc.glue.provider.localization.helper.LocalizationStorageHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@linkplain LocalizationService}
 */
public class LocalizationServiceImpl implements LocalizationService {
    private final LocalizationStorageHelper storageHelper;

    public LocalizationServiceImpl() {
        storageHelper = new LocalizationStorageHelper();
    }

    /* TODO: do all param logic
     List<LocalizedTextRef> ref; -> if not provided, return all, otherwise return TEXT for matching REF
     ReferencedVersion version; -> if not provided, returns LATEST VERSION of the TEXT.
     List<xsd:language> lang; -> if not provided, all TEXT translations returned.
     List<LocalizedTextWidth> textWidth;
     List<BigInteger> numberOfLines;
     */
    @Override
    public List<LocalizedText> getLocalizedText(List<String> ref,
                                                BigInteger version,
                                                List<String> lang,
                                                List<LocalizedTextWidth> textWidth,
                                                List<BigInteger> numberOfLines) {

        var storage = storageHelper.getLocalizationStorageByVersion(version);
        // TODO: do filtering in the Helper class.
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return storageHelper.getSupportedLanguages();
    }
}
