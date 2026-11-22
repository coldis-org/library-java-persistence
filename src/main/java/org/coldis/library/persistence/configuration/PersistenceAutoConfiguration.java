package org.coldis.library.persistence.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Persistence auto configuration.
 */
@Configuration
@ConditionalOnProperty(
		name = "org.coldis.configuration.persistence-enabled",
		havingValue = "true",
		matchIfMissing = true
)
@PropertySource(value = { PersistenceAutoConfiguration.PERSISTENCE_PROPERTIES })
@Import(value = { AopTransactionManagementAutoConfiguration.class, ProxyTransactionManagementAutoConfiguration.class, JpaAutoConfiguration.class,
		PrimaryJpaConfiguration.class, SecondaryDatasourcesRegistrar.class })
@AutoConfigureBefore(value = { JpaBaseConfiguration.class, HibernateJpaAutoConfiguration.class })
public class PersistenceAutoConfiguration {

	/**
	 * Persistence package.
	 */
	public static final String PERSISTENCE_PACKAGE = "org.coldis";

	/**
	 * Persistence properties.
	 */
	public static final String PERSISTENCE_PROPERTIES = "classpath:persistence.properties";

	/**
	 * Makes the entity manager factories take pooled connections at the first statement of a
	 * transaction instead of at its begin (static: it is a bean post processor).
	 *
	 * @return The lazy connection post processor.
	 */
	@Bean
	public static LazyConnectionPostProcessor lazyConnectionPostProcessor() {
		return new LazyConnectionPostProcessor();
	}

}
