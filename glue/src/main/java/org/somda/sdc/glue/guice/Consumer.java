package org.somda.sdc.glue.guice;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Consumer annotation for class bindings relevant to the SDC service consumer side.
 */
@Target({PARAMETER, FIELD, METHOD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface Consumer {
}
