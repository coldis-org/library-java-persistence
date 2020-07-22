package org.coldis.library.persistence.validation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Valid date.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface ValidDate {

	/**
	 * Mininum date in ISO format.
	 */
	String minimumDate() default "";

	/**
	 * minimum date attribute. Should be LocalDate or LocalDateTime.
	 */
	String minimumDateAttribute() default "";

	/**
	 * Maximum date in ISO format.
	 */
	String maximumDate() default "";

	/**
	 * Maximum date attribute. Should be LocalDate or LocalDateTime.
	 */
	String maximumDateAttribute() default "";

}
