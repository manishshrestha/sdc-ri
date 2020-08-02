package org.somda.sdc.proto.addressing;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.proto.model.addressing.Addressing;
import org.somda.sdc.proto.model.addressing.AddressingTypes;

import javax.annotation.Nullable;

public class AddressingUtil {

    private final SoapUtil soapUtil;

    @Inject
    AddressingUtil(SoapUtil soapUtil) {
        this.soapUtil = soapUtil;
    }

    public Addressing assemblyAddressing(@Nullable String action,
                                         @Nullable String to,
                                         @Nullable String relatesToMessageId) {
        var builder = Addressing.newBuilder()
                .setMessageId(soapUtil.createRandomUuidUri());

        if (action != null && !action.isEmpty()) {
            builder.setAction(action);
        }

        if (to != null && !to.isEmpty()) {
            builder.setTo(to);
        }

        if (relatesToMessageId != null && !relatesToMessageId.isEmpty()) {
            builder.setRelatesId(relatesToMessageId);
        }

        return builder.build();
    }

    public Addressing assemblyAddressing(@Nullable String action,
                                                         @Nullable String to) {
        return assemblyAddressing(action, to, null);
    }

    public Addressing assemblyAddressing(@Nullable String action) {
        return assemblyAddressing(action, null, null);
    }
}
