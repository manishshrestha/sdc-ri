package org.ieee11073.sdc.dpws.service;

import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.TransportBinding;
import org.ieee11073.sdc.dpws.TransportBindingException;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Transport binding for hosted services that automatically updates transport endpoints on metadata change.
 */
public class HostedServiceTransportBinding implements TransportBinding, HostedServiceMetadataObserver {
    private final HostedServiceProxy hostedServiceProxy;
    private final TransportBindingFactory transportBindingFactory;
    private TransportBinding transportBinding;
    private final Lock transportBindingLock;

    @AssistedInject
    HostedServiceTransportBinding(@Assisted HostedServiceProxy hostedServiceProxy,
                                  TransportBindingFactory transportBindingFactory) {
        this.hostedServiceProxy = hostedServiceProxy;
        this.transportBindingFactory = transportBindingFactory;
        this.transportBindingLock = new ReentrantLock();
        onMetadataChange(null);
    }

    @Override
    public void close() throws IOException {
        // nothing to do here
    }

    @Override
    public void onNotification(SoapMessage notification) throws MarshallingException, TransportException,
            TransportBindingException {
        TransportBinding localTransportBinding;
        transportBindingLock.lock();
        try {
            verifyTransportBindingAvailability();
            localTransportBinding = transportBinding;
        } finally {
            transportBindingLock.unlock();
        }

        localTransportBinding.onNotification(notification);
    }

    @Override
    public SoapMessage onRequestResponse(SoapMessage request) throws SoapFaultException, TransportException,
            TransportBindingException, MarshallingException {
        TransportBinding localTransportBinding;
        transportBindingLock.lock();
        try {
            verifyTransportBindingAvailability();
            localTransportBinding = transportBinding;
        } finally {
            transportBindingLock.unlock();
        }

        return localTransportBinding.onRequestResponse(request);
    }

    private void verifyTransportBindingAvailability() throws TransportBindingException {
        if (transportBinding == null) {
            throw new TransportBindingException(String.format("No transport binding could be established for {}",
                    Arrays.toString(hostedServiceProxy.getType().getEndpointReference().toArray())));
        }
    }

    @Subscribe
    void onMetadataChange(@Nullable HostedServiceMetadataChangeMessage msg) {
        transportBindingLock.lock();
        try {
            transportBinding = null;
            URI activeEprAddress = hostedServiceProxy.getActiveEprAddress();
            transportBinding = transportBindingFactory.createTransportBinding(activeEprAddress);
        } finally {
            transportBindingLock.unlock();
        }
    }
}
