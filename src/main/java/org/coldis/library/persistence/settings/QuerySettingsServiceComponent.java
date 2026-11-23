package org.coldis.library.persistence.settings;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.coldis.library.persistence.configuration.DatasourceUnit;
import org.coldis.library.persistence.configuration.SecondaryDatasourcesRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.Aspects;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;

/**
 * Resolves {@link QuerySettings} annotations and applies them around a call (see the annotation for
 * the semantics). Shared by {@link QuerySettingsAspect} (classes) and
 * {@link QuerySettingsRepositoryInterceptor} (repository proxies). On start-up it validates every
 * annotation found on a bean and fails when an annotated class was not woven.
 */
public class QuerySettingsServiceComponent implements SmartInitializingSingleton, DisposableBean {

	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(QuerySettingsServiceComponent.class);

	/** Primary datasource display name. */
	private static final String PRIMARY = "primary";

	/** Bean factory. */
	private final ConfigurableListableBeanFactory beanFactory;

	/** Repository running the statements. */
	private final QuerySettingsRepository querySettingsRepository;

	/** Resolved settings by annotated method and type (empty when not annotated). */
	private final Map<List<Object>, Optional<ResolvedQuerySettings>> resolvedSettings = new ConcurrentHashMap<>();

	/** Transaction targets by datasource name. */
	private final Map<String, TransactionTarget> transactionTargets = new ConcurrentHashMap<>();

	/** Scopes applied by the current thread, innermost first. */
	private final ThreadLocal<Deque<AppliedScope>> appliedScopes = new ThreadLocal<>();

	/**
	 * @param beanFactory             Bean factory.
	 * @param querySettingsRepository Repository running the statements.
	 */
	public QuerySettingsServiceComponent(
			final ConfigurableListableBeanFactory beanFactory,
			final QuerySettingsRepository querySettingsRepository) {
		this.beanFactory = beanFactory;
		this.querySettingsRepository = querySettingsRepository;
	}

	/**
	 * Gets a datasource's display name.
	 */
	private static String getDisplayName(
			final String datasource) {
		return (datasource.isEmpty() ? QuerySettingsServiceComponent.PRIMARY : datasource);
	}

	/**
	 * Resolves a datasource's transaction manager and the datasource its transactions bind.
	 */
	private TransactionTarget createTransactionTarget(
			final String datasource) {
		final PlatformTransactionManager transactionManager;
		try {
			transactionManager = (datasource.isEmpty() ? this.beanFactory.getBean(PlatformTransactionManager.class)
					: this.beanFactory.getBean(SecondaryDatasourcesRegistrar.transactionManagerBeanName(datasource), PlatformTransactionManager.class));
		}
		catch (final BeansException exception) {
			throw new IllegalStateException("@QuerySettings targets the " + QuerySettingsServiceComponent.getDisplayName(datasource)
					+ " datasource, but no transaction manager could be found for it.", exception);
		}
		DataSource dataSource = null;
		if (transactionManager instanceof final JpaTransactionManager jpaTransactionManager) {
			dataSource = jpaTransactionManager.getDataSource();
		}
		else if (transactionManager instanceof final DataSourceTransactionManager dataSourceTransactionManager) {
			dataSource = dataSourceTransactionManager.getDataSource();
		}
		if (dataSource == null) {
			throw new IllegalStateException("@QuerySettings targets the " + QuerySettingsServiceComponent.getDisplayName(datasource)
					+ " datasource, but its transaction manager (" + transactionManager.getClass().getName()
					+ ") does not expose the datasource its transactions are bound to.");
		}
		return new TransactionTarget(transactionManager, dataSource);
	}

	/**
	 * Gets (and caches) a datasource's transaction target.
	 */
	private TransactionTarget getTransactionTarget(
			final String datasource) {
		return this.transactionTargets.computeIfAbsent(datasource, this::createTransactionTarget);
	}

	/**
	 * Resolves a placeholder-bearing attribute.
	 */
	private String resolveValue(
			final String value) {
		final String resolvedValue = this.beanFactory.resolveEmbeddedValue(value);
		return (resolvedValue == null ? "" : resolvedValue.trim());
	}

