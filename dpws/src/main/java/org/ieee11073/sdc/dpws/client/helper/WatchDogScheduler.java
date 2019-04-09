package org.ieee11073.sdc.dpws.client.helper;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to define thread pool used by the DPWS implementation. The thread pool injected with this annotation is
 * used for incoming and outgoing network jobs.
 */
@Target({PARAMETER, FIELD, METHOD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface WatchDogScheduler {
}
