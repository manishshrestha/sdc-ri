package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;

/**
 * Static transport binding for hosted services.
 */
public class HostedServiceTransportBinding implements TransportBinding {
    private final TransportBinding transportBinding;

    @AssistedInject
    HostedServiceTransportBinding(@Assisted HostedServiceProxy hostedServiceProxy,
                                  TransportBindingFactory transportBindingFactory) {
        var activeEprAddress = hostedServiceProxy.getActiveEprAddress();
        this.transportBinding = transportBindingFactory.createTransportBinding(activeEprAddress);
    }

    @Override
    public void close() {
        // nothing to do here
    }

    @Override
    public void onNotification(SoapMessage notification) throws MarshallingException, TransportException,
            TransportBindingException {
        transportBinding.onNotification(notification);
    }

    @Override
    public SoapMessage onRequestResponse(SoapMessage request) throws SoapFaultException, TransportException,
            TransportBindingException, MarshallingException {
        return transportBinding.onRequestResponse(request);
    }
}
