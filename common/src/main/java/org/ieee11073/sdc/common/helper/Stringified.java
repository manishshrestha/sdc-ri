package org.ieee11073.sdc.common.helper;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Determines a field to be stringified if used with {@link ObjectStringifier}.
 */
@Target({FIELD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface Stringified {
}
