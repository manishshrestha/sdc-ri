package org.somda.sdc.proto.consumer.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.proto.consumer.Consumer;
import org.somda.sdc.proto.consumer.SdcRemoteDevice;
import org.somda.sdc.proto.consumer.SdcRemoteDeviceWatchdog;
import org.somda.sdc.proto.consumer.report.ReportProcessor;
import org.somda.sdc.proto.consumer.sco.ScoController;

import javax.annotation.Nullable;

public interface SdcRemoteDeviceFactory {
    SdcRemoteDevice create(@Assisted Consumer consumer,
                           @Assisted RemoteMdibAccess remoteMdibAccess,
                           @Assisted ReportProcessor reportProcessor,
                           @Assisted @Nullable ScoController scoController,
                           @Assisted SdcRemoteDeviceWatchdog watchdog);
}
