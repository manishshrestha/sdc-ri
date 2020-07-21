package org.somda.sdc.proto.provider.guice;

import org.somda.sdc.proto.provider.ProviderImpl;
import org.somda.sdc.proto.provider.ProviderSettings;

public interface ProviderImplFactory {

    ProviderImpl create(ProviderSettings settings);

}
