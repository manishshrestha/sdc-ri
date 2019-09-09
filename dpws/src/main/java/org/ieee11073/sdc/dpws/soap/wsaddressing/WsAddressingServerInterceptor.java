package org.ieee11073.sdc.dpws.soap.wsaddressing;

import com.google.common.collect.EvictingQueue;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.wsaddressing.factory.WsAddressingFaultFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * WS-Addressing server side interceptor.
 *
 * - Check for existing wsa:Action attribute and cancels in case of missing
 * - Track message ids and reject message if already found
 *
 * \todo process ReplyTo automatically
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
    InterceptorResult processMessage(RequestResponseObject rrInfo) throws SoapFaultException {
        InterceptorResult interceptorResult = processMessage(rrInfo.getRequest());
        if (interceptorResult == InterceptorResult.PROCEED) {
            rrInfo.getResponse().getWsAddressingHeader().setRelatesTo(
                    rrInfo.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
            rrInfo.getResponse().getWsAddressingHeader().setMessageId(wsaUtil.createAttributedURIType(
                    soapUtil.createRandomUuidUri()));
        }
        return interceptorResult;
    }

    @MessageInterceptor
    InterceptorResult processMessage(NotificationObject nInfo) throws SoapFaultException {
        return processMessage(nInfo.getNotification());
    }

    private InterceptorResult processMessage(SoapMessage msg) throws SoapFaultException {
        processAction(msg);
        if (processMessageId(msg) == InterceptorResult.CANCEL) {
            return InterceptorResult.CANCEL;
        }

        return InterceptorResult.PROCEED;
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

    private synchronized InterceptorResult processMessageId(SoapMessage msg) throws SoapFaultException {
        if (ignoreMessageIds) {
            return InterceptorResult.PROCEED;
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
            return InterceptorResult.CANCEL;
        }

        messageIdCache.add(messageId.get().getValue());
        return InterceptorResult.PROCEED;
    }
}
