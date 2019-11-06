package org.ieee11073.sdc.glue.provider.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.dpws.device.EventSourceAccess;
import org.ieee11073.sdc.glue.provider.sco.ScoController;

/**
 * Factory to create {@linkplain ScoControllerFactory} instances.
 */
public interface ScoControllerFactory {
    /**
     * Creates a new {@linkplain ScoController} instance.
     *
     * @param eventSourceAccess the event source access to send report notifications.
     * @param mdibAccess required to fetch MDIB versions.
     * @return the new instance.
     */
    ScoController createScoController(@Assisted EventSourceAccess eventSourceAccess,
                                      @Assisted MdibAccess mdibAccess);
}
