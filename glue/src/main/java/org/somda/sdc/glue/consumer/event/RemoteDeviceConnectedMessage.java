package org.somda.sdc.glue.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;

public class RemoteDeviceConnectedMessage extends AbstractEventMessage<SdcRemoteDevice> {
    protected RemoteDeviceConnectedMessage(SdcRemoteDevice payload) {
        super(payload);
    }
}
