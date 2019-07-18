package org.coldis.library.persistence.history;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Tracks historical data for JPA entities.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface HistoricalEntity {

	/**
	 * Producer classes target path. Default is "src/main/java".
	 */
	public String producerTargetPath() default "src/main/java";

	/**
	 * Consumer classes target path. Default is "src/main/java".
	 */
	public String consumerTargetPath() default "src/main/java";

	/**
	 * Entity history template relative path (from resources).
	 */
	public String entityTemplatePath() default "persistence/history/template/EntityHistory.java";

	/**
	 * Entity history repository template relative path (from resources).
	 */
	public String repositoryTemplatePath() default "persistence/history/template/EntityHistoryRepository.java";

	/**
	 * Entity history producer service template relative path (from resources).
	 */
	public String producerServiceTemplatePath() default "persistence/history/template/EntityHistoryProducerService.java";

	/**
	 * Entity history consumer service template relative path (from resources).
	 */
	public String consumerServiceTemplatePath() default "persistence/history/template/EntityHistoryConsumerService.java";

	/**
	 * Entity history repository bean name.
	 */
	public String repositoryBeanName() default "";

	/**
	 * Entity history producer service bean name.
	 */
	public String producerServiceBeanName() default "";

	/**
	 * Entity history consumer service bean name.
	 */
	public String consumerServiceBeanName() default "";

	/**
	 * Name of the base package for entity history classes.
	 */
	public String basePackageName();

	/**
	 * Entity state column definition.
	 */
	public String stateColumnDefinition() default "JSONB";

}
