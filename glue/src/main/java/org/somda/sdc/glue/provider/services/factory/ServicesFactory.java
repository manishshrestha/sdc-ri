package org.ieee11073.sdc.glue.provider.services.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccess;
import org.ieee11073.sdc.glue.provider.services.HighPriorityServices;

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
