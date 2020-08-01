package org.somda.sdc.proto.provider.services.guice;

import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.proto.provider.services.HighPriorityServices;

public interface ServiceFactory {

    HighPriorityServices createHighPriorityServices(LocalMdibAccess mdibAccess);
}
