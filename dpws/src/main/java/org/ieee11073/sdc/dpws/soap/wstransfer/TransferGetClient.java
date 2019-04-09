package org.ieee11073.sdc.dpws.soap.wstransfer;

import com.google.common.util.concurrent.ListenableFuture;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;

/**
 * Interface to send WS-Transfer Get requests.
 */
public interface TransferGetClient {
    /**
     * Send WS-Transfer Get request.
     *
     * @param requestResponseClient The request response client where to send the request to.
     * @param wsaTo                 WS-Addressing wsa:To field content.
     * @return Requested information as future.
     */
    ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo);
}
