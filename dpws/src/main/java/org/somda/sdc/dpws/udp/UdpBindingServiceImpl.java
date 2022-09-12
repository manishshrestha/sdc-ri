package org.somda.sdc.dpws.udp;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.network.NetworkInterfaceUtil;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.TransportException;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
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
    private final Logger instanceLogger;
    private Thread receiveSocketRunner;
    private Thread sendReceiveSocketRunner;

    private MulticastSocket receiveSocket;
    private MulticastSocket sendReceiveSocket;

    private final int maxMessageSize;
    private final int multicastTtl;
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
                          @Named(DpwsConfig.MULTICAST_TTL) Integer multicastTtl,
                          NetworkInterfaceUtil networkInterfaceUtil,
                          CommunicationLogFactory communicationLogFactory,
                          @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.networkInterface = networkInterface;
        this.multicastGroup = multicastGroup;
        this.socketPort = multicastPort;
        this.maxMessageSize = maxMessageSize;
        this.multicastTtl = multicastTtl;
        this.networkInterfaceUtil = networkInterfaceUtil;
        this.communicationLog = communicationLogFactory.createCommunicationLog();
        this.multicastAddress = new InetSocketAddress(multicastGroup, socketPort);
        this.networkInterfaceAddress = null;
    }

    @Override
    protected void startUp() throws Exception {
        instanceLogger.info("Start UDP binding on network interface {} with modified source", this);
        // try to get first available address from network interface
        networkInterfaceAddress = networkInterfaceUtil.getFirstIpV4Address(networkInterface).orElseThrow(() ->
                new SocketException(String.format("Could not retrieve network interface address from: %s",
                        networkInterface)));

        instanceLogger.info("Bind to address {}", networkInterfaceAddress);

        sendReceiveSocket = new MulticastSocket(0);
        sendReceiveSocket.setNetworkInterface(networkInterface);
        sendReceiveSocket.setTimeToLive(multicastTtl);
        instanceLogger.info("Outgoing socket at {} is open", sendReceiveSocket.getLocalSocketAddress());

        if (multicastGroup == null) {
            receiveSocket = new MulticastSocket(0);
            receiveSocket.setNetworkInterface(networkInterface);
            instanceLogger.info("Incoming socket is open: {}", receiveSocket.getLocalSocketAddress());

        } else {
            if (!multicastGroup.isMulticastAddress()) {
                throw new Exception(String.format("Given address is not a multicast address: %s", multicastGroup));
            }

            receiveSocket = new MulticastSocket(socketPort);
            instanceLogger.info("Join to UDP multicast address group {}", multicastAddress);
            receiveSocket.joinGroup(multicastAddress, networkInterface);
        }

        if (receiver == null) {
            instanceLogger.info("No data receiver configured; ignore incoming UDP messages");
        } else {
            instanceLogger.info("Data receiver configured; process incoming UDP messages");

            // Socket to receive any incoming multicast traffic
            this.receiveSocketRunner = new Thread(() -> {
                while (!receiveSocketRunner.isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(new byte[maxMessageSize], maxMessageSize);
                    try {
                        receiveSocket.receive(packet);
                    } catch (IOException e) {
                        instanceLogger.trace("Could not process UDP packet. Discard.");
                        continue;
                    }

                    var ctxt = new CommunicationContext(
                            new ApplicationInfo(),
                            new TransportInfo(
                                    DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                                    receiveSocket.getLocalAddress().getHostAddress(),
                                    receiveSocket.getLocalPort(),
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
            this.sendReceiveSocketRunner = new Thread(() -> {
                while (!sendReceiveSocketRunner.isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(new byte[maxMessageSize], maxMessageSize);
                    try {
                        sendReceiveSocket.receive(packet);
                    } catch (IOException e) {
                        instanceLogger.trace("Could not process UDP packet. Discard.");
                        continue;
                    }

                    var ctxt = new CommunicationContext(
                            new ApplicationInfo(),
                            new TransportInfo(
                                    DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                                    sendReceiveSocket.getLocalAddress().getHostAddress(),
                                    sendReceiveSocket.getLocalPort(),
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

            receiveSocketRunner.start();
            sendReceiveSocketRunner.start();
        }

        // wait for the sockets, the IGMP join and the worker threads to be available
        // this has primarily been an issue in low performance environments such as the CI
        Thread.sleep(1000);

        instanceLogger.info("UDP binding {} is running", this);
    }

    @Override
    protected void shutDown() throws Exception {
        instanceLogger.info("Shut down UDP binding {}", this);
        receiveSocketRunner.interrupt();
        sendReceiveSocketRunner.interrupt();
        if (multicastGroup != null && multicastAddress != null) {
            receiveSocket.leaveGroup(multicastAddress, networkInterface);
        }
        receiveSocket.close();
        sendReceiveSocket.close();
        instanceLogger.info("UDP binding {} shut down", this);
    }

    @Override
    public void setMessageReceiver(UdpMessageReceiverCallback receiver) {
        this.receiver = receiver;
    }

    @Override
    public void sendMessage(UdpMessage message) throws IOException, TransportException {
        if (!isRunning()) {
            instanceLogger.warn("Try to send message, but service is not running. Skip.");
            return;
        }
        if (message.getLength() > maxMessageSize) {
            String msg = String.format("Exceed maximum UDP message size. Try to write %d Bytes, " +
                            "but only %d Bytes allowed.",
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
                        String.format("No transport data in UDP message, which is required as no multicast group " +
                                        "is available. Message: %s",
                                message.toString()));
            }
            packet.setAddress(multicastGroup);
            packet.setPort(socketPort);
            this.logUdpPacket(CommunicationLog.Direction.OUTBOUND, packet);
        }

        sendMessageWithRetry(packet);
    }

    private void sendMessageWithRetry(DatagramPacket packet) throws IOException {
        sendReceiveSocket.send(packet);

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
                instanceLogger.info("Thread interrupted");
                break;
            }

            sendReceiveSocket.send(packet);

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
        if (receiveSocket != null) {
            multicast = String.format("w/ multicast joined at %s:%s", multicastGroup.getHostAddress(), socketPort);
        }

        return String.format("[%s:[%s|%s] %s]",
                networkInterfaceAddress.toString(),
                receiveSocket.getLocalPort(),
                sendReceiveSocket.getLocalPort(),
                multicast);
    }

    private void logUdpPacket(CommunicationLog.Direction direction, DatagramPacket packet) {

        var requestTransportInfo = new TransportInfo(
                DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                sendReceiveSocket.getLocalAddress().getHostName(),
                sendReceiveSocket.getPort(),
                packet.getAddress().getHostAddress(),
                packet.getPort(),
                Collections.emptyList()
        );

        // no UDP specialization, create ApplicationInfo
        var requestCommContext = new CommunicationContext(new ApplicationInfo(), requestTransportInfo);

        try (ByteArrayInputStream messageData =
                     new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())) {
            communicationLog.logMessage(
                    direction,
                    CommunicationLog.TransportType.UDP,
                    CommunicationLog.MessageType.UNKNOWN,
                    requestCommContext,
                    messageData
            );
        } catch (IOException e) {
            instanceLogger.warn("Could not log udp message though the communication log", e);
        }

    }
}
