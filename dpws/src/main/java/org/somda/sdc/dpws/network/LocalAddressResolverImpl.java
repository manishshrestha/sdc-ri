package org.somda.sdc.dpws.network;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.dpws.DpwsFramework;

import java.net.InterfaceAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Optional;

/**
 * Default implementation of {@linkplain LocalAddressResolver}.
 */
public class LocalAddressResolverImpl implements LocalAddressResolver {
    private static final Logger LOG = LogManager.getLogger(LocalAddressResolverImpl.class);
    private final DpwsFramework dpwsFramework;

    @Inject
    LocalAddressResolverImpl(DpwsFramework dpwsFramework) {
        this.dpwsFramework = dpwsFramework;
    }

    @Override
    public Optional<String> getLocalAddress(String remoteUri) {
        var parsedUri = URI.create(remoteUri);
        var ifaceOpt = dpwsFramework.getNetworkInterface();
        if (ifaceOpt.isEmpty()) {
            LOG.error("getLocalAddress could not determine the configured network interface");
            return Optional.empty();
        }
        var iface = ifaceOpt.get();
        for (InterfaceAddress interfaceAddress : iface.getInterfaceAddresses()) {
            try (Socket socket = new Socket(parsedUri.getHost(), parsedUri.getPort(), interfaceAddress.getAddress(), 0)) {
                return Optional.of(socket.getLocalAddress().getHostAddress());
            } catch (Exception e) {
                LOG.debug(
                        "Could not access remote URI {} and resolve local address using interface {}. Reason: {}",
                        remoteUri, interfaceAddress, e.getMessage()
                );
            }
        }

        LOG.warn(
                "Could not access remote URI {} and resolve local address,"
                        + " no connection could be made using interface {}",
                remoteUri, iface
        );
        return Optional.empty();
    }
}
