package org.ieee11073.sdc.dpws.soap.interception;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to define if a method of an object is called back by an interceptor processor.
 */
@Target({METHOD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface MessageInterceptor {
    /**
     * Action filter.
     *
     * Default is an empty string, which matches any actions.
     */
    String value() default "";

    /**
     * Interceptors are sorted and called in the order given by the sequence number in here.
     *
     * Default is {@linkplain Integer#MAX_VALUE} (means, the interceptor is put to the end of the chain).
     *
     * **Note that no two interceptors of the same interceptor chain may possess the same sequence number!**
     */
    int sequenceNumber() default Integer.MAX_VALUE;

    /**
     * Define in which communication direction the interceptor method is invoked.
     *
     * By default any direction let the method be invoked.
     */
    Direction direction() default Direction.ANY;
}
