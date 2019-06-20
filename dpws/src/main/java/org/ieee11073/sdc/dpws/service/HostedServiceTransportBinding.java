package org.ieee11073.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.TransportBinding;
import org.ieee11073.sdc.dpws.TransportBindingException;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

import java.io.IOException;
import java.net.URI;

/**
 * Static transport binding for hosted services.
 */
public class HostedServiceTransportBinding implements TransportBinding {
    private TransportBinding transportBinding;

    @AssistedInject
    HostedServiceTransportBinding(@Assisted HostedServiceProxy hostedServiceProxy,
                                  TransportBindingFactory transportBindingFactory) {
        URI activeEprAddress = hostedServiceProxy.getActiveEprAddress();
        this.transportBinding = transportBindingFactory.createTransportBinding(activeEprAddress);
    }

    @Override
    public void close() throws IOException {
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
