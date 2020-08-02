package org.somda.sdc.proto.provider.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.proto.provider.sco.ScoProvider;

public interface ScoProviderFactory {
    ScoProvider createScoProvider(@Assisted LocalMdibAccess localMdibAccess);
}
