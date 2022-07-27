package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionManagerBase;
import org.somda.sdc.dpws.soap.wseventing.model.FilterType;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wseventing.model.SubscriptionEnd;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Default implementation of {@link SourceSubscriptionManager}.
 */
public class SourceSubscriptionManagerImpl extends AbstractExecutionThreadService implements SourceSubscriptionManager {
    private static final Logger LOG = LogManager.getLogger(SourceSubscriptionManagerImpl.class);

    private final BlockingQueue<QueueItem> notificationQueue;
    private final SubscriptionManagerBase delegate;
    private final NotificationSourceFactory notificationSourceFactory;
    private final TransportBindingFactory transportBindingFactory;
    private final WsAddressingUtil wsaUtil;
    private final ExecutorWrapperService<ListeningExecutorService> networkJobExecutor;
    private final Logger instanceLogger;
    private final ObjectFactory wseFactory;
    private final SoapUtil soapUtil;

    private boolean endToTriggered;

    private NotificationSource notifyToSender;
    private NotificationSource endToSender;

    private String subscriptionId;
    private String notifyToUri;

    @AssistedInject
    SourceSubscriptionManagerImpl(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                  @Assisted Duration expires,
                                  @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                  @Assisted("EndTo") @Nullable EndpointReferenceType endTo,
                                  @Assisted("SubscriptionId") String subscriptionId,
                                  @Assisted("Filter") FilterType filter,
                                  @Named(WsEventingConfig.NOTIFICATION_QUEUE_CAPACITY)
                                          Integer notificationQueueCapacity,
                                  NotificationSourceFactory notificationSourceFactory,
                                  TransportBindingFactory transportBindingFactory,
                                  WsAddressingUtil wsaUtil,
                                  @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService>
                                          networkJobExecutor,
                                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                                  ObjectFactory wseFactory,
                                  SoapUtil soapUtil) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.notificationSourceFactory = notificationSourceFactory;
        this.transportBindingFactory = transportBindingFactory;
        this.wsaUtil = wsaUtil;
        this.networkJobExecutor = networkJobExecutor;
        this.subscriptionId = UUID.randomUUID().toString();
        this.delegate = new SubscriptionManagerBase(
                notifyTo, endTo, subscriptionId, expires, subscriptionManagerEpr, filter);
        this.notificationQueue = new ArrayBlockingQueue<>(notificationQueueCapacity);

        this.notifyToSender = null;
        this.endToSender = null;
        this.notifyToUri = "";

        this.wseFactory = wseFactory;
        this.soapUtil = soapUtil;
    }

    @Override
    public String getSubscriptionId() {
        return delegate.getSubscriptionId();
    }

    @Override
    public LocalDateTime getExpiresTimeout() {
        return delegate.getExpiresTimeout();
    }

    @Override
    public EndpointReferenceType getNotifyTo() {
        return delegate.getNotifyTo();
    }

    @Override
    public Optional<EndpointReferenceType> getEndTo() {
        return delegate.getEndTo();
    }

    @Override
    public Duration getExpires() {
        return delegate.getExpires();
    }

    @Override
    public EndpointReferenceType getSubscriptionManagerEpr() {
        return delegate.getSubscriptionManagerEpr();
    }

    @Override
    public Collection<String> getActions() {
        return delegate.getActions();
    }


    @Override
    public void renew(Duration expires) {
        delegate.renew(expires);
    }

    @Override
    public void offerNotification(Notification notification) {
        if (!isRunning() || endToTriggered) {
            return;
        }
        if (!notificationQueue.offer(new QueueItem(notification))) {
            stopAsync().awaitTerminated();
        }
    }

    @Override
    public void sendToEndTo(SoapMessage endToMessage) {
        if (endToSender == null || !isRunning() || endToTriggered) {
            return;
        }

        endToTriggered = true;

        networkJobExecutor.get().submit(() -> {
            try {
                endToSender.sendNotification(endToMessage);
                // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                instanceLogger.info("End-to message could not be delivered.", e);
            }
        });
    }

    @Override
    protected void startUp() {
        notifyToUri = wsaUtil.getAddressUri(getNotifyTo()).orElseThrow(() ->
                new RuntimeException("Invalid notify-to EPR"));
        this.notifyToSender = notificationSourceFactory.createNotificationSource(
                transportBindingFactory.createTransportBinding(notifyToUri));

        if (getEndTo().isPresent()) {
            var addressUriAsString = wsaUtil.getAddressUri(getEndTo().get());
            addressUriAsString.ifPresent(this::createEndToNotificationSource);
        }

        subscriptionId = wsaUtil.getAddressUri(delegate.getSubscriptionManagerEpr()).orElseThrow(() ->
                new NoSuchElementException("Subscription manager id could not be resolved"));

        instanceLogger.info("Source subscription manager '{}' started. Start delivering notifications to '{}'",
                subscriptionId, notifyToUri);
    }

    private void createEndToNotificationSource(String endToUri) {
        endToSender = notificationSourceFactory.createNotificationSource(
                transportBindingFactory.createTransportBinding(endToUri));
    }

    @Override
    protected void run() {
        while (true) {
            try {
                final QueueItem queueItem = notificationQueue.take();
                if (queueItem instanceof QueueShutDownItem) {
                    instanceLogger.info("Source subscription manager '{}' received stop signal and is about " +
                            "to shut down", subscriptionId);
                    if (!endToTriggered) {
                        sendToEndTo(createForEndTo(WsEventingStatus.STATUS_SOURCE_SHUTTING_DOWN.getUri()));
                    }

                    break;
                }
                instanceLogger.debug("Sending notification to {} - {}", notifyToUri,
                        queueItem.getNotification().getPayload());
                notifyToSender.sendNotification(queueItem.getNotification().getPayload());
                // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                instanceLogger.info("Source subscription manager '{}' ended unexpectedly", subscriptionId);
                instanceLogger.trace("Source subscription manager '{}' ended unexpectedly", subscriptionId, e);
                break;
            }
        }
    }

    @Override
    protected void shutDown() {
        instanceLogger.info("Source subscription manager '{}' shut down. Delivery to '{}' stopped.",
                subscriptionId, notifyToUri);
    }

    @Override
    public void triggerShutdown() {
        notificationQueue.offer(new QueueShutDownItem());
    }

    private SoapMessage createForEndTo(String status) {
        SubscriptionEnd subscriptionEnd = wseFactory.createSubscriptionEnd();
        subscriptionEnd.setSubscriptionManager(getSubscriptionManagerEpr());
        subscriptionEnd.setStatus(status);
        String wsaTo = wsaUtil.getAddressUri(getNotifyTo()).orElse(null);

        SoapMessage msg = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END, subscriptionEnd);
        Optional.ofNullable(wsaTo).ifPresent(to ->
                msg.getWsAddressingHeader().setTo(wsaUtil.createAttributedURIType(to)));

        return msg;
    }

    private static class QueueItem {
        private Notification notification;

        QueueItem(@Nullable Notification notification) {
            this.notification = notification;
        }

        public Notification getNotification() {
            return notification;
        }
    }

    private static class QueueShutDownItem extends QueueItem {
        QueueShutDownItem() {
            super(null);
        }
    }
}