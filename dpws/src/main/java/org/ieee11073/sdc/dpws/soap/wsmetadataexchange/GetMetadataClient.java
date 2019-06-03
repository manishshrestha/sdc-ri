package org.ieee11073.sdc.dpws.soap.wsmetadataexchange;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.guice.NetworkJobThreadPool;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory;

/**
 * API to send WS-MetadataExchange GetMetadata requests.
 */
public interface GetMetadataClient {
    ListenableFuture<SoapMessage> sendGetMetadata(RequestResponseClient requestResponseClient);
}
