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
     * <p>
     * Default is an empty string that matches any actions.
     *
     * @return the value of the interceptor that matches an action.
     */
    String value() default "";

    /**
     * Interceptors are sorted and called in the order given by the sequence number in here.
     * <p>
     * Default is {@linkplain Integer#MAX_VALUE} (means, the interceptor is put to the end of the chain).
     * <p>
     * <em>Note that no two interceptors of the same interceptor chain should possess the same sequence number!</em>
     *
     * @return the sequence number
     */
    int sequenceNumber() default Integer.MAX_VALUE;

    /**
     * Defines in which communication direction the interceptor method is invoked.
     * <p>
     * By default the annotated method is invoked on any direction.
     *
     * @return the direction.
     */
    Direction direction() default Direction.ANY;
}
