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
	 * Name of the base package for entity history classes.
	 */
	public String basePackageName();

	/**
	 * Entity history template relative path (from resources).
	 */
	public String entityTemplatePath() default "persistence/history/template/EntityHistory.java";

	/**
	 * Entity state column definition.
	 */
	public String stateColumnDefinition() default "JSONB";

	/**
	 * Entity history repository template relative path (from resources).
	 */
	public String repositoryTemplatePath() default "persistence/history/template/EntityHistoryRepository.java";

	/**
	 * Entity history repository bean name.
	 */
	public String repositoryBeanName() default "";

	/**
	 * Entity history producer service template relative path (from resources).
	 */
	public String producerServiceTemplatePath() default "persistence/history/template/EntityHistoryProducerService.java";

	/**
	 * Producer classes target path. Default is "src/main/java".
	 */
	public String producerTargetPath() default "src/main/java";

	/**
	 * JMS template qualifier.
	 */
	public String producerJmsTemplateQualifier() default "historicalJmsTemplate";

	/**
	 * Entity history consumer service template relative path (from resources).
	 */
	public String consumerServiceTemplatePath() default "persistence/history/template/EntityHistoryConsumerService.java";

	/**
	 * Consumer classes target path. Default is "src/main/java".
	 */
	public String consumerTargetPath() default "src/main/java";

	/**
	 * Consumer container factory.
	 */
	public String consumerJmsContainerFactory() default "historicalJmsContainerFactory";

}
