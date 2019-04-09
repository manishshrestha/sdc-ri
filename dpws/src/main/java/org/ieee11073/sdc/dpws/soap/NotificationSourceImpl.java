package org.ieee11073.sdc.dpws.soap;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationSourceImpl implements NotificationSource {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationSourceImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final NotificationCallback networkCallback;
    private final ClientHelper clientHelper;

    @Inject
    NotificationSourceImpl(@Assisted NotificationCallback networkCallback,
                           ClientHelper clientHelper,
                           InterceptorRegistry interceptorRegistry,
                           WsAddressingClientInterceptor wsaClientInterceptor) {
        this.networkCallback = networkCallback;
        this.clientHelper = clientHelper;
        this.interceptorRegistry = interceptorRegistry;

        register(wsaClientInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public InterceptorResult sendNotification(SoapMessage notification) throws MarshallingException, TransportException {
        NotificationObject nObj = new NotificationObject(notification);
        InterceptorResult ir = clientHelper.invokeDispatcher(Direction.NOTIFICATION, interceptorRegistry,
                notification, nObj);
        if (ir == InterceptorResult.CANCEL) {
            return InterceptorResult.CANCEL;
        }

        networkCallback.onNotification(notification);
        return InterceptorResult.PROCEED;
    }
}
