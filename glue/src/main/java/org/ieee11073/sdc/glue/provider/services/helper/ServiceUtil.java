package org.ieee11073.sdc.glue.provider.services.helper;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.model.message.AbstractSetResponse;
import org.ieee11073.sdc.biceps.model.message.InvocationInfo;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.glue.provider.sco.InvocationResponse;

public class ServiceUtil {
    private final SoapFaultFactory soapFaultFactory;

    @Inject
    ServiceUtil(SoapFaultFactory soapFaultFactory) {
        this.soapFaultFactory = soapFaultFactory;
    }

    <T> T getRequestObjectAsTypeOrThrow(Object object, Class<T> type) throws SoapFaultException {
        if (type.isAssignableFrom(object.getClass())) {
            return type.cast(object);
        }

        throw new SoapFaultException(soapFaultFactory.createSenderFault(
                String.format("Payload is invalid. Expected %s, but found %s", type.getSimpleName(),
                        object.getClass().getSimpleName())));
    }


}
