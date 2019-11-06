package org.somda.sdc.dpws.soap.wstransfer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;

/**
 * Default implementation of {@link TransferGetClient}.
 */
public class TransferGetClientImpl implements TransferGetClient {
    private final ListeningExecutorService executorService;
    private final SoapUtil soapUtil;

    @Inject
    TransferGetClientImpl(@NetworkJobThreadPool ListeningExecutorService executorService,
                          SoapUtil soapUtil) {
        this.executorService = executorService;
        this.soapUtil = soapUtil;
    }

    @Override
    public ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo) {
        return executorService.submit(() -> requestResponseClient.sendRequestResponse(
                soapUtil.createMessage(WsTransferConstants.WSA_ACTION_GET, wsaTo)));
    }
}
