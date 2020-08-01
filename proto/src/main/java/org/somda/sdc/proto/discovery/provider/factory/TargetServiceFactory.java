package org.somda.sdc.proto.discovery.provider.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.proto.discovery.provider.TargetService;

public interface TargetServiceFactory {
    TargetService create(@Assisted String eprAddress);
}
