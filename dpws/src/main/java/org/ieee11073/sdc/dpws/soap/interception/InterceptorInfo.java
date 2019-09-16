package org.ieee11073.sdc.dpws.soap.interception;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Information set to encapsulate information needed to invoke interceptor callbacks.
 */
class InterceptorInfo implements Comparable<InterceptorInfo> {
    private final Integer sequenceNumber;
    private final Object callbackObject;
    private final Method callbackMethod;

    InterceptorInfo(Object callbackObject, Method callbackMethod, Integer sequenceNumber) {
        this.callbackObject = callbackObject;
        this.callbackMethod = callbackMethod;
        this.sequenceNumber = sequenceNumber;
    }

    Method getCallbackMethod() {
        return callbackMethod;
    }

    Object getCallbackObject() {
        return callbackObject;
    }

    int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * (Required to sort {@linkplain InterceptorInfo} objects.
     */
    @Override
    public int compareTo(InterceptorInfo ii) {
        return sequenceNumber.compareTo(ii.sequenceNumber);
    }
}
