package org.coldis.library.persistence.keyvalue;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for the delete listener. Only active when JMS is available.
 */
@AutoConfiguration
@ConditionalOnBean(name = "jmsListenerContainerFactory")
public class KeyValueDeleteListenerAutoConfiguration {

	@Bean
	public KeyValueDeleteListener keyValueDeleteListener() {
		return new KeyValueDeleteListener();
	}

}
