package org.ieee11073.sdc.dpws.soap.wsmetadataexchange;

import com.google.common.util.concurrent.ListenableFuture;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;

/**
 * API to send WS-MetadataExchange GetMetadata requests.
 */
public interface GetMetadataClient {
    ListenableFuture<SoapMessage> sendGetMetadata(RequestResponseClient requestResponseClient);
}
