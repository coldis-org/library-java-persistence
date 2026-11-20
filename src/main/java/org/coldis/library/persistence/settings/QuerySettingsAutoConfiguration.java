package org.coldis.library.persistence.settings;

import org.aspectj.lang.Aspects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link QuerySettings} configuration: the component applying the settings and its repository, the repository proxy
 * post-processor, and the hand-over of the component to the woven {@link QuerySettingsAspect}.
 *
 * <p>
 * The aspect instance is deliberately not a bean: an {@code @Aspect} bean would also be applied as a
 * Spring AOP proxy advisor in a context with auto-proxying, outside the transaction.
 * </p>
 */
@Configuration
public class QuerySettingsAutoConfiguration {

	/**
	 * Creates the repository running the settings statements.
	 *
	 * @return The repository.
	 */
	@Bean
	public QuerySettingsRepository querySettingsRepository() {
		return new QuerySettingsRepository();
	}

	/**
	 * Creates the component applying the settings and hands it to the woven aspect.
	 *
	 * @param  beanFactory             Bean factory.
	 * @param  querySettingsRepository Repository running the statements.
	 * @return                         The component.
	 */
	@Bean
	public QuerySettingsServiceComponent querySettingsServiceComponent(
			final ConfigurableListableBeanFactory beanFactory,
			final QuerySettingsRepository querySettingsRepository) {
		final QuerySettingsServiceComponent component = new QuerySettingsServiceComponent(beanFactory, querySettingsRepository);
		Aspects.aspectOf(QuerySettingsAspect.class).setQuerySettingsServiceComponent(component);
		return component;
	}

	/**
	 * Creates the repository proxy post-processor.
	 *
	 * @param  querySettingsServiceComponent Component applying the settings.
	 * @return                               The post-processor.
	 */
	@Bean
	public static QuerySettingsRepositoryPostProcessor querySettingsRepositoryPostProcessor(
			final ObjectProvider<QuerySettingsServiceComponent> querySettingsServiceComponent) {
		return new QuerySettingsRepositoryPostProcessor(querySettingsServiceComponent);
	}

}
