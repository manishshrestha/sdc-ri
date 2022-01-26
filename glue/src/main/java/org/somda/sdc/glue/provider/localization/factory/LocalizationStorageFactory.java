package org.somda.sdc.glue.provider.localization.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.glue.provider.localization.LocalizationDataProvider;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;

/**
 * Factory to create {@linkplain LocalizationStorage} instances.
 */
public interface LocalizationStorageFactory {
    /**
     * Creates a new {@linkplain LocalizationStorage} instance.
     *
     * @param localizationDataProvider localization data provide to use
     * @return a new {@linkplain LocalizationStorage} instance.
     */
    LocalizationStorage createLocalizationStorage(@Assisted LocalizationDataProvider localizationDataProvider);
}
