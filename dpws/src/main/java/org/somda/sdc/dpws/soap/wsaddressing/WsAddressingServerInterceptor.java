package org.ieee11073.sdc.dpws.soap.wsaddressing;

import com.google.common.collect.EvictingQueue;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.factory.WsAddressingFaultFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Implements a WS-Addressing server interceptor to check WS-Addressing header information.
 * <p>
 * The {@linkplain WsAddressingServerInterceptor} is in charge of
 * <ul>
 * <li>checking for existing wsa:Action attribute (and cancelling an incoming request in case it is missing) and
 * <li>tracking message ids and reject duplicates.
 * </ul>
 * todo DGr process ReplyTo automatically
 */
public class WsAddressingServerInterceptor implements Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(WsAddressingServerInterceptor.class);

    private final Boolean ignoreMessageIds;
    private final WsAddressingFaultFactory addressingFaultFactory;
    private final WsAddressingUtil wsaUtil;
    private final SoapUtil soapUtil;
    private final EvictingQueue<String> messageIdCache;
    private final SoapFaultFactory soapFaultFactory;

    @Inject
    WsAddressingServerInterceptor(@Named(WsAddressingConfig.MESSAGE_ID_CACHE_SIZE) Integer messageIdCacheSize,
                                  @Named(WsAddressingConfig.IGNORE_MESSAGE_IDS) Boolean ignoreMessageIds,
                                  WsAddressingFaultFactory addressingFaultFactory,
                                  SoapFaultFactory soapFaultFactory,
                                  WsAddressingUtil wsaUtil,
                                  SoapUtil soapUtil) {
        this.messageIdCache = EvictingQueue.create(messageIdCacheSize);
        this.ignoreMessageIds = ignoreMessageIds;
        this.addressingFaultFactory = addressingFaultFactory;
        this.soapFaultFactory = soapFaultFactory;
        this.wsaUtil = wsaUtil;
        this.soapUtil = soapUtil;
    }

    @MessageInterceptor(direction = Direction.REQUEST)
    void processMessage(RequestResponseObject rrInfo) throws SoapFaultException {
        processMessage(rrInfo.getRequest());
            rrInfo.getResponse().getWsAddressingHeader().setRelatesTo(
                    rrInfo.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
            rrInfo.getResponse().getWsAddressingHeader().setMessageId(wsaUtil.createAttributedURIType(
                    soapUtil.createRandomUuidUri()));
    }

    @MessageInterceptor
    void processMessage(NotificationObject nInfo) throws SoapFaultException {
        processMessage(nInfo.getNotification());
    }

    private void processMessage(SoapMessage msg) throws SoapFaultException {
        processAction(msg);
        processMessageId(msg);
    }

    private void processAction(SoapMessage msg) throws SoapFaultException {
        Optional<AttributedURIType> action = msg.getWsAddressingHeader().getAction();

        if (action.isEmpty() || Optional.ofNullable(action.get().getValue()).isEmpty()) {
            throw new SoapFaultException(soapFaultFactory.createSenderFault(
                    "WS-Addressing header 'Action' required, but not given"));
        }

        if (action.get().getValue().isEmpty()) {
            throw new SoapFaultException(soapFaultFactory.createSenderFault(
                    "WS-Addressing header 'Action' given, but empty"));
        }
    }

    // note the synchronized keyword as the server interceptor is shared between different requests in order to
    // facilitate duplicate detection
    private synchronized void processMessageId(SoapMessage msg) throws SoapFaultException {
        if (ignoreMessageIds) {
            return;
        }

        Optional<AttributedURIType> messageId = msg.getWsAddressingHeader().getMessageId();
        if (messageId.isEmpty()) {
            throw new SoapFaultException(
                    addressingFaultFactory.createMessageInformationHeaderRequired(WsAddressingConstants.MESSAGE_ID));
        }

        Optional<String> foundMessageId = messageIdCache.parallelStream()
                .filter(s -> messageId.get().getValue().equals(s))
                .findFirst();
        if (foundMessageId.isPresent()) {
            String actionUri = "unknown action";
            if (msg.getWsAddressingHeader().getAction().isPresent()) {
                actionUri = msg.getWsAddressingHeader().getAction().get().getValue();
            }

            String faultMsg = String.format("Found message duplicate: %s (message: %s). Skip processing.",
                    foundMessageId.get(), actionUri);
            LOG.debug(faultMsg);
            throw new RuntimeException(faultMsg);
        }

        messageIdCache.add(messageId.get().getValue());
    }
}
