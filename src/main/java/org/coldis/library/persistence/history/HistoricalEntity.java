package org.coldis.library.persistence.history;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Tracks history data for JPA entities.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface HistoricalEntity {

	/**
	 * Target path. Default is "src/main/java".
	 */
	public String targetPath() default "src/main/java";

	/**
	 * Entity history template relative path (from resources).
	 */
	public String entityTemplatePath() default "persistence/history/template/EntityHistory.java";

	/**
	 * Entity history DAO template relative path (from resources).
	 */
	public String daoTemplatePath() default "persistence/history/template/EntityHistoryRepository.java";

	/**
	 * Entity history service template relative path (from resources).
	 */
	public String serviceTemplatePath() default "persistence/history/template/EntityHistoryService.java";

	/**
	 * Name of the base package for entity history classes.
	 */
	public String basePackageName();

	/**
	 * Entity state attribute converter.
	 */
	public Class<?/* extends AttributeConverter<?, ?> */> stateAttributeConverter() default Class.class;// FIXME

	/**
	 * Entity state column definition.
	 */
	public String stateColumnDefinition() default "JSONB";

}
