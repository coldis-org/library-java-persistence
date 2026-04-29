package org.coldis.library.persistence.configuration;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JPA auto configuration.
 */
@Configuration
@EntityScan(basePackages = { PersistenceAutoConfiguration.PERSISTENCE_PACKAGE, "${org.coldis.configuration.persistence.jpa.base-package}" })
@EnableJpaRepositories(
		enableDefaultTransactions = false,
		basePackages = { PersistenceAutoConfiguration.PERSISTENCE_PACKAGE, "${org.coldis.configuration.persistence.jpa.base-package}" }
)
public class JpaAutoConfiguration {

	/**
	 * Object mapper used in JPA converters.
	 */
	public static ObjectMapper OBJECT_MAPPER;

	/**
	 * JSON type packages.
	 */
	private List<String> jsonTypePackages;

	/**
	 * JSON type packages.
	 */
	@Value(value = "#{'${org.coldis.configuration.persistence.jpa.base-package:}'.split(',')}")
	private void setJsonTypePackages(
			final String[] jsonTypePackages) {
		this.jsonTypePackages = new ArrayList<>(List.of(jsonTypePackages));
		this.jsonTypePackages.removeIf(StringUtils::isBlank);
		this.jsonTypePackages.add(org.coldis.library.Configuration.BASE_PACKAGE);
	}

	/**
	 * Creates the JSON object mapper.
	 *
	 * @param  builder JSON object mapper builder.
	 * @return         The JSON object mapper.
	 */
	@Qualifier(value = "persistenceJsonMapper")
	@Bean(name = { "persistenceJsonMapper" })
	public ObjectMapper createJsonMapper(
			final Jackson2ObjectMapperBuilder builder) {
		// Creates the object mapper.
		if (JpaAutoConfiguration.OBJECT_MAPPER == null) {
			JpaAutoConfiguration.OBJECT_MAPPER = builder.build();
			ObjectMapperHelper.configureMapper(JpaAutoConfiguration.OBJECT_MAPPER, this.jsonTypePackages.toArray(new String[] {}));
			JpaAutoConfiguration.OBJECT_MAPPER.disable(SerializationFeature.INDENT_OUTPUT);
			JpaAutoConfiguration.OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL);
		}
		// Returns the configured object mapper.
		return JpaAutoConfiguration.OBJECT_MAPPER;
	}

}
