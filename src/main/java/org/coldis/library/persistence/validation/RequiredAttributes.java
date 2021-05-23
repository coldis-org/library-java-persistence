package org.coldis.library.persistence.validation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
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
@Repeatable(MutipleRequiredAttributes.class)
@Constraint(validatedBy = RequiredAttributesValidator.class)
public @interface RequiredAttributes {

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
	 * Supplier method. Must return a collection of attributes names (string).
	 */
	String supplierMethod();

	/**
	 * If not null is used as validation.
	 */
	boolean notNull() default true;

	/**
	 * If not false is used as validation.
	 */
	boolean notFalse() default true;

	/**
	 * If not empty string is used as validation.
	 */
	boolean notEmptyString() default true;

	/**
	 * If not empty collection is used as validation.
	 */
	boolean notEmptyCollection() default true;
}
