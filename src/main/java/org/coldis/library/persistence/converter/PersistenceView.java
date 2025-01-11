package org.coldis.library.persistence.converter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.coldis.library.model.view.ModelView.PersistentAndSensitive;

/**
 * Persistence view annotation.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PersistenceView {

	/**
	 * View or views that annotated element is part of. Views are identified by
	 * classes, and use expected class inheritance relationship: child views contain
	 * all elements parent views have, for example.
	 */
	public Class<?> value() default PersistentAndSensitive.class;

}
