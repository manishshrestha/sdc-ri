package org.ieee11073.sdc.dpws;

import org.ieee11073.sdc.dpws.soap.interception.NotificationCallback;
import org.ieee11073.sdc.dpws.soap.interception.RequestResponseCallback;

import java.io.Closeable;

/**
 * Interface to provide transport bindings of any kind.
 *
 * @see NotificationCallback
 * @see RequestResponseCallback
 */
public interface TransportBinding extends NotificationCallback, RequestResponseCallback, Closeable {
}
