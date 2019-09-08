package org.ieee11073.sdc.dpws.udp;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

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
        LOG.info("Start UDP message queue for binding {}", udpBinding);
        if (udpBinding == null) {
            String msg = "Cannot startup without UDP binding";
            LOG.warn(msg);
            throw new Exception(msg);
        }

        startProcessingOfIncomingMessages();
        startProcessingOfOutgoingMessages();

        LOG.info("UDP message queue for binding {} is running", udpBinding);
    }

    private void startProcessingOfOutgoingMessages() {
        outgoingThread = new Thread(() -> {
            do {
                try {
                    UdpMessage message = outgoingMessageQueue.take();
                    udpBinding.sendMessage(message);
                } catch (IOException e) {
                    LOG.warn("IO exception caught: {}", e.getMessage());
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    break;
                }
            } while (isRunning());
        });
        outgoingThread.setName(String.format("[%s] Outgoing UdpMessageQueueService", Integer.toString(instanceId)));
        outgoingThread.start();
    }

    private void startProcessingOfIncomingMessages() {
        incomingThread = new Thread(() -> {
            do {
                try {
                    UdpMessage message = incomingMessageQueue.take();
                    eventBus.post(message);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOG.info("Error on event dissemination", e);
                }

            } while (isRunning());
        });
        incomingThread.setName(String.format("[%s] Incoming UdpMessageQueueService", Integer.toString(instanceId)));
        incomingThread.start();
    }

    @Override
    protected void shutDown() {
        LOG.info("Shut down UDP message queue for binding {}", udpBinding);
        incomingMessageQueue.clear();
        outgoingMessageQueue.clear();
        incomingThread.interrupt();
        outgoingThread.interrupt();
        LOG.info("UDP message queue for binding {} shut down", udpBinding);
    }

    @Override
    public void receive(UdpMessage udpMessage) {
        if (!incomingMessageQueue.offer(udpMessage)) {
            LOG.info("Lost incoming UDP message in message queue");
        }
    }
}
