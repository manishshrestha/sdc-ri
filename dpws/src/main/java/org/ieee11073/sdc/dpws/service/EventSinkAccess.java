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
 * Class to manage event sinks.
 */
public interface EventSinkAccess {
    /**
     * @param actions          List of operation actions. Operation actions have the following format:
     *                         #WSDL-TARGET-NAMESPACE/#WSDL-PORT-TYPE-NAME/#OPERATION-NAME
     * @param expires          Desired expiration time (the hosted service may decide to grant lesser than this). If
     *                         known is given, the hosting service will take decision.
     * @param notificationSink Sink where to deliver notifications.
     * @return A future object that can throw
     *
     * - {@link SoapFaultException}
     * - {@link MarshallingException}
     * - {@link TransportException}
     *
     * or in case of success includes subscription information.
     */
    ListenableFuture<SubscribeResult> subscribe(List<String> actions,
                                                @Nullable Duration expires,
                                                Interceptor notificationSink);

    /**
     * Renew a subscription.
     *
     * @param subscriptionId The subscription id obtained in the {@link SubscribeResult} of
     * {@link #subscribe(List, Duration, Interceptor)}.
     * @param expires The desired new expiration duration.
     * @return A future object that can throw
     *
     * - {@link SoapFaultException}
     * - {@link MarshallingException}
     * - {@link TransportException}
     *
     * or in case of success includes a granted expires duration.
     */
    ListenableFuture<Duration> renew(String subscriptionId, Duration expires);

    /**
     * Get status of a subscription.
     *
     * @param subscriptionId The subscription id obtained in the {@link SubscribeResult} of
     * {@link #subscribe(List, Duration, Interceptor)}.
     * @return A future object that can throw
     *
     * - {@link SoapFaultException}
     * - {@link MarshallingException}
     * - {@link TransportException}
     *
     * or in case of success includes a duration that gives the remaining subscription time.
     */
    ListenableFuture<Duration> getStatus(String subscriptionId);

    /**
     * Unsubscribe from a subscription.
     *
     * @param subscriptionId The subscription id obtained in the {@link SubscribeResult} of
     * {@link #subscribe(List, Duration, Interceptor)}.
     * @return A future object that can throw
     *
     * - {@link SoapFaultException}
     * - {@link MarshallingException}
     * - {@link TransportException}
     *
     * or deliver an empty {@linkplain Object} instance to signal success.
     */
    ListenableFuture unsubscribe(String subscriptionId);

    /**
     * Start trying to automatically renew a subscription when it is going to expire.
     *
     * @param subscriptionId The subscription id obtained in the {@link SubscribeResult} of
     * {@link #subscribe(List, Duration, Interceptor)}.
     */
    void enableAutoRenew(String subscriptionId);

    /**
     * Stop automatic renew, if enabled via {@link #enableAutoRenew(String)}.
     *
     * @param subscriptionId The subscription id obtained in the {@link SubscribeResult} of
     * {@link #subscribe(List, Duration, Interceptor)}.
     */
    void disableAutoRenew(String subscriptionId);
}