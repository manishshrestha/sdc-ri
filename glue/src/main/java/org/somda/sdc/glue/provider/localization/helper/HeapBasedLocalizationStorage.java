package org.somda.sdc.glue.provider.localization.helper;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.apache.commons.lang3.StringUtils;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.glue.common.LocalizationServiceFilterUtil;
import org.somda.sdc.glue.provider.localization.LocalizationException;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@linkplain LocalizationStorage}.
 * <p>
 * Localized texts are stored in heap ({@link Map}) and can be added during the runtime.
 */
public class HeapBasedLocalizationStorage implements LocalizationStorage {

    private final List<String> supportedLanguages = new ArrayList<>();

    /**
     * Representation of Map<Version, Table<Row, Column, Value>>,
     * where row = ref, column = lang, value = LocalizedText
     */
    private final Map<BigInteger, Table<String, String, LocalizedText>> localizationStorage = new HashMap<>();

    @Override
    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    /* Filtering logic:
         List<LocalizedTextRef> references: if not provided, returns all, otherwise returns TEXT for matching REF;
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
                LocalizationServiceFilterUtil.filterByLanguage(localizationStorage.get(version), languages);

        // if references not provided, return all records, otherwise filter by reference
        return references.isEmpty() ? new ArrayList<>(refToValueMap.values()) :
                LocalizationServiceFilterUtil.filterByReferences(refToValueMap, references);
    }

    public void addLocalizedText(LocalizedText text) {
        // check if all mandatory data provided before processing
        if (text.getVersion() == null || StringUtils.isAnyBlank(text.getLang(), text.getRef())) {
            return;
        }

        addToSupportedLanguages(text);
        addToStorage(text);
    }

    public void allLocalizedTexts(Collection<LocalizedText> texts) {
        texts.forEach(this::addLocalizedText);
    }

    private void addToStorage(LocalizedText text) {
        var table = localizationStorage.getOrDefault(
                text.getVersion(),
                HashBasedTable.create());
        table.put(text.getRef(), text.getLang(), text);

        localizationStorage.put(text.getVersion(), table);
    }

    private void addToSupportedLanguages(LocalizedText text) {
        if (!supportedLanguages.contains(text.getLang())) {
            supportedLanguages.add(text.getLang());
        }
    }

    private BigInteger getLatestVersion() {
        return localizationStorage.entrySet()
                .stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new LocalizationException("Failed to determine latest translations version"));
    }
}
