package org.somda.sdc.dpws.device.helper;

import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.guice.AppDelayExecutor;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapDebug;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueObserver;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Message processor that receives and sends WS-Discovery SOAP messages via UDP at the device side.
 * <p>
 * To receive {@link UdpMessage} instances, {@linkplain DiscoveryDeviceUdpMessageProcessor} needs to be registered at a
 * {@link UdpMessageQueueService} by using
 * {@link UdpMessageQueueService#registerUdpMessageQueueObserver(UdpMessageQueueObserver)}.
 */
public class DiscoveryDeviceUdpMessageProcessor implements UdpMessageQueueObserver {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryDeviceUdpMessageProcessor.class);

    private final RequestResponseServer requestResponseServer;
    private final UdpMessageQueueService udpMessageQueueService;
    private final MarshallingService marshallingService;
    private final SoapUtil soapUtil;
    private final ExecutorWrapperService<ScheduledExecutorService> scheduledExecutorService;
    private final Random randomNumbers;

    @AssistedInject
    DiscoveryDeviceUdpMessageProcessor(@Assisted RequestResponseServer requestResponseServer,
                                       @Assisted UdpMessageQueueService udpMessageQueueService,
                                       MarshallingService marshallingService,
                                       SoapUtil soapUtil,
                                       @AppDelayExecutor ExecutorWrapperService<ScheduledExecutorService> scheduledExecutorService,
                                       Random randomNumbers) {
        this.requestResponseServer = requestResponseServer;
        this.udpMessageQueueService = udpMessageQueueService;
        this.marshallingService = marshallingService;
        this.soapUtil = soapUtil;
        this.scheduledExecutorService = scheduledExecutorService;
        this.randomNumbers = randomNumbers;
    }

    @Subscribe
    private void receiveUdpMessage(UdpMessage msg) {
        LOG.trace("Receive UDP message called with message: {}", msg);
        SoapMessage response = soapUtil.createMessage();
        SoapMessage request;

        // Unmarshal SOAP request message
        try {
            request = marshallingService.unmarshal(new ByteArrayInputStream(msg.getData(), 0, msg.getLength()));
        } catch (MarshallingException e) {
            LOG.warn("Incoming UDP message could not be unmarshalled. Message Bytes: {}", msg.toString());
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Incoming SOAP/UDP message: {}", SoapDebug.get(request));
        }

        // Forward SOAP message to given request response interceptor chain
        try {
            requestResponseServer.receiveRequestResponse(request, response, msg.getCommunicationContext());
        } catch (SoapFaultException e) {
            LOG.debug("SOAP fault thrown [{}]", e.getMessage());
            return;
        }

        // TODO: Workaround for Providers responding to Hello messages with empty messages, see #87
        //  Remove once proper UDP notification handling is in place.
        var action = response.getWsAddressingHeader().getAction();
        if (action.isEmpty() || action.get().getValue().isBlank()) {
            LOG.debug("Not sending a response, no response with an action generated for message {}", SoapDebug.get(request));
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Outgoing SOAP/UDP message: {}", SoapDebug.get(response));
        }

        // Marshal SOAP response message
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            marshallingService.marshal(response, os);
        } catch (MarshallingException e) {
            LOG.warn("WS-Discovery response could not be created. Reason: {}", e.getMessage());
            return;
        }

        // Create UDP message to send
        // Wait random time between 0 and MAX_APP_DELAY seconds
        byte[] bytes = os.toByteArray();

        int wait = randomNumbers.nextInt((int) WsDiscoveryConstants.APP_MAX_DELAY.toMillis() + 1);
        scheduledExecutorService.get().schedule(
                () -> {
                    var ctxt = new CommunicationContext(
                            new ApplicationInfo(),
                            new TransportInfo(
                                    DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                                    null, null,
                                    msg.getHost(), msg.getPort(),
                                    Collections.emptyList()
                            )
                    );
                    udpMessageQueueService.sendMessage(new UdpMessage(bytes, bytes.length, ctxt));
                },
                wait, TimeUnit.MILLISECONDS);
    }
}
