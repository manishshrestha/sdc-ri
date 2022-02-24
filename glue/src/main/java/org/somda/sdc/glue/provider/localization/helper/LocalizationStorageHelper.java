package org.somda.sdc.glue.provider.localization.helper;

import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.glue.common.LocalizationServiceFilterUtil;
import org.somda.sdc.glue.provider.localization.LocalizationDataProvider;
import org.somda.sdc.glue.provider.localization.LocalizationException;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.somda.sdc.glue.common.LocalizationServiceFilterUtil.filterByLanguage;

/**
 * Default implementation of {@linkplain LocalizationStorage}.
 */
public class LocalizationStorageHelper extends AbstractIdleService implements LocalizationStorage {

    private final LocalizationDataProvider localizationDataProvider;

    /**
     * Representation of Map<Version, Table<Row, Column, Value>>,
     * where row = ref, column = lang, value = LocalizedText
     */
    private Map<BigInteger, Table<String, String, LocalizedText>> localizationStorage;

    @AssistedInject
    public LocalizationStorageHelper(@Assisted LocalizationDataProvider localizationDataProvider) {
        this.localizationDataProvider = localizationDataProvider;
    }

    @Override
    protected void startUp() throws Exception {
        localizationStorage = localizationDataProvider.getLocalizationData();
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Override
    public List<String> getSupportedLanguages() {
        return localizationDataProvider.getSupportedLanguages();
    }

    /* Filtering logic:
         List<LocalizedTextRef> references: if not provided, return all, otherwise return TEXT for matching REF;
         ReferencedVersion version: if not provided, returns the LATEST VERSION of the TEXT;
         List<xsd:language> languages: if not provided, all TEXT translations returned;
    */
    @Override
    public List<LocalizedText> getLocalizedText(List<String> references,
                                                BigInteger version,
                                                List<String> languages) {

        // if version not provided, get latest version from storage
        if (BigInteger.ZERO.equals(version)) {
            version = getLatestVersion();
        }

        Multimap<String, LocalizedText> refToValueMap =
                filterByLanguage(localizationStorage.get(version), languages);

        // if references not provided, return all records, otherwise filter by reference
        return references.isEmpty() ? new ArrayList<>(refToValueMap.values()) :
                LocalizationServiceFilterUtil.filterByReferences(refToValueMap, references);
    }

    private BigInteger getLatestVersion() {
        return localizationStorage.entrySet()
                .stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new LocalizationException("Failed to determine latest translations version"));
    }
}
