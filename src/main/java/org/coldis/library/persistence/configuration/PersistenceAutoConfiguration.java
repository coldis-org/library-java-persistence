package org.coldis.library.persistence.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
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
@Import(value = { AopTransactionManagementAutoConfiguration.class, ProxyTransactionManagementAutoConfiguration.class, JpaAutoConfiguration.class })
@AutoConfigureBefore(value = { JpaBaseConfiguration.class, HibernateJpaAutoConfiguration.class })
public class PersistenceAutoConfiguration {

	/**
	 * Persistence package.
	 */
	public static final String PERSISTENCE_PACKAGE = "org.coldis.library.persistence";

	/**
	 * Persistence properties.
	 */
	public static final String PERSISTENCE_PROPERTIES = "classpath:persistence.properties";

}
