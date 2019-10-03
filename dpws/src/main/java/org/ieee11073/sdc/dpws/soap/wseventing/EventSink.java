package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.ListenableFuture;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

/**
 * Interface to manage WS-Eventing subscriptions.
 */
public interface EventSink {
    /**
     * Sends a Subscribe request.
     *
     * @param actions          the list of operation actions.
     *                         Operation actions typically have the following format:
     *                         #WSDL-TARGET-NAMESPACE/#WSDL-PORT-TYPE-NAME/#OPERATION-NAME
     * @param expires          desired expiration time (the hosted service may decide to grant lesser than this).
     *                         If none is given, the hosting service will take decision.
     * @param notificationSink sink where to deliver notifications.
     * @return a future object that in case of a success includes subscription information or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<SubscribeResult> subscribe(List<String> actions,
                                                @Nullable Duration expires,
                                                NotificationSink notificationSink);

    /**
     * Renews a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, NotificationSink)}.
     * @param expires        the desired new expiration duration.
     * @return a future object that in case of a success includes a granted expires duration or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<Duration> renew(String subscriptionId, Duration expires);

    /**
     * Gets the status of a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, NotificationSink)}.
     * @return a future object that in case of a success includes the remaining subscription time or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<Duration> getStatus(String subscriptionId);

    /**
     * Unsubscribes from a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, NotificationSink)}.
     * @return a future object that in case of a success includes an empty {@linkplain Object} instance or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.ieee11073.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture unsubscribe(String subscriptionId);

    /**
     * Starts trying to automatically renew a subscription shortly before it is about to expire.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, NotificationSink)}.
     */
    void enableAutoRenew(String subscriptionId);

    /**
     * Stops automatic renew if enabled via {@link #enableAutoRenew(String)}.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, NotificationSink)}.
     */
    void disableAutoRenew(String subscriptionId);
}
