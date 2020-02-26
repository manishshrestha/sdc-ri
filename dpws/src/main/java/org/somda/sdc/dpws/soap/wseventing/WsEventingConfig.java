package org.somda.sdc.dpws.soap.wseventing;

/**
 * WS-Eventing configuration identifiers.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class WsEventingConfig {
    /**
     * Controls maximum duration to grant for a subscription [in seconds].
     * <p>
     * This configuration value is used by the event source to designate a maximum duration that it will grant for
     * subscriptions.
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String SOURCE_MAX_EXPIRES = "WsEventing.Source.MaxExpires";

    /**
     * A pre-defined subscription manager URI.
     * <p>
     * This URI part will be used by the event source as the default subscription manager URI part.
     * Do not muddle up that value with subscription ids.
     * Those will be generated by the event source automatically.
     * <ul>
     * <li>Data type: {@linkplain java.net.URI}
     * <li>Use: optional
     * </ul>
     */
    public static final String SOURCE_SUBSCRIPTION_MANAGER_PATH = "WsEventing.Source.SubscriptionManagerPath";

    /**
     * Configuration of the notification queue size used for WS-Eventing.
     *
     * <ul>
     * <li>Data type: {@linkplain Integer}
     * <li>Use: optional
     * </ul>
     */
    public static final String NOTIFICATION_QUEUE_CAPACITY = "SoapConfig.NotificationQueueCapacity";
}
