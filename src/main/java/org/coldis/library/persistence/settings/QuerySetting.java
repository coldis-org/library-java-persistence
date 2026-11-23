package org.coldis.library.persistence.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * One PostgreSQL setting applied by {@link QuerySettings}. The name must be one of
 * {@link QuerySettingNames the allowed query planning and resource settings}; the value is bound as a
 * parameter of {@code set_config} and validated by PostgreSQL. Both accept {@code ${...}}
 * placeholders.
 */
@Documented
@Target(value = {})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface QuerySetting {

	/**
	 * @return Setting name (for example {@code enable_nestloop}).
	 */
	String name();

	/**
	 * @return Setting value (for example {@code off}).
	 */
	String value();

}
