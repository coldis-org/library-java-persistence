package org.coldis.library.persistence.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA auto configuration.
 */
@EntityScan(basePackages = { PersistenceAutoConfiguration.PERSISTENCE_PACKAGE,
"${org.coldis.configuration.persistence.jpa.base-package}" })
@EnableJpaRepositories(enableDefaultTransactions = false,
basePackages = { PersistenceAutoConfiguration.PERSISTENCE_PACKAGE,
				"${org.coldis.configuration.persistence.jpa.base-package}" })
public class JpaAutoConfiguration {

}