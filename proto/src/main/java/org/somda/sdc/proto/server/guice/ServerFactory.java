package org.somda.sdc.proto.server.guice;

import org.somda.sdc.proto.provider.ProviderSettings;
import org.somda.sdc.proto.server.Server;

public interface ServerFactory {
    Server create(ProviderSettings settings);
}
