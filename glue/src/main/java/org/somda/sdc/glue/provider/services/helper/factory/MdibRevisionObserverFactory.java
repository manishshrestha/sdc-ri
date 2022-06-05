package org.somda.sdc.glue.provider.services.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.provider.services.helper.MdibRevisionObserver;

/**
 * Factory to create {@linkplain MdibRevisionObserver} instances.
 */
public interface MdibRevisionObserverFactory {

    /**
     * Creates a new {@linkplain MdibRevisionObserver} instance.
     *
     * @param eventSourceAccess the event source access to send history service notifications.
     * @param mdibAccess the {@link LocalMdibAccess} instance that is used to access MDIB data.
     * @return a new instance.
     */
    MdibRevisionObserver createMdibRevisionObserver(@Assisted EventSourceAccess eventSourceAccess,
                                                    @Assisted LocalMdibAccess mdibAccess);
}
