package org.somda.sdc.dpws.udp;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.network.NetworkInterfaceUtil;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.TransportException;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Random;

/**
 * Default implementation of {@linkplain UdpBindingService}.
 */
public class UdpBindingServiceImpl extends AbstractIdleService implements UdpBindingService {
    private static final Logger LOG = LogManager.getLogger(UdpBindingServiceImpl.class);

    private final Random random = new Random();
    private final NetworkInterface networkInterface;
    private final InetAddress multicastGroup;
    private final Integer socketPort;
    private Thread multicastSocketRunner;
    private Thread unicastSocketRunner;

    private DatagramSocket incomingSocket;
    private MulticastSocket multicastSocket;
    private DatagramSocket outgoingSocket;

    private final int maxMessageSize;
    private NetworkInterfaceUtil networkInterfaceUtil;
    private final CommunicationLog communicationLog;
    private UdpMessageReceiverCallback receiver;
    private InetAddress networkInterfaceAddress;
    private InetSocketAddress multicastAddress;

    @AssistedInject
    UdpBindingServiceImpl(@Assisted NetworkInterface networkInterface,
                          @Assisted @Nullable InetAddress multicastGroup,
                          @Assisted("multicastPort") Integer multicastPort,
                          @Assisted("maxMessageSize") Integer maxMessageSize,
                          NetworkInterfaceUtil networkInterfaceUtil,
                          CommunicationLog communicationLog) {
        this.networkInterface = networkInterface;
        this.multicastGroup = multicastGroup;
        this.socketPort = multicastPort;
        this.maxMessageSize = maxMessageSize;
        this.networkInterfaceUtil = networkInterfaceUtil;
        this.communicationLog = communicationLog;
        this.multicastAddress = new InetSocketAddress(multicastGroup, socketPort);
        this.multicastSocket = null;
        this.networkInterfaceAddress = null;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Start UDP binding on network interface {}", this);
        // try to get first available address from network interface
        networkInterfaceAddress = networkInterfaceUtil.getFirstIpV4Address(networkInterface).orElseThrow(() ->
                new SocketException(String.format("Could not retrieve network interface address from: %s", networkInterface)));

        LOG.info("Bind to address {}", networkInterfaceAddress);

        outgoingSocket = new DatagramSocket(0, networkInterfaceAddress);
        LOG.info("Outgoing socket at {} is open", outgoingSocket.getLocalSocketAddress());
        if (multicastGroup != null) {
            if (!multicastGroup.isMulticastAddress()) {
                throw new Exception(String.format("Given address is not a multicast address: %s", multicastGroup));
            }

            multicastSocket = new MulticastSocket(socketPort);
            LOG.info("Join to UDP multicast address group {}", multicastAddress);
            multicastSocket.joinGroup(multicastAddress, networkInterface);
            incomingSocket = multicastSocket;
        } else {
            incomingSocket = new DatagramSocket(0, networkInterfaceAddress);
            LOG.info("Incoming socket is open: {}", incomingSocket.getLocalSocketAddress());
        }

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

                    var ctxt = new CommunicationContext(
                            new ApplicationInfo(),
                            new TransportInfo(
                                    DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                                    incomingSocket.getLocalAddress().getHostAddress(),
                                    incomingSocket.getLocalPort(),
                                    packet.getAddress().getHostAddress(),
                                    packet.getPort(),
                                    Collections.emptyList()
                            )
                    );

                    UdpMessage message = new UdpMessage(packet.getData(), packet.getLength(), ctxt);
                    this.logUdpPacket(CommunicationLog.Direction.INBOUND, packet);
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

                    var ctxt = new CommunicationContext(
                            new ApplicationInfo(),
                            new TransportInfo(
                                    DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                                    outgoingSocket.getLocalAddress().getHostAddress(),
                                    outgoingSocket.getLocalPort(),
                                    packet.getAddress().getHostAddress(),
                                    packet.getPort(),
                                    Collections.emptyList()
                            )
                    );

                    UdpMessage message = new UdpMessage(packet.getData(), packet.getLength(), ctxt);
                    this.logUdpPacket(CommunicationLog.Direction.INBOUND, packet);
                    receiver.receive(message);
                }
            });

            multicastSocketRunner.start();
            unicastSocketRunner.start();
        }

        // wait for the sockets, the IGMP join and the worker threads to be available
        // this has primarily been an issue in low performance environments such as the CI
        Thread.sleep(1000);

        LOG.info("UDP binding {} is running", this);
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down UDP binding {}", this);
        multicastSocketRunner.interrupt();
        unicastSocketRunner.interrupt();
        if (multicastSocket != null && multicastGroup != null && multicastAddress != null) {
            multicastSocket.leaveGroup(multicastAddress, networkInterface);
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
    public void sendMessage(UdpMessage message) throws IOException, TransportException {
        if (!isRunning()) {
            LOG.warn("Try to send message, but service is not running. Skip.");
            return;
        }
        if (message.getLength() > maxMessageSize) {
            String msg = String.format("Exceed maximum UDP message size. Try to write %d Bytes, but only %d Bytes allowed.",
                    message.getLength(), maxMessageSize);
            throw new IOException(msg);
        }

        DatagramPacket packet = new DatagramPacket(message.getData(), message.getLength());

        if (message.hasTransportData()) {
            packet.setAddress(InetAddress.getByName(message.getHost()));
            packet.setPort(message.getPort());
            this.logUdpPacket(CommunicationLog.Direction.OUTBOUND, packet);
        } else {
            if (multicastGroup == null) {
                throw new TransportException(
                        String.format("No transport data in UDP message, which is required as no multicast group is available. Message: %s",
                                message.toString()));
            }
            packet.setAddress(multicastGroup);
            packet.setPort(socketPort);
            this.logUdpPacket(CommunicationLog.Direction.OUTBOUND, packet);
        }

        sendMessageWithRetry(packet);
    }

    private void sendMessageWithRetry(DatagramPacket packet) throws IOException {
        outgoingSocket.send(packet);

        // Retransmission algorithm as defined in
        // http://docs.oasis-open.org/ws-dd/soapoverudp/1.1/os/wsdd-soapoverudp-1.1-spec-os.docx
        int udpMinDelay = (int) DpwsConstants.UDP_MIN_DELAY.toMillis();
        int udpMaxDelay = (int) DpwsConstants.UDP_MAX_DELAY.toMillis();
        int udpUpperDelay = (int) DpwsConstants.UDP_UPPER_DELAY.toMillis();
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
        if (this.isRunning()) {
            return makeStringRepresentation();
        } else {
            return String.format("[%s]", networkInterface);
        }
    }

    private String makeStringRepresentation() {
        String multicast = "w/o multicast";
        if (multicastSocket != null) {
            multicast = String.format("w/ multicast joined at %s:%s", multicastGroup.getHostName(), socketPort);
        }

        return String.format("[%s:[%s|%s] %s]",
                networkInterfaceAddress.toString(),
                incomingSocket.getLocalPort(),
                outgoingSocket.getLocalPort(),
                multicast);
    }

    private void logUdpPacket(CommunicationLog.Direction direction, DatagramPacket packet) {

        var requestTransportInfo = new TransportInfo(
                DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                outgoingSocket.getLocalAddress().getHostName(),
                outgoingSocket.getPort(),
                packet.getAddress().getHostAddress(),
                packet.getPort(),
                Collections.emptyList()
        );

        // no UDP specialization, create ApplicationInfo
        var requestCommContext = new CommunicationContext(new ApplicationInfo(), requestTransportInfo);

        try (ByteArrayInputStream messageData = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())) {
            communicationLog.logMessage(
                    direction,
                    CommunicationLog.TransportType.UDP,
                    requestCommContext,
                    messageData
            );
        } catch (IOException e) {
            LOG.warn("Could not log udp message though the communication log", e);
        }

    }
}
