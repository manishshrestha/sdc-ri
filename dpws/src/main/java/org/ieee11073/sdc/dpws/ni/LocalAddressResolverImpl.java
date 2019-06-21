package org.ieee11073.sdc.dpws.ni;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Optional;

/**
 * Default implementation of {@link LocalAddressResolver}.
 */
public class LocalAddressResolverImpl implements LocalAddressResolver {
    private static final Logger LOG = LoggerFactory.getLogger(LocalAddressResolverImpl.class);

    @Inject
    LocalAddressResolverImpl() {
    }

    @Override
    public Optional<String> getLocalAddress(URI remoteUri) {
        try {
            Socket socket = new Socket(remoteUri.getHost(), remoteUri.getPort());
            return Optional.of(socket.getLocalAddress().getHostAddress());
        } catch (Exception e) {
            LOG.info("Could not access remote URI {} and resolve local address. Reason: {}", remoteUri, e.getMessage());
            return Optional.empty();
        }

//        Enumeration<NetworkInterface> networkInterfaces;
//        try {
//            networkInterfaces = NetworkInterface.getNetworkInterfaces();
//        } catch (SocketException e) {
//            return Optional.empty();
//        }
//
//        Optional<InetAddress> localAddress = Optional.empty();
//        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
//            try {
//                if (!networkInterface.isUp()) {
//                    continue;
//                }
//
//                localAddress = Collections.list(networkInterface.getInetAddresses()).parallelStream()
//                        .filter(inetAddress -> {
//                            try {
//                                if (!inetAddress.isReachable(2000)) {
//                                    return false;
//                                }
//                            } catch (IOException e) {
//                                return false;
//                            }
//
//                            try (SocketChannel socket = SocketChannel.open()) {
//                                // Bind socket to local interface
//                                socket.bind(new InetSocketAddress(inetAddress, 8080));
//
//                                // Try to connect to given host
//                                socket.connect(new InetSocketAddress(host, port));
//                            } catch (IOException e) {
//                                return false;
//                            }
//
//                            return true;
//                        }).findFirst();
//
//                if (localAddress.isPresent()) {
//                    break;
//                }
//            } catch (SocketException e) {
//                LOG.info("Caught socket exception while trying to get local address.", e);
//            }
//        }
//
//        if (localAddress.isPresent()) {
//            return Optional.ofNullable(localAddress.get().getHostAddress());
//        }
    }
}
