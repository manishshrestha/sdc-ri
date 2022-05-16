package org.somda.sdc.glue.provider.services.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.provider.services.helper.MdibRevisionObserver;

/**
 * TODO #142
 */
public interface MdibRevisionObserverFactory {

    MdibRevisionObserver createMdibRevisionObserver(@Assisted EventSourceAccess eventSourceAccess);
}
