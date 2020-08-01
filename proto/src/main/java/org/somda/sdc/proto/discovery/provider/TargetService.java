package org.somda.sdc.proto.discovery.provider;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.udp.UdpMessageQueueObserver;

import java.util.Collection;

public interface TargetService extends Service, UdpMessageQueueObserver {
    void updateScopes(Collection<String> scopes);
    void updateXAddrs(Collection<String> xAddrs);
}
