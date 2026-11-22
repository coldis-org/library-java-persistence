package org.coldis.library.test.persistence.lazy;

import javax.sql.DataSource;

import org.coldis.library.persistence.configuration.LazyConnectionDataSource;
import org.coldis.library.persistence.configuration.LazyConnectionPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Which entity manager factories {@link LazyConnectionPostProcessor} wraps, by flag. Nothing here
 * builds a factory or opens a connection: the post processor only swaps the data source the factory
 * will be built with.
 */
public class LazyConnectionPostProcessorTest {

	/** Pool (never started). */
	private HikariDataSource pool;

	/**
	 * Creates the pool.
	 */
	@BeforeEach
	public void createPool() {
		this.pool = new HikariDataSource();
	}

	/**
	 * Closes the pool.
	 */
	@AfterEach
	public void closePool() {
		this.pool.close();
	}

	/**
	 * Runs the post processor on a factory over the pool.
	 *
	 * @param  environment Environment.
	 * @param  beanFactory Bean factory.
	 * @param  beanName    Factory bean name.
	 * @return             The data source the factory will be built with.
	 */
	private DataSource postProcess(
			final MockEnvironment environment,
			final DefaultListableBeanFactory beanFactory,
			final String beanName) {
		final LazyConnectionPostProcessor postProcessor = new LazyConnectionPostProcessor();
		postProcessor.setEnvironment(environment);
		postProcessor.setBeanFactory(beanFactory);
		final LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactory.setDataSource(this.pool);
		postProcessor.postProcessBeforeInitialization(entityManagerFactory, beanName);
		return entityManagerFactory.getDataSource();
	}

	/**
	 * Enabled by default, for the primary and for secondary units, and through to the pool.
	 */
	@Test
	@DisplayName("enabled by default for the primary and secondary units")
	public void testEnabledByDefault() {
		final MockEnvironment environment = new MockEnvironment().withProperty("org.coldis.configuration.persistence.datasources.secondary.url",
				"jdbc:postgresql://localhost/test");
		final DataSource primary = this.postProcess(environment, new DefaultListableBeanFactory(), "entityManagerFactory");
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, primary);
		Assertions.assertSame(this.pool, ((LazyConnectionDataSource) primary).getWrappedObject());
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, this.postProcess(environment, new DefaultListableBeanFactory(), "secondaryEntityManagerFactory"));
	}

	/**
	 * The global flag turns it off everywhere, and a secondary unit's own flag wins over it either way.
	 */
	@Test
	@DisplayName("the global flag turns it off and a unit flag overrides it either way")
	public void testGlobalAndPerDatasourceFlags() {
		final MockEnvironment globalOff = new MockEnvironment().withProperty(LazyConnectionPostProcessor.ENABLED_PROPERTY, "false")
				.withProperty("org.coldis.configuration.persistence.datasources.secondary.url", "jdbc:postgresql://localhost/test")
				.withProperty("org.coldis.configuration.persistence.datasources.tertiary.url", "jdbc:postgresql://localhost/test")
				.withProperty("org.coldis.configuration.persistence.datasources.tertiary.lazy-connection.enabled", "true");
		Assertions.assertSame(this.pool, this.postProcess(globalOff, new DefaultListableBeanFactory(), "entityManagerFactory"));
		Assertions.assertSame(this.pool, this.postProcess(globalOff, new DefaultListableBeanFactory(), "secondaryEntityManagerFactory"));
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, this.postProcess(globalOff, new DefaultListableBeanFactory(), "tertiaryEntityManagerFactory"));
		final MockEnvironment unitOff = new MockEnvironment()
				.withProperty("org.coldis.configuration.persistence.datasources.secondary.url", "jdbc:postgresql://localhost/test")
				.withProperty("org.coldis.configuration.persistence.datasources.secondary.lazy-connection.enabled", "false");
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, this.postProcess(unitOff, new DefaultListableBeanFactory(), "entityManagerFactory"));
		Assertions.assertSame(this.pool, this.postProcess(unitOff, new DefaultListableBeanFactory(), "secondaryEntityManagerFactory"));
	}

	/**
	 * A factory already over a lazy proxy keeps it, and a factory without a data source is left alone.
	 */
	@Test
	@DisplayName("an already lazy or missing data source is left as it is")
	public void testNoDoubleWrapNorMissingDataSource() {
		final LazyConnectionPostProcessor postProcessor = new LazyConnectionPostProcessor();
		postProcessor.setEnvironment(new MockEnvironment());
		postProcessor.setBeanFactory(new DefaultListableBeanFactory());
		final LazyConnectionDataSource lazyDataSource = new LazyConnectionDataSource(this.pool);
		final LocalContainerEntityManagerFactoryBean wrappedFactory = new LocalContainerEntityManagerFactoryBean();
		wrappedFactory.setDataSource(lazyDataSource);
		postProcessor.postProcessBeforeInitialization(wrappedFactory, "entityManagerFactory");
		Assertions.assertSame(lazyDataSource, wrappedFactory.getDataSource());
		final LocalContainerEntityManagerFactoryBean factoryWithoutDataSource = new LocalContainerEntityManagerFactoryBean();
		postProcessor.postProcessBeforeInitialization(factoryWithoutDataSource, "entityManagerFactory");
		Assertions.assertNull(factoryWithoutDataSource.getDataSource());
	}

	/**
	 * Off when a JTA transaction manager is defined, where the factory's data source is the JTA one.
	 */
	@Test
	@DisplayName("off under JTA")
	public void testOffUnderJta() {
		final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("transactionManager", new RootBeanDefinition(JtaTransactionManager.class));
		Assertions.assertSame(this.pool, this.postProcess(new MockEnvironment(), beanFactory, "entityManagerFactory"));
	}

}
