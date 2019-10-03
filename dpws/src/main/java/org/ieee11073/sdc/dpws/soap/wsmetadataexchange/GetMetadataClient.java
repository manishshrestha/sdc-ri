package org.ieee11073.sdc.dpws.soap.wsmetadataexchange;

import com.google.common.util.concurrent.ListenableFuture;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;

import java.time.Duration;
import java.util.List;

/**
 * API to send WS-MetadataExchange GetMetadata requests.
 */
public interface GetMetadataClient {
    /**
     * Sends a WS-MetadataExchange GetMetadata request.
     *
     * @param requestResponseClient the request-response client that is used to send the request.
     * @return a future object that in case of a success includes the SOAP response message or throws
     * <ul>
     * <li>{@link org.ieee11073.sdc.dpws.soap.exception.SoapFaultException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.exception.MarshallingException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.exception.TransportException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<SoapMessage> sendGetMetadata(RequestResponseClient requestResponseClient);
}