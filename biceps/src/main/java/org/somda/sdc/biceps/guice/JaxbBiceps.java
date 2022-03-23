package org.somda.sdc.biceps.guice;


import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to define the JAXB Context for BICEPS models.
 *
 * @see DefaultBicepsModule
 */
@Qualifier
@Target({PARAMETER, FIELD, METHOD})
@Retention(RUNTIME)
public @interface JaxbBiceps {
}
