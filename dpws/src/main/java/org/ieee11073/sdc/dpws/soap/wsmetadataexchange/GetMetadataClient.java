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
public class GetMetadataClient {
    private final ListeningExecutorService executorService;
    private final SoapUtil soapUtil;
    private final ObjectFactory wsmexFactory;

    @Inject
    GetMetadataClient(@NetworkJobThreadPool ListeningExecutorService executorService,
                      SoapUtil soapUtil,
                      ObjectFactory wsmexFactory) {
        this.executorService = executorService;
        this.soapUtil = soapUtil;
        this.wsmexFactory = wsmexFactory;
    }

    /**
     * Send GetMetadata request.
     *
     * @param requestResponseClient The request response client where to send the request to.
     * @return Requested information as future.
     */
    public ListenableFuture<SoapMessage> sendGetMetadata(RequestResponseClient requestResponseClient) {
        return executorService.submit(() -> requestResponseClient.sendRequestResponse(
            soapUtil.createMessage(WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_REQUEST,
                    wsmexFactory.createGetMetadata())));
    }
}
