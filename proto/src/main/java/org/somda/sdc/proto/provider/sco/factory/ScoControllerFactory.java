package org.somda.sdc.proto.provider.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.proto.provider.EventSource;
import org.somda.sdc.proto.provider.sco.ScoController;

/**
 * Factory to create {@linkplain ScoControllerFactory} instances.
 */
public interface ScoControllerFactory {
    /**
     * Creates a new {@linkplain ScoController} instance.
     *
     * @param eventSource the event source access to send report notifications.
     * @param mdibAccess  required to fetch MDIB versions and pass to invocation contexts.
     * @return the new instance.
     */
    ScoController createScoController(@Assisted EventSource eventSource,
                                      @Assisted LocalMdibAccess mdibAccess);
}
