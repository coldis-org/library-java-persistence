package org.coldis.library.test.persistence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test application.
 */
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = { "org.coldis" })
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
