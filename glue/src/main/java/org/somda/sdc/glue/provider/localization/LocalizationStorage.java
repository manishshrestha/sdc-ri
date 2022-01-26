package org.somda.sdc.glue.provider.localization;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;

import java.math.BigInteger;
import java.util.List;

/**
 * Localization storage.
 */
public interface LocalizationStorage extends Service {

    /**
     * Gets a list of all supported languages.
     *
     * @return a list of supported language identifiers.
     */
    List<String> getSupportedLanguages();

    /**
     * Gets a localized text that is referenced in the MDIB.
     *
     * @param references a zero or more reference names of the texts that are requested.
     * @param version    of the referenced text that is requested.
     * @param languages  a zero or more language identifiers to get different translations of the requested text.
     * @return a list of {@linkplain LocalizedText} texts that matches search criteria.
     */
    List<LocalizedText> getLocalizedText(List<String> references,
                                         BigInteger version,
                                         List<String> languages);

}
