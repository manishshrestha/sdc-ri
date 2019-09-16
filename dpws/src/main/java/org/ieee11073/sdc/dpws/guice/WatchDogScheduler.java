package org.ieee11073.sdc.dpws.guice;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Guice annotation to define the thread pool that is used to schedule watchdog jobs.
 */
@Target({PARAMETER, FIELD, METHOD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface WatchDogScheduler {
}
