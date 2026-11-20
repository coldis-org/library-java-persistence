package org.coldis.library.persistence.settings;

/**
 * The annotated method call, run by {@link QuerySettingsServiceComponent} once the settings are in
 * effect.
 */
@FunctionalInterface
public interface QuerySettingsInvocation {

	/**
	 * Runs the annotated method.
	 *
	 * @return           The method result.
	 * @throws Throwable Whatever the method throws.
	 */
	Object proceed() throws Throwable;

}
