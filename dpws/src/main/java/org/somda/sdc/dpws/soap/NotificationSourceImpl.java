package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.*;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Default implementation of {@linkplain NotificationSource}.
 */
public class NotificationSourceImpl implements NotificationSource {

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
