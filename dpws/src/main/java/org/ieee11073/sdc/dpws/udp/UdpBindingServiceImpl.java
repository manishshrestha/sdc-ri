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
    private Thread multicastSocketRunner;
    private Thread unicastSocketRunner;

    private DatagramSocket incomingSocket;
    private MulticastSocket incomingMulticastSocket;
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
        this.incomingMulticastSocket = null;
    }

    @Override
    protected void startUp() throws Exception {
        InetSocketAddress address = new InetSocketAddress(socketAddress, socketPort);
        LOG.info("Start UDP binding {}", this);

        try {
            if (socketAddress.isMulticastAddress()) {
                incomingMulticastSocket = new MulticastSocket(socketPort);
                incomingMulticastSocket.joinGroup(socketAddress);
                incomingSocket = incomingMulticastSocket;
            } else {
                incomingSocket = new DatagramSocket(address);
            }
        } catch (SocketException e) {
            LOG.warn(String.format("Error while connecting to %s:%s", socketAddress.toString(), socketPort),
                    e.getMessage());
        }

        outgoingSocket = new DatagramSocket();

        if (receiver == null) {
            LOG.info("No data receiver configured; ignore incoming UDP messages");
        } else {
            LOG.info("Data receiver configured; process incoming UDP messages");

            // Socket to receive any incoming multicast traffic
            this.multicastSocketRunner = new Thread(() -> {
                while (!multicastSocketRunner.isInterrupted()) {
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

            // Socket to receive unicast-replied messages like ProbeMatches and ResolveMatches
            this.unicastSocketRunner = new Thread(() -> {
                while (!unicastSocketRunner.isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(new byte[maxMessageSize], maxMessageSize);
                    try {
                        outgoingSocket.receive(packet);
                    } catch (IOException e) {
                        LOG.trace("Could not process UDP packet. Discard.");
                        continue;
                    }

                    UdpMessage message = new UdpMessage(packet.getData(), packet.getLength(),
                            packet.getAddress().getHostAddress(), packet.getPort());
                    receiver.receive(message);
                }
            });

            multicastSocketRunner.start();
            unicastSocketRunner.start();
        }

        LOG.info("UDP binding {} is running", this);
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down UDP binding {}", this);
        multicastSocketRunner.interrupt();
        unicastSocketRunner.interrupt();
        if (incomingMulticastSocket != null) {
            incomingMulticastSocket.leaveGroup(socketAddress);
        }
        incomingSocket.close();
        outgoingSocket.close();
        LOG.info("UDP binding {} shut down", this);
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
                LOG.info("Thread interrupted");
                break;
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
        String type = socketAddress.isMulticastAddress() ? "multicast" : "unicast";
        return String.format("udp-%s://%s:%s", type, socketAddress.getHostName(), socketPort);
    }
}
