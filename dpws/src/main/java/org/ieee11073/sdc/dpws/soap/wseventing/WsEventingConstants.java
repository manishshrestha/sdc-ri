package org.ieee11073.sdc.dpws.soap.wseventing;

import javax.xml.namespace.QName;

/**
 * WS-Eventing constants.
 */
public class WsEventingConstants {
    /**
     * Package that includes all JAXB generated WS-Eventing objects.
     */
    public static final String JAXB_CONTEXT_PACKAGE = "org.ieee11073.sdc.dpws.soap.wseventing.model";

    public static final String NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/08/eventing";

    public static final String WSA_ACTION_SUBSCRIBE = NAMESPACE + "/Subscribe";

    public static final String WSA_ACTION_SUBSCRIBE_RESPONSE = NAMESPACE + "/SubscribeResponse";

    public static final String WSA_ACTION_RENEW = NAMESPACE + "/Renew";

    public static final String WSA_ACTION_RENEW_RESPONSE = NAMESPACE + "/RenewResponse";

    public static final String WSA_ACTION_GET_STATUS = NAMESPACE + "/GetStatus";

    public static final String WSA_ACTION_GET_STATUS_RESPONSE = NAMESPACE + "/GetStatusResponse";

    public static final String WSA_ACTION_UNSUBSCRIBE = NAMESPACE + "/Unsubscribe";

    public static final String WSA_ACTION_UNSUBSCRIBE_RESPONSE = NAMESPACE + "/UnsubscribeResponse";

    public static final String WSA_ACTION_SUBSCRIPTION_END = NAMESPACE + "/SubscriptionEnd";

    public static final String SUPPORTED_DELIVERY_MODE = NAMESPACE + "/DeliveryModes/Push";

    public static final QName DELIVERY_MODE_REQUESTED_UNAVAILABLE = new QName(NAMESPACE, "DeliveryModeRequestedUnavailable");
    public static final QName INVALID_EXPIRATION_TIME = new QName(NAMESPACE, "InvalidExpirationTime");
    public static final QName UNSUPPORTED_EXPIRATION_TYPE = new QName(NAMESPACE, "UnsupportedExpirationType");
    public static final QName FILTERING_NOT_SUPPORTED = new QName(NAMESPACE, "FilteringNotSupported");
    public static final QName FILTERING_REQUESTED_UNAVAILABLE = new QName(NAMESPACE, "FilteringRequestedUnavailable");
    public static final QName EVENT_SOURCE_UNABLE_TO_PROCESS = new QName(NAMESPACE, "EventSourceUnableToProcess");
    public static final QName UNABLE_TO_RENEW = new QName(NAMESPACE, "UnableToRenew");
    public static final QName INVALID_MESSAGE = new QName(NAMESPACE, "InvalidMessage");

    public static final QName NOTIFY_TO = new QName(NAMESPACE, "NotifyTo");

    public static final QName IDENTIFIER = new QName(NAMESPACE, "Identifier");

    public static final String STATUS_SOURCE_DELIVERY_FAILURE = NAMESPACE + "/DeliveryFailure";
    public static final String STATUS_SOURCE_SHUTTING_DOWN = NAMESPACE + "/SourceShuttingDown";
    public static final String STATUS_SOURCE_CANCELLING = NAMESPACE + "/SourceCancelling";
}
