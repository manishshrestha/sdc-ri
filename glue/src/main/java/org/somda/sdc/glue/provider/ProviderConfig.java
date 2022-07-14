package org.somda.sdc.glue.provider;

import org.somda.sdc.glue.provider.services.HistoryService;

/**
 * Configuration for the SDC provider side.
 *
 * @see org.somda.sdc.glue.guice.DefaultGlueConfigModule
 */
public class ProviderConfig {

    /**
     * Enables {@linkplain HistoryService}.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String ENABLE_HISTORY_SERVICE = "SdcGlue.Provider.EnableHistoryService";

    /**
     * Configures the maximum number of historical reports in one notification provided by {@linkplain HistoryService}.
     *
     * <ul>
     * <li>Data type: {@linkplain Integer}
     * <li>Use: optional
     * </ul>
     */
    public static final String MAX_HISTORICAL_REPORTS_PER_NOTIFICATION =
            "SdcGlue.Provider.MaxHistoricalReportsPerNotification";
}
