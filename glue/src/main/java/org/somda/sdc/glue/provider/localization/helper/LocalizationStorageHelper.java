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
public class LocalizationStorageHelper { //TODO: extend as guice service
    //TODO: LocalizationStorage interface, Helper as default impl. in case real implementation not found

    public final List<String> supportedLanguages = List.of("EN", "DE", "ES"); // TODO: generalize it (inject)+

    /*
     * Representation of Map<Version, Table<Row, Column, Value>>;
     * Where row = ref, column = lang, value = LocalizedText
     * Currently generated records are:
     *   Version = 1  |  REF1  |  EN  |  LocalizedText(...)
     *   Version = 1  |  REF1  |  DE  |  LocalizedText(...)
     *   Version = 1  |  REF1  |  ES  |  LocalizedText(...)
     *   Version = 1  |  REF2  |  EN  |  LocalizedText(...)
     *   Version = 1  |  REF2  |  DE  |  LocalizedText(...)
     *   Version = 1  |  REF2  |  ES  |  LocalizedText(...)
     *   Version = 1  |  REF3  |  EN  |  LocalizedText(...)
     *   Version = 1  |  REF3  |  DE  |  LocalizedText(...)
     *   Version = 1  |  REF3  |  ES  |  LocalizedText(...)
     *   --------------------------------------------------
     *   Version = 2  |  REF1  |  EN  |  LocalizedText(...)
     *   Version = 2  |  REF1  |  DE  |  LocalizedText(...)
     *   Version = 2  |  REF1  |  ES  |  LocalizedText(...)
     *   Version = 2  |  REF2  |  EN  |  LocalizedText(...)
     *   Version = 2  |  REF2  |  DE  |  LocalizedText(...)
     *   Version = 2  |  REF2  |  ES  |  LocalizedText(...)
     *   Version = 2  |  REF3  |  EN  |  LocalizedText(...)
     *   Version = 2  |  REF3  |  DE  |  LocalizedText(...)
     *   Version = 2  |  REF3  |  ES  |  LocalizedText(...)
     *
     * TODO: add new translations
     */
    public final Map<BigInteger, Table<String, String, LocalizedText>> localizationStorage = new HashMap<>();

    public LocalizationStorageHelper() {
        // TODO: provide languages, provide translations / table during init, so provider can provide its own
        //  translations
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
