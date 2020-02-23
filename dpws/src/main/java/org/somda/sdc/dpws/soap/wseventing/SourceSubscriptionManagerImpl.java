package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionManagerBase;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Default implementation of {@link SourceSubscriptionManager}.
 */
public class SourceSubscriptionManagerImpl extends AbstractExecutionThreadService implements SourceSubscriptionManager {
    private static final Logger LOG = LoggerFactory.getLogger(SourceSubscriptionManagerImpl.class);

    private final BlockingQueue<QueueItem> notificationQueue;
    private final SubscriptionManagerBase delegate;
    private final NotificationSourceFactory notificationSourceFactory;
    private final TransportBindingFactory transportBindingFactory;
    private final WsAddressingUtil wsaUtil;
    private final ExecutorWrapperService<ListeningExecutorService> networkJobExecutor;

    private NotificationSource notifyToSender;
    private NotificationSource endToSender;

    private String subscriptionId;
    private String notifyToUri;

    @AssistedInject
    SourceSubscriptionManagerImpl(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                  @Assisted Duration expires,
                                  @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                  @Assisted("EntTo") @Nullable EndpointReferenceType endTo,
                                  @Assisted("SubscriptionId") String subscriptionId,
                                  @Named(WsEventingConfig.NOTIFICATION_QUEUE_CAPACITY) Integer notificationQueueCapacity,
                                  NotificationSourceFactory notificationSourceFactory,
                                  TransportBindingFactory transportBindingFactory,
                                  WsAddressingUtil wsaUtil,
                                  @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> networkJobExecutor) {
        this.notificationSourceFactory = notificationSourceFactory;
        this.transportBindingFactory = transportBindingFactory;
        this.wsaUtil = wsaUtil;
        this.networkJobExecutor = networkJobExecutor;
        this.subscriptionId = UUID.randomUUID().toString();
        this.delegate = new SubscriptionManagerBase(notifyTo, endTo, subscriptionId, expires, subscriptionManagerEpr);
        this.notificationQueue = new ArrayBlockingQueue<>(notificationQueueCapacity);

        this.notifyToSender = null;
        this.endToSender = null;
        this.notifyToUri = "";
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
    public void renew(Duration expires) {
        delegate.renew(expires);
    }

    @Override
    public void offerNotification(Notification notification) {
        if (!isRunning()) {
            return;
        }
        if (!notificationQueue.offer(new QueueItem(notification))) {
            stopAsync().awaitTerminated();
        }
    }

    @Override
    public void sendToEndTo(SoapMessage endToMessage) {
        if (endToSender == null) {
            return;
        }

        networkJobExecutor.get().submit(() -> {
            try {
                endToSender.sendNotification(endToMessage);
            } catch (Exception e) {
                LOG.info("End-to message could not be delivered.", e);
            }
        });
    }

    @Override
    protected void startUp() {
        notifyToUri = wsaUtil.getAddressUriAsString(getNotifyTo()).orElseThrow(() ->
                new RuntimeException("Invalid notify-to EPR"));
        this.notifyToSender = notificationSourceFactory.createNotificationSource(
                transportBindingFactory.createTransportBinding(URI.create(notifyToUri)));

        if (getEndTo().isPresent()) {
            final Optional<String> addressUriAsString = wsaUtil.getAddressUriAsString(getEndTo().get());
            if (addressUriAsString.isPresent()) {
                this.endToSender = notificationSourceFactory.createNotificationSource(
                        transportBindingFactory.createTransportBinding(URI.create(notifyToUri)));
            }
        }

        subscriptionId = wsaUtil.getAddressUriAsString(delegate.getSubscriptionManagerEpr()).orElseThrow(() ->
                new NoSuchElementException("Subscription manager id could not be resolved"));

        LOG.info("Source subscription manager '{}' started. Start delivering notifications to '{}'",
                subscriptionId, notifyToUri);
    }

    @Override
    protected void run() {
        while (isRunning()) {
            try {
                final QueueItem queueItem = notificationQueue.take();
                if (queueItem instanceof QueueShutDownItem) {
                    LOG.info("Source subscription manager '{}' received stop signal and is about to shut down", subscriptionId);
                    break;
                }
                LOG.debug("Sending notification to {} - {}", notifyToUri, queueItem.getNotification().getPayload());
                notifyToSender.sendNotification(queueItem.getNotification().getPayload());
            } catch (Exception e) {
                LOG.info("Source subscription manager '{}' ended unexpectedly", subscriptionId);
                LOG.trace("Source subscription manager '{}' ended unexpectedly", subscriptionId, e);
                break;
            }
        }
    }

    @Override
    protected void shutDown() {
        LOG.info("Source subscription manager '{}' shut down. Delivery to '{}' stopped.",
                subscriptionId, notifyToUri);
    }

    @Override
    protected void triggerShutdown() {
        notificationQueue.clear();
        notificationQueue.offer(new QueueShutDownItem());
    }

    private static class QueueItem {
        public Notification notification;

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