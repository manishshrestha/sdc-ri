package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;

/**
 * Subscription manager interface that is used by event sources.
 */
public interface SourceSubscriptionManager extends SubscriptionManager, Service {

    /**
     * Inserts the notification into the subscription manager's queue.
     * <p>
     * The manager is shut down
     * <ul>
     * <li>on first delivery failure or
     * <li>in case there is queue overflow or a delivery failure.
     * </ul>
     *
     * @param notification the notification to add.
     */
    void offerNotification(Notification notification);

    /**
     * Tries to send an end-to message to the event sink.
     * <p>
     * This is a non-blocking call that silently ignores failed delivery.
     *
     * @param endToMessage the message to send. This message is supposed to be a valid end-to message.
     */
    void sendToEndTo(SoapMessage endToMessage);
}
