package org.somda.sdc.dpws.factory;

import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;

import javax.annotation.Nullable;

/**
 * Factory to create {@linkplain CommunicationLog} instances.
 */
public interface CommunicationLogFactory {
    /**
     * Creates a {@linkplain CommunicationLog} instance receiving a log context.
     *
     * @param context the context information passed to the {@link CommunicationLog} instance.
     * @return a new {@link CommunicationLog} instance.
     */
    CommunicationLog createCommunicationLog(@Nullable CommunicationLogContext context);

    /**
     * Creates a {@linkplain CommunicationLog} instance receiving with no additional context info.
     *
     * @return a new {@link CommunicationLog} instance.
     */
    CommunicationLog createCommunicationLog();
}
