package org.ieee11073.sdc.dpws.soap;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.interception.*;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@linkplain NotificationSink}.
 */
public class NotificationSinkImpl implements NotificationSink {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationSinkImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final ServerDispatcher serverDispatcher;

    @Inject
    NotificationSinkImpl(ServerDispatcher serverDispatcher,
                         InterceptorRegistry interceptorRegistry,
                         WsAddressingServerInterceptor wsaServerInterceptor) {
        this.serverDispatcher = serverDispatcher;
        this.interceptorRegistry = interceptorRegistry;
        register(wsaServerInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public void receiveNotification(SoapMessage notification) {
        NotificationObject nObj = new NotificationObject(notification);
        try {
            serverDispatcher.invokeDispatcher(Direction.NOTIFICATION, interceptorRegistry, notification, nObj);
        } catch (SoapFaultException e) {
            LOG.warn("SoapFaultException shall not be thrown by notifications");
        }
    }
}
