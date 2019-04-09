package org.ieee11073.sdc.dpws.udp.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.udp.UdpBindingService;

import java.net.InetAddress;

/**
 * Factory to create {@link UdpBindingService} instances.
 */
public interface UdpBindingServiceFactory {

    /**
     * Create {@link UdpBindingService} instance.
     *
     * @param socketAddress The UDP socket address;
     * @param socketPort The UDP socket port.
     * @param maxMessageSize Maximum allowed message size for any messages sent over this binding.
     */
    UdpBindingService createUdpBindingService(@Assisted InetAddress socketAddress,
                                              @Assisted("socketPort") Integer socketPort,
                                              @Assisted("maxMessageSize") Integer maxMessageSize);
}
