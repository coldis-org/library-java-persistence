package org.coldis.library.persistence.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA auto configuration.
 */
@Configuration
@EnableJpaRepositories(enableDefaultTransactions = false,
basePackages = { "org.coldis.library.persistence", "${org.coldis.configuration.persistence.jpa.base-package}" })
public class JpaAutoConfiguration {

}