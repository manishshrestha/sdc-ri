package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.HelloType;

/**
 * Message to represent a WS-Discovery Hello.
 */
public class HelloMessage extends AbstractEventMessage<HelloType> {
    public HelloMessage(HelloType payload) {
        super(payload);
    }
}
