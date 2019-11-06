package org.somda.sdc.glue.provider.services.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.glue.provider.services.HighPriorityServices;

/**
 * Factory to create {@linkplain ServicesFactory} instances.
 */
public interface ServicesFactory {
    /**
     * Creates a new {@linkplain HighPriorityServices} instance.
     *
     * @param mdibAccess the {@link LocalMdibAccess} instance that is used for response MDIB data.
     * @return a new instance.
     */
    HighPriorityServices createHighPriorityServices(@Assisted LocalMdibAccess mdibAccess);
}
