package org.coldis.library.persistence.validation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Payload;

/**
 * Required fields.
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface MutipleRequiredAttributes {

	/**
	 * Validation groups.
	 */
	Class<?>[] groups() default {};

	/**
	 * Validation payload.
	 */
	Class<? extends Payload>[] payload() default {};

	/**
	 * Validation message.
	 */
	String message() default "Required attributes must be present";

	/**
	 * Required attributes.
	 */
	RequiredAttributes[] value();

}
