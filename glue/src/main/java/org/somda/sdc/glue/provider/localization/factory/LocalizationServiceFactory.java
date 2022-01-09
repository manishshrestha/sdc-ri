package org.somda.sdc.glue.provider.localization.factory;

import org.somda.sdc.glue.provider.localization.LocalizationService;

/**
 * Factory to create {@linkplain LocalizationService} instances.
 */
public interface LocalizationServiceFactory {
    /**
     * Creates a new {@linkplain LocalizationService} instance.
     *
     * @return a new {@linkplain LocalizationService} instance.
     */
    LocalizationService createLocalizationService();
}
