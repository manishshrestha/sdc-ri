package org.ieee11073.sdc.dpws.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscribeResult;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

/**
 * Offers access to event sink functionality in accordance with WS-Eventing.
 */
public interface EventSinkAccess {
    /**
     * Conducts a subscribe.
     *
     * @param actions          a list of operation actions to subscribe for.
     * @param expires          the desired expiration time (the hosted service may decide to grant lesser than this).
     *                         If none is given, the hosting service will take decision.
     * @param notificationSink the sink where to deliver notifications.
     * @return a future object that either includes subscription information or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * </ul>
     */
    ListenableFuture<SubscribeResult> subscribe(List<String> actions,
                                                @Nullable Duration expires,
                                                Interceptor notificationSink);

    /**
     * Renews a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     * @param expires        the desired new expiration duration.
     * @return a future object that either includes a granted expires duration or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * </ul>
     */
    ListenableFuture<Duration> renew(String subscriptionId, Duration expires);

    /**
     * Gets the status of a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     * @return a future object that either includes the remaining subscription time or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * </ul>
     */
    ListenableFuture<Duration> getStatus(String subscriptionId);

    /**
     * Unsubscribes from a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     * @return a future object that either delivers an empty object in case of successful unsubscribe or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * </ul>
     */
    ListenableFuture unsubscribe(String subscriptionId);

    /**
     * Start trying to automatically renew a subscription when it is going to expire.
     *
     * @param subscriptionId The subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     */
    void enableAutoRenew(String subscriptionId);

    /**
     * Stop automatic renew, if enabled via {@link #enableAutoRenew(String)}.
     *
     * @param subscriptionId The subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     */
    void disableAutoRenew(String subscriptionId);
}
