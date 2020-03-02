package org.somda.sdc.common.util;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Determines a field to be stringified if used with {@link ObjectStringifier}.
 */
@Target({FIELD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface Stringified {
}
