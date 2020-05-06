package org.somda.sdc.dpws.network;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;

import java.net.Socket;
import java.net.URI;
import java.util.Optional;

/**
 * Default implementation of {@linkplain LocalAddressResolver}.
 */
public class LocalAddressResolverImpl implements LocalAddressResolver {
    private static final Logger LOG = LogManager.getLogger(LocalAddressResolverImpl.class);
    private final Logger instanceLogger;

    @Inject
    LocalAddressResolverImpl(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
    }

    @Override
    public Optional<String> getLocalAddress(String remoteUri) {
        var parsedUri = URI.create(remoteUri);
        try (Socket socket = new Socket(parsedUri.getHost(), parsedUri.getPort())) {
            return Optional.of(socket.getLocalAddress().getHostAddress());
        } catch (Exception e) {
            instanceLogger.info("Could not access remote URI {} and resolve local address. Reason: {}", remoteUri, e.getMessage());
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
//                localAddress = Collections.list(networkInterface.getInetAddresses()).stream()
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
