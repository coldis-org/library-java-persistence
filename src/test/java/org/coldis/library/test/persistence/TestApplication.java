package org.coldis.library.test.persistence;

import org.coldis.spring.configuration.DefaultAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Test application.
 */
@SpringBootApplication(scanBasePackages = { DefaultAutoConfiguration.BASE_PACKAGE })
public class TestApplication {

	/**
	 * Runs the test application.
	 *
	 * @param args Application arguments.
	 */
	public static void main(final String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}

	/**
	 * Creates the rest template object.
	 *
	 * @param  builder Rest template builder.
	 * @return         The rest template object.
	 */
	@Bean
	public RestTemplate restTemplate(final RestTemplateBuilder builder) {
		return builder.build();
	}

}
