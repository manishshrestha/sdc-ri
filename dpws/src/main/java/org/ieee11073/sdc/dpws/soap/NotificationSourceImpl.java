package org.ieee11073.sdc.dpws.soap;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@linkplain NotificationSource}.
 */
public class NotificationSourceImpl implements NotificationSource {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationSourceImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final NotificationCallback networkCallback;
    private final ClientDispatcher clientDispatcher;

    @Inject
    NotificationSourceImpl(@Assisted NotificationCallback networkCallback,
                           ClientDispatcher clientDispatcher,
                           InterceptorRegistry interceptorRegistry,
                           WsAddressingClientInterceptor wsaClientInterceptor) {
        this.networkCallback = networkCallback;
        this.clientDispatcher = clientDispatcher;
        this.interceptorRegistry = interceptorRegistry;

        register(wsaClientInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public void sendNotification(SoapMessage notification) throws MarshallingException, TransportException, InterceptorException {
        NotificationObject nObj = new NotificationObject(notification);
        clientDispatcher.invokeDispatcher(Direction.NOTIFICATION, interceptorRegistry, notification, nObj);
        networkCallback.onNotification(notification);
    }
}
