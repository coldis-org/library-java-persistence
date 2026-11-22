package org.coldis.library.persistence.configuration;

import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Makes every JPA entity manager factory use its data source through a
 * {@link LazyConnectionDataSource}, so a transaction takes a pooled connection at its first statement
 * instead of at begin. It applies to the primary unit (Boot's or the explicit one) and to every
 * secondary unit, before the factory is built; the {@code DataSource} beans themselves stay the pools.
 *
 * <p>
 * Enabled by default. {@code org.coldis.configuration.persistence.lazy-connection.enabled=false} turns
 * it off for every unit; a secondary unit can override it with
 * {@code org.coldis.configuration.persistence.datasources.<name>.lazy-connection.enabled}. It stays
 * off when the application uses JTA, where the factory's data source is the JTA one.
 * </p>
 */
public class LazyConnectionPostProcessor implements BeanPostProcessor, PriorityOrdered, EnvironmentAware, BeanFactoryAware {

	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(LazyConnectionPostProcessor.class);

	/** Global flag (default true). */
	public static final String ENABLED_PROPERTY = "org.coldis.configuration.persistence.lazy-connection.enabled";

	/** Suffix of a secondary unit's entity manager factory bean name. */
	private static final String ENTITY_MANAGER_FACTORY_SUFFIX = "EntityManagerFactory";

	/** Environment. */
	private Environment environment;

	/** Bean factory. */
	private BeanFactory beanFactory;

	/**
	 * @see org.springframework.context.EnvironmentAware#setEnvironment(org.springframework.core.env.Environment)
	 */
	@Override
	public void setEnvironment(
			final Environment environment) {
		this.environment = environment;
	}

	/**
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	public void setBeanFactory(
			final BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Registered with the first post processors, so an entity manager factory that another post
	 * processor pulls in early is still wrapped. A factory created before even these are registered is
	 * left as it is (Spring logs it as not eligible for all post processors).
	 *
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/**
	 * Whether the application uses JTA (the condition under which Boot builds the factory over a JTA
	 * data source). Only bean names are read, so no bean is created.
	 *
	 * @return Whether a JTA transaction manager is defined.
	 */
	private boolean isJta() {
		return (this.beanFactory instanceof final ListableBeanFactory listableBeanFactory)
				&& (listableBeanFactory.getBeanNamesForType(JtaTransactionManager.class, true, false).length > 0);
	}

	/**
	 * Whether lazy acquisition is enabled for the entity manager factory with the given bean name: a
	 * secondary unit's own flag when set, otherwise the global flag.
	 *
	 * @param  beanName Entity manager factory bean name.
	 * @return          Whether lazy acquisition is enabled.
	 */
	private boolean isEnabled(
			final String beanName) {
		Boolean enabled = this.environment.getProperty(LazyConnectionPostProcessor.ENABLED_PROPERTY, Boolean.class, Boolean.TRUE);
		final Map<String, SecondaryDatasourceProperties> datasources = Binder.get(this.environment)
				.bind(SecondaryDatasourcesRegistrar.DATASOURCES_PROPERTY_PREFIX, Bindable.mapOf(String.class, SecondaryDatasourceProperties.class))
				.orElseGet(Map::of);
		for (final Map.Entry<String, SecondaryDatasourceProperties> datasource : datasources.entrySet()) {
			final Boolean unitEnabled = datasource.getValue().getLazyConnection().getEnabled();
			if ((datasource.getKey() + LazyConnectionPostProcessor.ENTITY_MANAGER_FACTORY_SUFFIX).equals(beanName) && (unitEnabled != null)) {
				enabled = unitEnabled;
			}
		}
		return enabled;
	}

	/**
	 * Wraps the factory's data source before the factory is built.
	 *
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
	 *      java.lang.String)
	 */
	@Override
	public Object postProcessBeforeInitialization(
			final Object bean,
			final String beanName) throws BeansException {
		if (bean instanceof final LocalContainerEntityManagerFactoryBean entityManagerFactory) {
			final DataSource dataSource = entityManagerFactory.getDataSource();
			if ((dataSource != null) && !(dataSource instanceof LazyConnectionDataSourceProxy) && !this.isJta() && this.isEnabled(beanName)) {
				entityManagerFactory.setDataSource(new LazyConnectionDataSource(dataSource));
				LazyConnectionPostProcessor.LOGGER.debug("Entity manager factory '{}' takes connections lazily from '{}'.", beanName, dataSource);
			}
		}
		return bean;
	}

}
