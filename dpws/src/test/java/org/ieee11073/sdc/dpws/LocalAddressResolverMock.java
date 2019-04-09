package org.ieee11073.sdc.dpws;

import org.ieee11073.sdc.dpws.ni.LocalAddressResolver;

import java.net.URI;
import java.util.Optional;

public class LocalAddressResolverMock implements LocalAddressResolver {
    @Override
    public Optional<String> getLocalAddress(URI remoteUri) {
        return Optional.of(remoteUri.getHost() + "-local");
    }
}
