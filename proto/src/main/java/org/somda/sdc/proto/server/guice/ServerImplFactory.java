package org.somda.sdc.proto.server.guice;

import org.somda.sdc.proto.provider.ProviderSettings;
import org.somda.sdc.proto.server.ServerImpl;

public interface ServerImplFactory {
    ServerImpl create(ProviderSettings settings);
}
