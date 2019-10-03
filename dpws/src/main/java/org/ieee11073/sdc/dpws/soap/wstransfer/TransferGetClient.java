package org.ieee11073.sdc.dpws.soap.wstransfer;

import com.google.common.util.concurrent.ListenableFuture;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;

/**
 * Interface to send WS-Transfer Get requests.
 */
public interface TransferGetClient {
    /**
     * Sends a WS-Transfer Get request.
     *
     * @param requestResponseClient the request response client where to send the request to.
     * @param wsaTo                 WS-Addressing wsa:To field content.
     * @return a future object that in case of a success includes the SOAP response message or throws
     * <ul>
     * <li>{@link org.ieee11073.sdc.dpws.soap.exception.SoapFaultException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.exception.MarshallingException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.exception.TransportException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo);
}
