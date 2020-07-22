package org.coldis.library.persistence.validation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Required fields.
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Constraint(validatedBy = RequiredAttributesValidator.class)
public @interface RequiredAttributes {

	/**
	 * Validation message.
	 */
	String message() default "Required attributes must be present";

	/**
	 * Validation groups.
	 */
	Class<?>[] groups() default {};

	/**
	 * Validation payload.
	 */
	Class<? extends Payload>[] payload() default {};

	/**
	 * Supplier method. Must return a collection of attributes names (string).
	 */
	String supplierMethod();

	/**
	 * If not null is used as validation.
	 */
	boolean notNull() default true;

	/**
	 * If not empty string is used as validation.
	 */
	boolean notEmptyString() default true;

	/**
	 * If not empty collection is used as validation.
	 */
	boolean notEmptyCollection() default true;
}
