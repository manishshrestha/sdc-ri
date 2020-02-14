package org.somda.sdc.dpws.udp;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Default implementation of {@linkplain UdpMessageQueueService}.
 */
public class UdpMessageQueueServiceImpl extends AbstractIdleService implements Service, UdpMessageQueueService {
    private final static Logger LOG = LoggerFactory.getLogger(UdpMessageQueueServiceImpl.class);
    private static int instanceIdCounter = 0;
    private final int instanceId;
    private final LinkedBlockingDeque<UdpMessage> incomingMessageQueue;
    private final LinkedBlockingDeque<UdpMessage> outgoingMessageQueue;
    private final EventBus eventBus;

    private UdpBindingService udpBinding;
    private Thread outgoingThread;
    private Thread incomingThread;

    @Inject
    UdpMessageQueueServiceImpl(EventBus eventBus) {
        this.instanceId = instanceIdCounter++;
        this.incomingMessageQueue = new LinkedBlockingDeque<>();
        this.outgoingMessageQueue = new LinkedBlockingDeque<>();
        this.eventBus = eventBus;
        this.udpBinding = null;
    }

    @Override
    public void setUdpBinding(UdpBindingService udpBinding) {
        this.udpBinding = udpBinding;
    }

    @Override
    public boolean sendMessage(UdpMessage message) {
        return isRunning() && outgoingMessageQueue.offer(message);
    }

    @Override
    public void registerUdpMessageQueueObserver(UdpMessageQueueObserver observer) {
        eventBus.register(observer);
    }

    @Override
    public void unregisterUdpMessageQueueObserver(UdpMessageQueueObserver observer) {
        eventBus.unregister(observer);
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("[{}] Start UDP message queue for binding {}", instanceId, udpBinding);
        if (udpBinding == null) {
            String msg = "Cannot startup without UDP binding";
            LOG.warn("[{}] {}", instanceId, msg);
            throw new Exception(msg);
        }

        startProcessingOfIncomingMessages();
        startProcessingOfOutgoingMessages();

        LOG.info("[{}] UDP message queue for binding {} is running", instanceId, udpBinding);
    }

    private void startProcessingOfOutgoingMessages() {
        outgoingThread = new Thread(() -> {
            try {
                do {
                    try {
                        UdpMessage message = outgoingMessageQueue.take();
                        LOG.trace("[{}] Outgoing UdpMessageQueueService received UDP message, sending: {}", instanceId, message);
                        udpBinding.sendMessage(message);
                    } catch (IOException e) {
                        LOG.warn("[{}] Outgoing UdpMessageQueueService IO exception caught.", instanceId, e);
                    } catch (InterruptedException e) {
                        LOG.info("[{}] Outgoing UdpMessageQueueService interrupted.", instanceId, e);
                        break;
                    }
                } while (true);
            } finally {
            LOG.error("[{}] Outgoing UdpMessageQueueService ended!", instanceId);
            }
        });
        outgoingThread.setName(String.format("[%s] Outgoing UdpMessageQueueService", instanceId));
        outgoingThread.setDaemon(true);
        outgoingThread.start();
    }

    private void startProcessingOfIncomingMessages() {
        incomingThread = new Thread(() -> {
            try {
                do {
                    try {
                        UdpMessage message = incomingMessageQueue.take();
                        LOG.trace("[{}] Incoming UdpMessageQueueService received UDP message, posting to EventBus: {}", instanceId, message);
                        eventBus.post(message);
                    } catch (InterruptedException e) {
                        LOG.info("[{}] Incoming UdpMessageQueueService interrupted.", instanceId, e);
                        break;
                    } catch (Exception e) {
                        LOG.warn("[{}] Incoming UdpMessageQueueService encountered an error on event dissemination", instanceId, e);
                    }
                } while (true);
            } finally {
                LOG.error("[{}] Incoming UdpMessageQueueService ended!", instanceId);
            }
        });
        incomingThread.setName(String.format("[%s] Incoming UdpMessageQueueService", instanceId));
        incomingThread.setDaemon(true);
        incomingThread.start();
    }

    @Override
    protected void shutDown() {
        LOG.info("[{}] Shut down UDP message queue for binding {}", instanceId, udpBinding);
        incomingMessageQueue.clear();
        outgoingMessageQueue.clear();
        incomingThread.interrupt();
        outgoingThread.interrupt();
        LOG.info("[{}] UDP message queue for binding {} shut down", instanceId, udpBinding);
    }

    @Override
    public void receive(UdpMessage udpMessage) {
        LOG.debug("[{}] Received UDP message, adding to queue.", instanceId);
        if (!incomingMessageQueue.offer(udpMessage)) {
            LOG.error("[{}] Lost incoming UDP message in message queue. {}", instanceId, udpMessage);
        }
    }
}
