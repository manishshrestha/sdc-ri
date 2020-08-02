package org.somda.sdc.proto.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;

import java.net.URI;

public class RemoteDeviceDisconnectedMessage extends AbstractEventMessage<URI> {
    protected RemoteDeviceDisconnectedMessage(URI payload) {
        super(payload);
    }
}
