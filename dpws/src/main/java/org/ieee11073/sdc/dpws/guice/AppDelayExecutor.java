package org.ieee11073.sdc.dpws.guice;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to mark the app-delay executor used by WS-Discovery.
 *
 * @see org.ieee11073.sdc.dpws.guice.DefaultDpwsModule
 */
@Target({PARAMETER, FIELD, METHOD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface AppDelayExecutor {
}
