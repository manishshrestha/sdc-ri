package org.somda.sdc.dpws.soap.wsaddressing;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.interception.*;

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
    void processMessage(RequestObject rInfo) {
        processMessage(rInfo.getRequest());
    }

    @MessageInterceptor(direction = Direction.NOTIFICATION)
    void processMessage(NotificationObject nInfo) {
        processMessage(nInfo.getNotification());
    }

    private void processMessage(SoapMessage msg) {
        processMessageId(msg);
    }

    private void processMessageId(SoapMessage msg) {
        if (msg.getWsAddressingHeader().getMessageId().isEmpty()) {
            msg.getWsAddressingHeader().setMessageId(wsaUtil.createAttributedURIType(soapUtil.createRandomUuidUri()));
        }
    }
}
