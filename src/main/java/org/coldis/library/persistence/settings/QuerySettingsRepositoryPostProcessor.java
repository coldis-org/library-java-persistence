package org.coldis.library.persistence.settings;

import java.util.Arrays;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/**
 * Adds {@link QuerySettingsRepositoryInterceptor} to the proxy of every Spring Data repository whose
 * interface (or one of its methods) carries {@link QuerySettings}, on every persistence unit. The
 * customizer runs after the repository factory registered its transaction post-processor, so the
 * advice sits inside the repository's transaction interceptor.
 */
public class QuerySettingsRepositoryPostProcessor implements BeanPostProcessor {

	/** Component applying the settings (resolved when the repository is created). */
	private final ObjectProvider<QuerySettingsServiceComponent> querySettingsServiceComponent;

	/**
	 * @param querySettingsServiceComponent Component applying the settings.
	 */
	public QuerySettingsRepositoryPostProcessor(final ObjectProvider<QuerySettingsServiceComponent> querySettingsServiceComponent) {
		this.querySettingsServiceComponent = querySettingsServiceComponent;
	}

	/**
	 * Whether a repository interface carries the annotation.
	 */
	private static boolean isAnnotated(
			final Class<?> repositoryInterface) {
		return repositoryInterface.isAnnotationPresent(QuerySettings.class)
				|| Arrays.stream(repositoryInterface.getMethods()).anyMatch(method -> method.isAnnotationPresent(QuerySettings.class));
	}

	/**
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
	 *      java.lang.String)
	 */
	@Override
	public Object postProcessBeforeInitialization(
			final Object bean,
			final String beanName) {
		if (bean instanceof final RepositoryFactoryBeanSupport<?, ?, ?> repositoryFactoryBean) {
			repositoryFactoryBean.addRepositoryFactoryCustomizer(repositoryFactory -> repositoryFactory
					.addRepositoryProxyPostProcessor((proxyFactory, repositoryInformation) -> {
						if (QuerySettingsRepositoryPostProcessor.isAnnotated(repositoryInformation.getRepositoryInterface())) {
							proxyFactory.addAdvice(new QuerySettingsRepositoryInterceptor(repositoryInformation.getRepositoryInterface(),
									this.querySettingsServiceComponent.getObject()));
						}
					}));
		}
		return bean;
	}

}
