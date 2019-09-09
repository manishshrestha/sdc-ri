package org.ieee11073.sdc.dpws.soap.wsdiscovery.event;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ByeType;

/**
 * Message to represent a WS-Discovery Bye.
 */
public class ByeMessage extends AbstractEventMessage<ByeType> {
    public ByeMessage(ByeType payload) {
        super(payload);
    }
}
