package org.coldis.library.test.persistence;

import org.coldis.spring.configuration.DefaultAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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

}
