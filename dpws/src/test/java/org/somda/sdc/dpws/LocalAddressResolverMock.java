package org.somda.sdc.dpws;

import org.somda.sdc.dpws.network.LocalAddressResolver;

import java.net.URI;
import java.util.Optional;

public class LocalAddressResolverMock implements LocalAddressResolver {
    @Override
    public Optional<String> getLocalAddress(URI remoteUri) {
        return Optional.of(remoteUri.getHost() + "-local");
    }
}
