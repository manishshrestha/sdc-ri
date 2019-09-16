package org.ieee11073.sdc.dpws.soap.wseventing.factory;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.DpwsConstants;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.ieee11073.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.ieee11073.sdc.dpws.soap.wseventing.model.ObjectFactory;

public class WsEventingFaultFactory {
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory wseFactory;
    private final org.ieee11073.sdc.dpws.soap.model.ObjectFactory soapFactory;

    @Inject
    WsEventingFaultFactory(SoapFaultFactory soapFaultFactory,
                           ObjectFactory wseFactory,
                           org.ieee11073.sdc.dpws.soap.model.ObjectFactory soapFactory) {
        this.soapFaultFactory = soapFaultFactory;
        this.wseFactory = wseFactory;
        this.soapFactory = soapFactory;
    }

    public SoapMessage createDeliveryModeRequestedUnavailable() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.DELIVERY_MODE_REQUESTED_UNAVAILABLE,
                "The requested delivery mode is not supported.",
                wseFactory.createSupportedDeliveryMode(WsEventingConstants.SUPPORTED_DELIVERY_MODE)
        );
    }

    public SoapMessage createInvalidExpirationTime() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.INVALID_EXPIRATION_TIME,
                "The expiration time requested is invalid."
        );
    }

    public SoapMessage createUnsupportedExpirationType() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.UNSUPPORTED_EXPIRATION_TYPE,
                "Only expiration durations are supported."
        );
    }

    public SoapMessage createFilteringNotSupported() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.FILTERING_NOT_SUPPORTED,
                "Filtering is not supported."
        );
    }

    public SoapMessage createFilteringRequestedUnavailable() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.FILTERING_REQUESTED_UNAVAILABLE,
                "The requested filter dialect is not supported",
                wseFactory.createSupportedDialect(DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT)
        );
    }

    public SoapMessage createEventSourceUnableToProcess(String reason) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.EVENT_SOURCE_UNABLE_TO_PROCESS,
                reason
        );
    }

    public SoapMessage createUnableToRenew(String reason) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.RECEIVER,
                WsEventingConstants.UNABLE_TO_RENEW,
                reason
        );
    }

    public SoapMessage createInvalidMessage(String reason, Envelope invalidMessage) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.RECEIVER,
                WsEventingConstants.INVALID_MESSAGE,
                "The message is not valid and cannot be processed.",
                soapFactory.createEnvelope(invalidMessage)
        );
    }
}
