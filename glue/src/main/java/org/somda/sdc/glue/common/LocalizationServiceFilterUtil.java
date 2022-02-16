package org.somda.sdc.glue.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.somda.sdc.biceps.model.participant.LocalizedText;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LocalizationServiceFilterUtil {

    public static Multimap<String, LocalizedText> filterByLanguage(
            Table<String, String, LocalizedText> localizedTextTable, List<String> languages) {

        Multimap<String, LocalizedText> refToValueMap = ArrayListMultimap.create();

        // if language list provided filter records by language, otherwise include all languages
        if (!languages.isEmpty()) {
            languages.forEach(language -> localizedTextTable.column(language).forEach(refToValueMap::put));
        } else {
            localizedTextTable.columnKeySet().forEach(key -> localizedTextTable.column(key).forEach(refToValueMap::put));
        }
        return refToValueMap;
    }

    public static List<LocalizedText> filterByReferences(List<String> references,
                                                   Multimap<String, LocalizedText> refToValueMap) {
        return references.stream()
                .filter(refToValueMap::containsKey)
                .map(refToValueMap::get)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
