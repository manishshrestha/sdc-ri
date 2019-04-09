package org.ieee11073.sdc.dpws.udp;

import com.google.common.util.concurrent.AbstractIdleService;

import java.io.IOException;

public class MockUdpBindingService extends AbstractIdleService implements UdpBindingService{
    private UdpMessageReceiverCallback receiver;

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Override
    public void setMessageReceiver(UdpMessageReceiverCallback receiver) {
        this.receiver = receiver;
    }

    @Override
    public void sendMessage(UdpMessage message) throws IOException {
        receiver.receive(message);
    }
}
