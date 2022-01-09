package org.somda.sdc.glue.provider.localization.helper;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to imitate localization database which provides translations.
 */
public class LocalizationStorageHelper {

    public final List<String> supportedLanguages = List.of("EN", "DE", "ES");
    /*
     * Representation of Map<Version, Table<Row, Column, Value>>;
     * Where row = ref, column = lang, value = LocalizedText
     */
    public final Map<BigInteger, Table<String, String, LocalizedText>> localizationStorage = new HashMap<>();


    public LocalizationStorageHelper() {
        populateData();
    }

    public Table<String, String, LocalizedText> getLocalizationStorageByVersion(BigInteger version) {
        return localizationStorage.get(version);
    }

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    private void populateData() {
        localizationStorage.put(BigInteger.ONE, createRecordsTable(BigInteger.ONE));
        localizationStorage.put(BigInteger.TWO, createRecordsTable(BigInteger.TWO));
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
        localizedText.setVersion(BigInteger.ONE);
        localizedText.setTextWidth(LocalizedTextWidth.S);
        localizedText.setValue(String.format("[version=%s, lang=%s] Translated text for REF: %s", version, lang, ref));
        localizedTextTable.put(ref, lang, localizedText);
    }
}
