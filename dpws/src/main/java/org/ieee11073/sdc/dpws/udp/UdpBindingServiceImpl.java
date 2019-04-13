package org.ieee11073.sdc.dpws.udp;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.DpwsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Random;

public class UdpBindingServiceImpl extends AbstractIdleService implements UdpBindingService {
    private static final Logger LOG = LoggerFactory.getLogger(UdpBindingServiceImpl.class);


    private final Random random = new Random();
    private final InetAddress socketAddress;
    private final Integer socketPort;
    private Thread socketRunner;

    private DatagramSocket incomingSocket;
    private DatagramSocket outgoingSocket;

    private final int maxMessageSize;
    private UdpMessageReceiverCallback receiver;

    @AssistedInject
    UdpBindingServiceImpl(@Assisted InetAddress socketAddress,
                          @Assisted("socketPort") Integer socketPort,
                          @Assisted("maxMessageSize") Integer maxMessageSize) {
        this.socketAddress = socketAddress;
        this.socketPort = socketPort;
        this.maxMessageSize = maxMessageSize;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Start UDP binding {}.", this);
        InetSocketAddress address = new InetSocketAddress(socketAddress, socketPort);

        try {
            if (socketAddress.isMulticastAddress()) {
                MulticastSocket multicastSocket = new MulticastSocket(socketPort);
                multicastSocket.joinGroup(socketAddress);
                incomingSocket = multicastSocket;
            } else {
                incomingSocket = new DatagramSocket(address);
            }
        } catch (SocketException e) {
            LOG.warn(String.format("Error while connecting to %s:%s.", socketAddress.toString(), socketPort),
                    e.getMessage());
        }

        outgoingSocket = new DatagramSocket();

        if (receiver == null) {
            LOG.info("No data receiver configured; ignore incoming UDP messages.");
        } else {
            LOG.info("Data receiver configured; process incoming UDP messages.");
            this.socketRunner = new Thread(() -> {
                while (!socketRunner.isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(new byte[maxMessageSize], maxMessageSize);
                    try {
                        incomingSocket.receive(packet);
                    } catch (IOException e) {
                        LOG.trace("Could not process UDP packet. Discard.");
                        continue;
                    }

                    UdpMessage message = new UdpMessage(packet.getData(), packet.getLength(),
                            packet.getAddress().getHostAddress(), packet.getPort());
                    receiver.receive(message);
                }
            });

            socketRunner.start();
        }

        LOG.info("UDP binding {} is running.", this);
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down UDP binding {}.", this);
        socketRunner.interrupt();
        incomingSocket.close();
        outgoingSocket.close();
        LOG.info("UDP binding {} shut down.", this);
    }

    @Override
    public void setMessageReceiver(UdpMessageReceiverCallback receiver) {
        this.receiver = receiver;
    }

    @Override
    public void sendMessage(UdpMessage message) throws IOException {
        if (!isRunning()) {
            LOG.warn("Try to send message, but service is not running. Skip.");
            return;
        }
        if (message.getLength() > maxMessageSize) {
            String msg = String.format("Exceed maximum UDP message size. Try to write %d Bytes, but only %d Bytes allowed",
                    message.getLength(), maxMessageSize);
            throw new IOException(msg);
        }
        DatagramPacket packet = new DatagramPacket(message.getData(), message.getLength());

        if (message.hasTransportData()) {
            packet.setAddress(InetAddress.getByName(message.getHost()));
            packet.setPort(message.getPort());
        } else {
            packet.setAddress(socketAddress);
            packet.setPort(socketPort);
        }

        sendMessageWithRetry(packet);
    }

    private void sendMessageWithRetry(DatagramPacket packet) throws IOException {
        outgoingSocket.send(packet);

        // Retransmission algorithm as defined in
        // http://docs.oasis-open.org/ws-dd/soapoverudp/1.1/os/wsdd-soapoverudp-1.1-spec-os.docx
        int udpMinDelay = (int)DpwsConstants.UDP_MIN_DELAY.toMillis();
        int udpMaxDelay = (int)DpwsConstants.UDP_MAX_DELAY.toMillis();
        int udpUpperDelay = (int)DpwsConstants.UDP_UPPER_DELAY.toMillis();
        int t = random.nextInt(udpMaxDelay - udpMinDelay + 1) + udpMinDelay;

        // Use MULTICAST_UDP_REPEAT since UNICAST and MULTICAST repeat numbers are the same in DPWS
        for (int udpRepeat = DpwsConstants.MULTICAST_UDP_REPEAT; udpRepeat > 0; --udpRepeat) {
            try {
                Thread.sleep(t);
            } catch (InterruptedException e) {
                LOG.warn("Thread interrupted.", e);
            }

            outgoingSocket.send(packet);

            t *= 2;
            if (t > udpUpperDelay) {
                t = udpUpperDelay;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("udp://%s:%s", socketAddress.getHostName(), socketPort);
    }
}