	/**
	 * Adds one validated setting.
	 */
	private void addSetting(
			final Map<String, String> values,
			final String name,
			final String value,
			final String description) {
		final String resolvedName = this.resolveValue(name).toLowerCase(Locale.ROOT);
		final String resolvedValue = this.resolveValue(value);
		if (!QuerySettingNames.isAllowed(resolvedName)) {
			throw new IllegalStateException("@QuerySettings on " + description + " sets '" + resolvedName
					+ "', which is not an allowed query setting (see QuerySettingNames).");
		}
		if (resolvedValue.isEmpty()) {
			throw new IllegalStateException("@QuerySettings on " + description + " sets '" + resolvedName + "' to an empty value.");
		}
		if (values.containsKey(resolvedName)) {
			throw new IllegalStateException("@QuerySettings on " + description + " sets '" + resolvedName + "' more than once.");
		}
		values.put(resolvedName, resolvedValue);
	}

	/**
	 * Validates an annotation and resolves its placeholders.
	 *
	 * @param  annotation Annotation.
	 * @param  description          Annotated element description (for error messages).
	 * @param  repositoryDatasource Datasource of the annotated repository (the default target, and
	 *                                  the only one allowed), or {@code null} for classes.
	 * @return            The resolved settings.
	 */
	ResolvedQuerySettings resolve(
			final QuerySettings annotation,
			final String description,
			final String repositoryDatasource) {
		final Map<String, String> values = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(annotation.workMem())) {
			this.addSetting(values, "work_mem", annotation.workMem(), description);
		}
		if (annotation.planCacheMode() != PlanCacheMode.UNCHANGED) {
			this.addSetting(values, "plan_cache_mode", annotation.planCacheMode().getValue(), description);
		}
		for (final QuerySetting setting : annotation.settings()) {
			this.addSetting(values, setting.name(), setting.value(), description);
		}
		if (values.isEmpty()) {
			throw new IllegalStateException("@QuerySettings on " + description + " declares no setting.");
		}
		final String declaredDatasource = this.resolveValue(annotation.datasource());
		if ((repositoryDatasource != null) && !declaredDatasource.isEmpty() && !declaredDatasource.equals(repositoryDatasource)) {
			throw new IllegalStateException("@QuerySettings on " + description + " targets the " + declaredDatasource + " datasource, but the repository belongs to the "
					+ QuerySettingsServiceComponent.getDisplayName(repositoryDatasource) + " datasource, where its queries run.");
		}
		final String datasource = ((repositoryDatasource != null) && declaredDatasource.isEmpty() ? repositoryDatasource : declaredDatasource);
		this.getTransactionTarget(datasource);
		return new ResolvedQuerySettings(datasource, annotation.startTransaction(), values);
	}

	/**
	 * Gets the datasource a Spring Data repository belongs to (its {@link DatasourceUnit}, or the
	 * primary one), or {@code null} when the type is not a repository.
	 */
	private static String getRepositoryDatasource(
			final Class<?> type) {
		String repositoryDatasource = null;
		if (type.isInterface() && Repository.class.isAssignableFrom(type)) {
			final DatasourceUnit unit = AnnotationUtils.findAnnotation(type, DatasourceUnit.class);
			repositoryDatasource = (unit == null ? "" : unit.value());
		}
		return repositoryDatasource;
	}

	/**
	 * Gets (and caches) the settings that apply to a method: its own annotation, or else the given
	 * type's.
	 *
	 * @param  method Called method.
	 * @param  type   Type whose annotation applies when the method has none.
	 * @return        The settings, or empty when neither is annotated.
	 */
	Optional<ResolvedQuerySettings> resolve(
			final Method method,
			final Class<?> type) {
		return this.resolvedSettings.computeIfAbsent(List.of(method, type), key -> {
			final QuerySettings methodAnnotation = method.getAnnotation(QuerySettings.class);
			final QuerySettings typeAnnotation = AnnotationUtils.findAnnotation(type, QuerySettings.class);
			final String repositoryDatasource = QuerySettingsServiceComponent.getRepositoryDatasource(type);
			Optional<ResolvedQuerySettings> settings = Optional.empty();
			if (methodAnnotation != null) {
				settings = Optional.of(this.resolve(methodAnnotation, method.toGenericString(), repositoryDatasource));
			}
			else if (typeAnnotation != null) {
				settings = Optional.of(this.resolve(typeAnnotation, type.getName(), repositoryDatasource));
			}
			return settings;
		});
	}

	/**
	 * Gets the connection of the transaction active for the target datasource, failing when there is
	 * none (the settings would otherwise apply to nothing). With lazy connection acquisition it is the
	 * transaction's lazy handle, which answers the auto-commit check itself: the pooled connection is
	 * taken by the first statement run on it.
	 */
	private Connection getTransactionConnection(
			final TransactionTarget target,
			final String datasource) throws SQLException {
		Connection connection = null;
		final Object resource = TransactionSynchronizationManager.getResource(target.dataSource());
		if (TransactionSynchronizationManager.isActualTransactionActive() && (resource instanceof final ConnectionHolder holder) && (holder.getConnectionHandle() != null)
				&& !holder.getConnection().getAutoCommit()) {
			connection = holder.getConnection();
		}
		if (connection == null) {
			throw new IllegalStateException("@QuerySettings needs a transaction active on the " + QuerySettingsServiceComponent.getDisplayName(datasource)
					+ " datasource, and there is none here (no @Transactional, suspended by NOT_SUPPORTED/NEVER, or started on another datasource),"
					+ " so the settings would apply to nothing. Run the call inside a transaction on that datasource, or set startTransaction = true.");
		}
		return connection;
	}

	/**
	 * Leaves a scope, forgetting the thread's stack once empty.
	 */
	private void leaveScope(
			final Deque<AppliedScope> scopes) {
		scopes.pop();
		if (scopes.isEmpty()) {
			this.appliedScopes.remove();
		}
	}

	/**
	 * Applies the settings, runs the call and restores the previous values, whether the call returns
	 * or throws.
	 */
	private Object applyAndProceed(
			final Connection connection,
			final ResolvedQuerySettings settings,
			final QuerySettingsInvocation invocation) throws Throwable {
		final String datasource = QuerySettingsServiceComponent.getDisplayName(settings.getDatasource());
		final Map<String, String> previousValues;
		try {
			previousValues = this.querySettingsRepository.read(connection, settings.getValues().keySet());
			this.querySettingsRepository.write(connection, settings.getValues());
		}
		catch (final SQLException exception) {
			throw new IllegalStateException("Could not apply query settings " + settings.getValues() + " on the " + datasource + " datasource.", exception);
		}
		Deque<AppliedScope> scopes = this.appliedScopes.get();
		if (scopes == null) {
			scopes = new ArrayDeque<>();
			this.appliedScopes.set(scopes);
		}
		scopes.push(new AppliedScope(connection, settings.getValues()));
		final Object result;
		try {
			result = invocation.proceed();
		}
		catch (final Throwable throwable) {
			this.leaveScope(scopes);
			// An SQL error aborts the transaction, so restoring fails too: the rollback that follows
			// discards the transaction-local values anyway. Keeps the original error.
			try {
				this.querySettingsRepository.write(connection, previousValues);
			}
			catch (final SQLException restoreException) {
				throwable.addSuppressed(restoreException);
			}
			throw throwable;
		}
		this.leaveScope(scopes);
		try {
			this.querySettingsRepository.write(connection, previousValues);
		}
		catch (final SQLException exception) {
			throw new IllegalStateException("Could not restore query settings " + previousValues + " on the " + datasource + " datasource.", exception);
		}
		return result;
	}

	/**
	 * Runs a call with the settings in effect on the active transaction.
	 */
	private Object executeInScope(
			final TransactionTarget target,
			final ResolvedQuerySettings settings,
			final QuerySettingsInvocation invocation) throws Throwable {
		final Connection connection = this.getTransactionConnection(target, settings.getDatasource());
		final Deque<AppliedScope> scopes = this.appliedScopes.get();
		final AppliedScope currentScope = (scopes == null ? null : scopes.peek());
		final Object result;
		// The enclosing scope already applied the same values on this connection (a type-level
		// annotation on a class calling its own methods): no need to apply them again.
		if ((currentScope != null) && (currentScope.connection() == connection) && currentScope.values().equals(settings.getValues())) {
			result = invocation.proceed();
		}
		else {
			result = this.applyAndProceed(connection, settings, invocation);
		}
		return result;
	}

	/**
	 * Completes the transaction this component opened after a failed call, as {@code @Transactional}
	 * would: rolls back on unchecked exceptions, commits otherwise. Keeps the call's error.
	 */
	private void completeAfterFailure(
			final PlatformTransactionManager transactionManager,
			final TransactionStatus status,
			final Throwable throwable) {
		try {
			if ((throwable instanceof RuntimeException) || (throwable instanceof Error)) {
				transactionManager.rollback(status);
			}
			else {
				transactionManager.commit(status);
			}
		}
		catch (final RuntimeException | Error completionException) {
			throwable.addSuppressed(completionException);
		}
	}

	/**
	 * Runs a call inside a read-only transaction (joining an active one).
	 */
	private Object executeInTransaction(
			final TransactionTarget target,
			final ResolvedQuerySettings settings,
			final QuerySettingsInvocation invocation) throws Throwable {
		final DefaultTransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
		definition.setReadOnly(true);
		final TransactionStatus status = target.transactionManager().getTransaction(definition);
		final Object result;
		try {
			result = this.executeInScope(target, settings, invocation);
		}
		catch (final Throwable throwable) {
			this.completeAfterFailure(target.transactionManager(), status, throwable);
			throw throwable;
		}
		target.transactionManager().commit(status);
		return result;
	}

	/**
	 * Runs a call with the settings in effect.
	 *
	 * @param  settings   Settings.
	 * @param  invocation Call.
	 * @return            The call result.
	 * @throws Throwable  Whatever the call throws, or {@link IllegalStateException} when no
	 *                        transaction is active for the datasource.
	 */
	Object execute(
			final ResolvedQuerySettings settings,
			final QuerySettingsInvocation invocation) throws Throwable {
		final TransactionTarget target = this.getTransactionTarget(settings.getDatasource());
		final Object result;
		if (settings.isStartTransaction()) {
			result = this.executeInTransaction(target, settings, invocation);
		}
		else {
			result = this.executeInScope(target, settings, invocation);
		}
		return result;
	}

	/**
	 * Fails when an annotated element of a class was not woven by {@link QuerySettingsAspect}.
	 */
	private void checkWoven(
			final AnnotatedElement element,
			final String description) {
		if (!element.isAnnotationPresent(QuerySettingsWoven.class)) {
			throw new IllegalStateException("@QuerySettings on " + description
					+ " is not woven, so it would be ignored. Add org.coldis.library:persistence to the aspectj-maven-plugin aspectLibraries of the module that compiles it.");
		}
	}

	/**
	 * Validates the annotations of a Spring Data repository interface.
	 */
	private void validateRepository(
			final Class<?> repositoryInterface) {
		final String repositoryDatasource = QuerySettingsServiceComponent.getRepositoryDatasource(repositoryInterface);
		final QuerySettings typeAnnotation = AnnotationUtils.findAnnotation(repositoryInterface, QuerySettings.class);
		if (typeAnnotation != null) {
			this.resolve(typeAnnotation, repositoryInterface.getName(), repositoryDatasource);
		}
		for (final Method method : repositoryInterface.getMethods()) {
			final QuerySettings methodAnnotation = method.getAnnotation(QuerySettings.class);
			if (methodAnnotation != null) {
				this.resolve(methodAnnotation, method.toGenericString(), repositoryDatasource);
			}
		}
	}

	/**
	 * Fails when an interface that is not a repository carries the annotation: nothing would apply it.
	 */
	private void checkNotAnnotated(
			final Class<?> nonRepositoryInterface) {
		boolean isAnnotated = nonRepositoryInterface.isAnnotationPresent(QuerySettings.class);
		for (final Method method : nonRepositoryInterface.getMethods()) {
			isAnnotated = isAnnotated || method.isAnnotationPresent(QuerySettings.class);
		}
		if (isAnnotated) {
			throw new IllegalStateException("@QuerySettings on interface " + nonRepositoryInterface.getName()
					+ " is ignored: only Spring Data repository interfaces are supported. Annotate the implementing class or its methods.");
		}
	}

	/**
	 * Validates the annotations of a class (and its superclasses and interfaces).
	 */
	private void validateClass(
			final Class<?> beanClass) {
		Class<?> currentClass = beanClass;
		while ((currentClass != null) && (currentClass != Object.class)) {
			final QuerySettings typeAnnotation = currentClass.getAnnotation(QuerySettings.class);
			if (typeAnnotation != null) {
				this.checkWoven(currentClass, currentClass.getName());
				this.resolve(typeAnnotation, currentClass.getName(), null);
			}
			for (final Method method : currentClass.getDeclaredMethods()) {
				final QuerySettings methodAnnotation = method.getAnnotation(QuerySettings.class);
				if (methodAnnotation != null) {
					this.checkWoven(method, method.toGenericString());
					this.resolve(methodAnnotation, method.toGenericString(), null);
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		for (final Class<?> implementedInterface : ClassUtils.getAllInterfacesForClassAsSet(beanClass)) {
			if (!Repository.class.isAssignableFrom(implementedInterface)) {
				this.checkNotAnnotated(implementedInterface);
			}
		}
	}

	/**
	 * Validates the annotations of one bean type.
	 */
	private void validateBeanType(
			final Class<?> beanType) {
		if (beanType.isInterface() && Repository.class.isAssignableFrom(beanType)) {
			this.validateRepository(beanType);
		}
		else if (beanType.isInterface()) {
			this.checkNotAnnotated(beanType);
		}
		else {
			this.validateClass(ClassUtils.getUserClass(beanType));
		}
	}

	/**
	 * Validates every {@link QuerySettings} annotation on the context's beans: setting names, values,
	 * datasources, and (for classes) that the aspect was woven.
	 *
	 * @see org.springframework.beans.factory.SmartInitializingSingleton#afterSingletonsInstantiated()
	 */
	@Override
	public void afterSingletonsInstantiated() {
		for (final String beanName : this.beanFactory.getBeanDefinitionNames()) {
			final Class<?> beanType = this.beanFactory.getType(beanName, false);
			if ((beanType != null) && !Proxy.isProxyClass(beanType) && AnnotationUtils.isCandidateClass(beanType, QuerySettings.class)) {
				try {
					this.validateBeanType(beanType);
				}
				// A class whose methods reference an optional class missing from the classpath cannot be
				// introspected, and so cannot be advised either: skipped, as Spring's own scanners do.
				catch (final LinkageError error) {
					QuerySettingsServiceComponent.LOGGER.debug("Bean type '{}' could not be introspected for @QuerySettings: {}.", beanType.getName(),
							error.toString());
				}
			}
		}
	}

	/**
	 * Clears the woven aspect's reference to this component when its context closes, unless a newer
	 * context already replaced it, so annotated classes fail clearly instead of using a closed context.
	 *
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() {
		Aspects.aspectOf(QuerySettingsAspect.class).clearQuerySettingsServiceComponent(this);
	}

	/**
	 * A datasource's transaction manager and the datasource its transactions bind.
	 *
	 * @param transactionManager Transaction manager.
	 * @param dataSource         Datasource bound by its transactions.
	 */
	private record TransactionTarget(PlatformTransactionManager transactionManager, DataSource dataSource) {
	}

	/**
	 * Settings applied on a connection by an enclosing call.
	 *
	 * @param connection Connection.
	 * @param values     Applied values.
	 */
	private record AppliedScope(Connection connection, Map<String, String> values) {
	}

}
