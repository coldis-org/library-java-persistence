package org.coldis.library.persistence.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JPA auto configuration.
 */
@EntityScan(basePackages = { PersistenceAutoConfiguration.PERSISTENCE_PACKAGE,
"${org.coldis.configuration.persistence.jpa.base-package}" })
@EnableJpaRepositories(enableDefaultTransactions = false,
basePackages = { PersistenceAutoConfiguration.PERSISTENCE_PACKAGE,
				"${org.coldis.configuration.persistence.jpa.base-package}" })
public class JpaAutoConfiguration {

	/**
	 * Object mapper used in JPA converters..
	 */
	public static ObjectMapper OBJECT_MAPPER;

	/**
	 * Sets the object mapper.
	 *
	 * @param objectMapper Object mapper.
	 */
	@Autowired
	public void setObjectMapper(final ObjectMapper objectMapper) {
		JpaAutoConfiguration.OBJECT_MAPPER = objectMapper;
	}

}