package org.somda.sdc.proto.provider.service.guice;

import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.proto.provider.service.HighPriorityServices;

public interface ServiceFactory {

    HighPriorityServices createHighPriorityServices(LocalMdibAccess mdibAccess);
}
