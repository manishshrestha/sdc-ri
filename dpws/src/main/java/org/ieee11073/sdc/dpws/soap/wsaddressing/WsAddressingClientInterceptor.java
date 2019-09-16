package org.ieee11073.sdc.dpws.soap.wsaddressing;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.interception.*;

/**
 * Implements a WS-Addressing client interceptor to apply WS-Addressing header information.
 * <p>
 * The {@linkplain WsAddressingClientInterceptor} creates unique message ids on requests and notifications.
 */
public class WsAddressingClientInterceptor implements Interceptor {

    private final WsAddressingUtil wsaUtil;
    private final SoapUtil soapUtil;

    @Inject
    WsAddressingClientInterceptor(WsAddressingUtil wsaUtil, SoapUtil soapUtil) {
        this.wsaUtil = wsaUtil;
        this.soapUtil = soapUtil;
    }

    @MessageInterceptor(direction = Direction.REQUEST)
    InterceptorResult processMessage(RequestObject rInfo) {
        return processMessage(rInfo.getRequest());
    }

    @MessageInterceptor(direction = Direction.NOTIFICATION)
    InterceptorResult processMessage(NotificationObject nInfo) {
        return processMessage(nInfo.getNotification());
    }

    private InterceptorResult processMessage(SoapMessage msg) {
        processMessageId(msg);
        return InterceptorResult.PROCEED;
    }

    private void processMessageId(SoapMessage msg) {
        if (msg.getWsAddressingHeader().getMessageId().isEmpty()) {
            msg.getWsAddressingHeader().setMessageId(wsaUtil.createAttributedURIType(soapUtil.createRandomUuidUri()));
        }
    }
}
