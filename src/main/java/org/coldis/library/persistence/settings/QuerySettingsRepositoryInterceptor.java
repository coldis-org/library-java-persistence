package org.coldis.library.persistence.settings;

import java.lang.reflect.Method;
import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Applies {@link QuerySettings} on a Spring Data repository proxy: on the repository interface or
 * its methods (repository proxies are JDK proxies, which compile-time weaving never reaches). Added
 * to the proxy after the repository's transaction interceptor, so a {@code @Transactional} repository
 * method gets the settings inside its own transaction.
 */
class QuerySettingsRepositoryInterceptor implements MethodInterceptor {

	/** Repository interface. */
	private final Class<?> repositoryInterface;

	/** Component applying the settings. */
	private final QuerySettingsServiceComponent querySettingsServiceComponent;

	/**
	 * @param repositoryInterface           Repository interface.
	 * @param querySettingsServiceComponent Component applying the settings.
	 */
	QuerySettingsRepositoryInterceptor(
			final Class<?> repositoryInterface,
			final QuerySettingsServiceComponent querySettingsServiceComponent) {
		this.repositoryInterface = repositoryInterface;
		this.querySettingsServiceComponent = querySettingsServiceComponent;
	}

	/**
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(
			final MethodInvocation invocation) throws Throwable {
		final Method method = invocation.getMethod();
		Optional<ResolvedQuerySettings> settings = Optional.empty();
		if (method.getDeclaringClass() != Object.class) {
			settings = this.querySettingsServiceComponent.resolve(method, this.repositoryInterface);
		}
		final Object result;
		if (settings.isPresent()) {
			result = this.querySettingsServiceComponent.execute(settings.get(), invocation::proceed);
		}
		else {
			result = invocation.proceed();
		}
		return result;
	}

}
