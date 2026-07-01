package org.coldis.library.persistence.configuration;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * Explicit <strong>primary</strong> persistence unit, enabled with
 * {@code org.coldis.configuration.persistence.explicit-primary=true}.
 *
 * <p>
 * Spring Boot's primary {@code DataSource}/{@code EntityManagerFactory} auto-configuration is
 * {@code @ConditionalOnMissingBean}, so it backs off — leaving the primary uncreated — as soon as a
 * service defines a <em>second</em> datasource. A service that needs one enables this flag; coldis
 * then defines the primary beans explicitly and {@code @Primary}, built the same way Boot would (the
 * shared {@link EntityManagerFactoryBuilder} over {@link EntityScanPackages}), so adding a secondary
 * unit never breaks the primary. Disabled (the default) it does not load and nothing changes.
 * </p>
 */
@Configuration
@ConditionalOnProperty(
		name = "org.coldis.configuration.persistence.explicit-primary",
		havingValue = "true")
public class PrimaryJpaConfiguration {

	/** Bean factory (used to read the registered entity-scan packages). */
	@Autowired
	private BeanFactory beanFactory;

	/**
	 * Primary datasource properties (bound from {@code spring.datasource}).
	 *
	 * @return The primary datasource properties.
	 */
	@Primary
	@Bean
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	/**
	 * Primary datasource (pool tuning bound from {@code spring.datasource.hikari}).
	 *
	 * @return The primary datasource.
	 */
	@Primary
	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.hikari")
	public DataSource dataSource() {
		return this.dataSourceProperties().initializeDataSourceBuilder().build();
	}

	/**
	 * Primary entity manager factory, scanning the packages registered by the coldis
	 * {@link JpaAutoConfiguration} entity scan ({@link EntityScanPackages}, single source of truth).
	 *
	 * @param  builder Shared entity manager factory builder.
	 * @return         The primary entity manager factory.
	 */
	@Primary
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			final EntityManagerFactoryBuilder builder) {
		final List<String> packages = EntityScanPackages.get(this.beanFactory).getPackageNames();
		return builder.dataSource(this.dataSource()).persistenceUnit("default").packages(packages.toArray(new String[] {})).build();
	}

	/**
	 * Primary transaction manager.
	 *
	 * @param  entityManagerFactory Primary entity manager factory.
	 * @return                      The primary transaction manager.
	 */
	@Primary
	@Bean
	public PlatformTransactionManager transactionManager(
			final EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}

}
