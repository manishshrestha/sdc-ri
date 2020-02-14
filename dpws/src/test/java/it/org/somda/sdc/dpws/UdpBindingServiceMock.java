package it.org.somda.sdc.dpws;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageReceiverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UdpBindingServiceMock extends AbstractIdleService implements UdpBindingService {
    private static final Logger LOG = LoggerFactory.getLogger(UdpBindingServiceMock.class);
    private static final int MIN_PORT = 49152;
    private static final int MAX_PORT = 65535;
    private static final Random RANDOM = new Random();
    private static final Set<Integer> ACTIVE_PORTS = new HashSet<>();
    private static final EventBus UDP_BUS = new AsyncEventBus(
            Executors.newFixedThreadPool(
                    10,
                    new ThreadFactoryBuilder()
                            .setNameFormat("UDP_BUS-thread-%d")
                            .setDaemon(true)
                            .build()
            )
    );

    private final String selfAddress;
    private final Integer selfPort;
    private final String multicastAddress;
    private final Integer multicastPort;

    private UdpMessageReceiverCallback udpMessageReceiver;

    @AssistedInject
    UdpBindingServiceMock(@Assisted NetworkInterface networkInterface,
                          @Assisted @Nullable InetAddress multicastGroup,
                          @Assisted("multicastPort") @Nullable Integer multicastPort,
                          @Assisted("maxMessageSize") Integer maxMessageSize) {
        this.selfAddress = "0.0.0.0";
        this.selfPort = assignRandomPort();
        if (multicastGroup != null && multicastPort != null) {
            this.multicastAddress = multicastGroup.getHostAddress();
            this.multicastPort = multicastPort;
        } else {
            this.multicastAddress = null;
            this.multicastPort = null;
        }
    }

    @Override
    public void setMessageReceiver(UdpMessageReceiverCallback receiver) {
        udpMessageReceiver = receiver;
    }

    @Override
    public void sendMessage(UdpMessage message) {
        if (!message.hasTransportData()) {
            message = new UdpMessage(message.getData(), message.getLength(), multicastAddress, multicastPort);
        }

        LOG.trace("Posting message from {}:{} to bus\n{}", selfAddress, selfPort, message);
        UDP_BUS.post(new UdpEvent(message, selfAddress, selfPort));
    }

    public String toString() {
        String multicast = "w/o multicast";
        if (multicastAddress != null) {
            multicast = String.format("w/ multicast joined at %s:%s", multicastAddress, multicastPort);
        }

        return String.format("[%s:%s %s]",
                selfAddress,
                selfPort,
                multicast);
    }

    @Override
    protected void startUp() {
        UDP_BUS.register(this);
        LOG.info("UDP message queue for binding is running: {}", this);
    }

    @Override
    protected void shutDown() {
        UDP_BUS.unregister(this);
        LOG.info("UDP message queue for binding shut down: {}", this);
    }

    private Integer assignRandomPort() {
        Integer random;
        do {
            random = RANDOM.nextInt(MAX_PORT - MIN_PORT + 1) + MIN_PORT;
        } while (ACTIVE_PORTS.contains(random));

        ACTIVE_PORTS.add(random);
        return random;
    }

    @Subscribe
    private void receiveUdpMessage(UdpEvent udpMessage) {
        LOG.debug(
                "Received message from sender {}:{}",
                udpMessage.getSenderAddress(),
                udpMessage.getSenderPort()
        );
        boolean isReceiverSelf = udpMessage.getMessage().getHost().equals(selfAddress) &&
                udpMessage.getMessage().getPort().equals(selfPort);
        boolean isReceiverMyMulticast = udpMessage.getMessage().getHost().equals(multicastAddress) &&
                udpMessage.getMessage().getPort().equals(multicastPort);
        if (isReceiverSelf || isReceiverMyMulticast) {
            var message = new UdpMessage(udpMessage.getMessage().getData(), udpMessage.getMessage().getLength(),
                    udpMessage.getSenderAddress(), udpMessage.getSenderPort());
            LOG.debug(
                    "Incoming UDP message from sender {}:{} was directed at me!",
                    udpMessage.getSenderAddress(),
                    udpMessage.getSenderPort()
            );
            LOG.trace("Received incoming udp message {}", message);
            udpMessageReceiver.receive(message);
        }
    }

    private class UdpEvent {
        private final UdpMessage message;
        private final String senderAddress;
        private final Integer senderPort;

        UdpEvent(UdpMessage message, String senderAddress, Integer senderPort) {

            this.message = message;
            this.senderAddress = senderAddress;
            this.senderPort = senderPort;
        }

        public UdpMessage getMessage() {
            return message;
        }

        public String getSenderAddress() {
            return senderAddress;
        }

        public Integer getSenderPort() {
            return senderPort;
        }
    }
}
