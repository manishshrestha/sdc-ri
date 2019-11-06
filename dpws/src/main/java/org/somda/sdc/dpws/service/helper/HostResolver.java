package org.ieee11073.sdc.dpws.service.helper;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.network.LocalAddressResolver;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.model.HostServiceType;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Host resolving utility class.
 */
public class HostResolver {
    private final LocalAddressResolver localAddressResolver;
    private final WsAddressingUtil wsaUtil;

    @Inject
    HostResolver(LocalAddressResolver localAddressResolver,
                 WsAddressingUtil wsaUtil) {
        this.localAddressResolver = localAddressResolver;
        this.wsaUtil = wsaUtil;
    }

    /**
     * Take a {@link HostServiceType} and try to find the first XAddr that is resolvable.
     *
     * @param hostedServiceType A hosted service information set.
     * @return Peer information of the first resolvable XAddr or {@link Optional#empty()} if none is resolvable.
     */
    public Optional<PeerInformation> deriveFirstResolvable(HostedServiceType hostedServiceType) {
        for (EndpointReferenceType eprType : hostedServiceType.getEndpointReference()) {
            Optional<String> hostedSrvAddr = wsaUtil.getAddressUriAsString(eprType);
            if (hostedSrvAddr.isPresent()) {
                Optional<PeerInformation> resolved = resolve(hostedSrvAddr.get());
                if (resolved.isPresent()) {
                    return resolved;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Take a list of XAddrs and try to find the first XAddr that is resolvable.
     *
     * @param xAddrs List of XAddrs.
     * @return Peer information of the first resolvable XAddr or {@link Optional#empty()} if none is resolvable.
     */
    public Optional<PeerInformation> deriveFirstResolvable(List<String> xAddrs) {
        for (String xAddr : xAddrs) {
            Optional<PeerInformation> resolved = resolve(xAddr);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return Optional.empty();
    }

    /**
     * Take a single URI and try to resolve it.
     *
     * @param uri The URI to resolve as string.
     * @return The peer information of the URI or {@link Optional#empty()} if the URI is not resolvable.
     */
    public Optional<PeerInformation> resolve(String uri) {
        try {
            URI remoteAddress = URI.create(uri);
            Optional<String> locAddr = localAddressResolver.getLocalAddress(remoteAddress);
            return locAddr.map(s -> new PeerInformation(remoteAddress, locAddr.get()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
