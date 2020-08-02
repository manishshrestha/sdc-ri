package org.somda.sdc.proto.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.proto.consumer.SdcRemoteDevice;

public class RemoteDeviceConnectedMessage extends AbstractEventMessage<SdcRemoteDevice> {
    public RemoteDeviceConnectedMessage(SdcRemoteDevice payload) {
        super(payload);
    }
}
