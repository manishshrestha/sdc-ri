package org.somda.sdc.glue.provider.localization.helper;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;
import org.somda.sdc.glue.provider.localization.LocalizationDataProvider;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@linkplain LocalizationDataProvider}.
 */
public class LocalizationDataProviderHelper implements LocalizationDataProvider {

    private final List<String> supportedLanguages;
    /**
     * Representation of Map<Version, Table<Row, Column, Value>>,
     * where row = ref, column = lang, value = LocalizedText/
     */
    private final Map<BigInteger, Table<String, String, LocalizedText>> localizationData;

    public LocalizationDataProviderHelper() {
        this.supportedLanguages = List.of("EN", "DE", "ES");
        this.localizationData = new HashMap<>();
        populateData();
    }

    public LocalizationDataProviderHelper(List<String> supportedLanguages, Map<BigInteger, Table<String, String,
            LocalizedText>> localizationData) {
        this.supportedLanguages = supportedLanguages;
        this.localizationData = localizationData;
    }

    public Map<BigInteger, Table<String, String, LocalizedText>> getLocalizationData() {
        return localizationData;
    }

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    /**
     * Currently generated records are:
     * Version = 1  |  REF1  |  EN  |  LocalizedText(...)
     * Version = 1  |  REF1  |  DE  |  LocalizedText(...)
     * Version = 1  |  REF1  |  ES  |  LocalizedText(...)
     * Version = 1  |  REF2  |  EN  |  LocalizedText(...)
     * Version = 1  |  REF2  |  DE  |  LocalizedText(...)
     * Version = 1  |  REF2  |  ES  |  LocalizedText(...)
     * Version = 1  |  REF3  |  EN  |  LocalizedText(...)
     * Version = 1  |  REF3  |  DE  |  LocalizedText(...)
     * Version = 1  |  REF3  |  ES  |  LocalizedText(...)
     * --------------------------------------------------
     * Version = 2  |  REF1  |  EN  |  LocalizedText(...)
     * Version = 2  |  REF1  |  DE  |  LocalizedText(...)
     * Version = 2  |  REF1  |  ES  |  LocalizedText(...)
     * Version = 2  |  REF2  |  EN  |  LocalizedText(...)
     * Version = 2  |  REF2  |  DE  |  LocalizedText(...)
     * Version = 2  |  REF2  |  ES  |  LocalizedText(...)
     * Version = 2  |  REF3  |  EN  |  LocalizedText(...)
     * Version = 2  |  REF3  |  DE  |  LocalizedText(...)
     * Version = 2  |  REF3  |  ES  |  LocalizedText(...)
     */
    private void populateData() {
        localizationData.put(BigInteger.ONE, createRecordsTable(BigInteger.ONE));
        localizationData.put(BigInteger.TWO, createRecordsTable(BigInteger.TWO));
    }

    private Table<String, String, LocalizedText> createRecordsTable(BigInteger version) {
        Table<String, String, LocalizedText> localizedTextTable = HashBasedTable.create();

        supportedLanguages.forEach(lang -> {
            addLocalizedText(localizedTextTable, "REF1", lang, version);
            addLocalizedText(localizedTextTable, "REF2", lang, version);
            addLocalizedText(localizedTextTable, "REF3", lang, version);
        });

        return localizedTextTable;
    }

    private void addLocalizedText(Table<String, String, LocalizedText> localizedTextTable,
                                  String ref, String lang, BigInteger version) {
        var localizedText = new LocalizedText();
        localizedText.setRef(ref);
        localizedText.setLang(lang);
        localizedText.setVersion(version);
        localizedText.setTextWidth(LocalizedTextWidth.S);
        localizedText.setValue(String.format("[version=%s, lang=%s] Translated text for REF: %s", version, lang, ref));
        localizedTextTable.put(ref, lang, localizedText);
    }
}
