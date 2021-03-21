package org.somda.sdc.proto.addressing;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.protosdc.proto.model.addressing.Addressing;

import javax.annotation.Nullable;

public class AddressingUtil {

    private final SoapUtil soapUtil;

    @Inject
    AddressingUtil(SoapUtil soapUtil) {
        this.soapUtil = soapUtil;
    }

    public Addressing assembleAddressing(@Nullable String action,
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

    public Addressing assembleAddressing(@Nullable String action,
                                         @Nullable String to) {
        return assembleAddressing(action, to, null);
    }

    public Addressing assembleAddressing(@Nullable String action) {
        return assembleAddressing(action, null, null);
    }
}
