package org.somda.sdc.proto.discovery.provider.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.proto.addressing.AddressingValidator;
import org.somda.sdc.proto.addressing.MessageDuplicateDetection;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.model.addressing.AddressingTypes;

public interface TargetServiceFactory {
    TargetService create(@Assisted String eprAddress);
}
