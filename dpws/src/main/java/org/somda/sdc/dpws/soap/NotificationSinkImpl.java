package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorRegistry;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.interception.ServerDispatcher;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;

/**
 * Default implementation of {@linkplain NotificationSink}.
 */
public class NotificationSinkImpl implements NotificationSink {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationSinkImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final ServerDispatcher serverDispatcher;

    @Inject
    NotificationSinkImpl(@Assisted WsAddressingServerInterceptor wsaServerInterceptor,
                         ServerDispatcher serverDispatcher,
                         InterceptorRegistry interceptorRegistry) {
        this.serverDispatcher = serverDispatcher;
        this.interceptorRegistry = interceptorRegistry;
        register(wsaServerInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public void receiveNotification(SoapMessage notification, CommunicationContext communicationContext) {
        NotificationObject nObj = new NotificationObject(notification, communicationContext);
        try {
            serverDispatcher.invokeDispatcher(Direction.NOTIFICATION, interceptorRegistry, notification, nObj);
        } catch (SoapFaultException e) {
            LOG.debug("Caught SoapFaultException on notification is dropped. Message: {}", e.getMessage());
        }
    }
}
