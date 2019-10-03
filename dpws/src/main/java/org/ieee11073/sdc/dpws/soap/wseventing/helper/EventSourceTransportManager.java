package org.ieee11073.sdc.dpws.soap.wseventing.helper;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.guice.NetworkJobThreadPool;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.WsEventingConfig;
import org.ieee11073.sdc.dpws.soap.wseventing.event.SubscriptionAddedMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.event.SubscriptionRemovedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to manage sending of event messages to notify-to and end-to destinations.
 */
public class EventSourceTransportManager {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceTransportManager.class);

    private final Map<String, NotificationSource> endToSources;
    private final Map<String, NotificationSource> notifyToSources;
    private final NotificationSourceFactory notificationSourceFactory;
    private final TransportBindingFactory transportBindingFactory;
    private final WsAddressingUtil wsaUtil;
    private final Integer maxRetriesOnDeliveryFailure;
    private final ListeningExecutorService networkJobExecutor;

    @Inject
    EventSourceTransportManager(@Named(WsEventingConfig.SOURCE_MAX_RETRIES_ON_DELIVERY_FAILURE) Integer maxRetriesOnDeliveryFailure,
                                NotificationSourceFactory notificationSourceFactory,
                                TransportBindingFactory transportBindingFactory,
                                WsAddressingUtil wsaUtil,
                                @NetworkJobThreadPool ListeningExecutorService networkJobExecutor) {
        this.maxRetriesOnDeliveryFailure = maxRetriesOnDeliveryFailure;
        this.networkJobExecutor = networkJobExecutor;
        this.endToSources = new ConcurrentHashMap<>();
        this.notifyToSources = new ConcurrentHashMap<>();
        this.notificationSourceFactory = notificationSourceFactory;
        this.transportBindingFactory = transportBindingFactory;
        this.wsaUtil = wsaUtil;
    }

    /**
     * Tries to send an end-to message to a specific event sink.
     *
     * @param subMan  the subscription manager where to send the end-to message to.
     * @param message the SOAP message to send.
     * @return a future with delivery status; true if delivered, false if not.
     */
    public ListenableFuture<Boolean> sendEndTo(SourceSubscriptionManager subMan, SoapMessage message) {
        String subId = subMan.getSubscriptionId();
        Optional<NotificationSource> notificationSource = Optional.ofNullable(endToSources.get(subId));
        return notificationSource.map(source -> networkJobExecutor.submit(() -> {
            try {
                source.sendNotification(message);
            } catch (Exception e) {
                return false;
            }
            return true;
        })).orElseGet(() -> networkJobExecutor.submit(() -> {
            LOG.info("No sink for end-to messages available. Abort sending an end-to event");
            return false;
        }));
    }

    /**
     * Sends a notify-to message to a specific event sink.
     *
     * @param subMan  the subscription manager where to send the notify-to message to.
     * @param message the SOAP message to send.
     * @return a future with delivery status; true if delivered, false if not.
     */
    public ListenableFuture<Boolean> sendNotifyTo(SourceSubscriptionManager subMan, SoapMessage message) {
        NotificationSource notifSrc = Optional.ofNullable(notifyToSources.get(subMan.getSubscriptionId()))
                .orElseThrow(() -> new RuntimeException("Notification source is missing, but still required"));
        return networkJobExecutor.submit(() -> {
            for (int i = 1; i <= maxRetriesOnDeliveryFailure; ++i) {
                try {
                    notifSrc.sendNotification(message);
                    return true;
                } catch (Exception e) {
                    LOG.debug("Deliver notification to {} failed. Try {} of {}. Reason: {}",
                            subMan.getSubscriptionId(), i, maxRetriesOnDeliveryFailure,
                            e.getMessage());
                }
            }

            subMan.stopAsync().awaitTerminated();
            String messageId = "unknown id";

            return false;
        });
    }

    @Subscribe
    void onAddSubscription(SubscriptionAddedMessage msg) {
        addSubscriptionManager(msg.getPayload());
    }

    @Subscribe
    void onRemoveSubscription(SubscriptionRemovedMessage msg) {
        removeSubscriptionManager(msg.getPayload());
    }

    private void removeSubscriptionManager(SourceSubscriptionManager subMan) {
        notifyToSources.remove(subMan.getSubscriptionId());
        endToSources.remove(subMan.getSubscriptionId());
    }

    private void addSubscriptionManager(SourceSubscriptionManager subMan) {
        String subId = subMan.getSubscriptionId();

        EndpointReferenceType notifyTo = subMan.getNotifyTo();
        wsaUtil.getAddressUriAsString(notifyTo).ifPresent(uri ->
                notifyToSources.put(subId, notificationSourceFactory.createNotificationSource(
                        transportBindingFactory.createTransportBinding(URI.create(uri)))));

        Optional.ofNullable(subMan.getEndTo()).ifPresent(endTo ->
                wsaUtil.getAddressUriAsString(endTo).ifPresent(uri ->
                        endToSources.put(subId, notificationSourceFactory.createNotificationSource(
                                transportBindingFactory.createTransportBinding(URI.create(uri))))));
    }
}
