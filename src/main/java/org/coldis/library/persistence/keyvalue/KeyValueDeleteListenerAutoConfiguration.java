package org.coldis.library.persistence.keyvalue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.JmsListenerContainerFactory;

/**
 * Configuration for the delete listener. Only active when JMS is available.
 */
@Configuration
@ConditionalOnClass(JmsListenerContainerFactory.class)
public class KeyValueDeleteListenerAutoConfiguration {

	@Bean
	public KeyValueDeleteListener keyValueDeleteListener() {
		return new KeyValueDeleteListener();
	}

}
