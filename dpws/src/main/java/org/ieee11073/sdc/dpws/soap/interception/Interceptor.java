package org.ieee11073.sdc.dpws.soap.interception;

/**
 * Interface to designate interceptors.
 * <p>
 * Invoked methods are identified by using the {@link MessageInterceptor} annotation.
 * An interceptor method accepts one parameter of a certain type depending on the message exchange pattern and
 * message direction:
 * <ul>
 * <li>a {@link RequestResponseObject} in case of an input-output pattern on the outgoing direction.
 * <li>a {@link RequestObject} in case of an input-output pattern or input pattern on the incoming direction.
 * <li>a {@link NotificationObject} in case of an output pattern in the outgoing direction
 * </ul>
 *
 *  todo refactor interceptors such that they throw exceptions instead of cancel
 *
 * @see <a href="https://www.w3.org/2002/ws/cg/2/07/meps.html">Web Services message exchange patterns</a>
 */
public interface Interceptor {
}
