package org.somda.sdc.proto.provider;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.proto.discovery.provider.TargetService;

/**
 * Adds SDC services to a DPWS device and manages incoming set service requests.
 * <p>
 * The purpose of the {@linkplain SdcDevice} class is to provide SDC data on the network.
 */
public interface SdcDevice extends Service, SdcDeviceContext {
    String getEprAddress();
}
