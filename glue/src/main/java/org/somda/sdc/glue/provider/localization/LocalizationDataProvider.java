package org.somda.sdc.glue.provider.localization;

import com.google.common.collect.Table;
import org.somda.sdc.biceps.model.participant.LocalizedText;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Localization data (translations) provider.
 */
public interface LocalizationDataProvider {

    Map<BigInteger, Table<String, String, LocalizedText>> getLocalizationData();

    List<String> getSupportedLanguages();
}
