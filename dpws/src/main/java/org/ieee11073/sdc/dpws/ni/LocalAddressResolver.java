package org.ieee11073.sdc.dpws.ni;

import java.net.URI;
import java.util.Optional;

/**
 * Tool to resolve a local address given a remote address.
 */
public interface LocalAddressResolver {

    /**
     * Resolve a local address given a remote URI.
     *
     * @param remoteUri The remote URI to test against.
     * @return {@link Optional} of the resolved address or {@link Optional#empty()} if resolving failed.
     */
    Optional<String> getLocalAddress(URI remoteUri);
}
