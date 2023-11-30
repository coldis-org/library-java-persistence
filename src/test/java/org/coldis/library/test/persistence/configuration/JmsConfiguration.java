package org.coldis.library.test.persistence.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jms.AcknowledgeMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

import jakarta.jms.ConnectionFactory;

/**
 * JMS configuration.
 */
@Configuration
public class JmsConfiguration {

	/**
	 * DTO message converter.
	 */
	@Autowired(required = false)
	private MessageConverter messageConverter;

	/**
	 * JMS destination resolver.
	 */
	@Autowired(required = false)
	private DestinationResolver destinationResolver;

	/**
	 * Creates the JMS container factory.
	 *
	 * @param  connectionFactory Connection factory.
	 * @return                   The JMS container factory.
	 */
	@Primary
	@Bean(name = "jmsContainerFactory")
	public DefaultJmsListenerContainerFactory createJmsContainerFactory(
			final ConnectionFactory connectionFactory) {
		// Creates a new container factory.
		final DefaultJmsListenerContainerFactory jmsContainerFactory = new DefaultJmsListenerContainerFactory();
		// Sets the default configuration.
		if (destinationResolver != null) {
			jmsContainerFactory.setDestinationResolver(destinationResolver);
		}
		if (messageConverter != null) {
			jmsContainerFactory.setMessageConverter(messageConverter);
		}
		jmsContainerFactory.setConnectionFactory(connectionFactory);
		jmsContainerFactory.setSessionTransacted(true);
		jmsContainerFactory.setAutoStartup(true);
		jmsContainerFactory.setSessionAcknowledgeMode(AcknowledgeMode.AUTO.getMode());
		// Returns the container factory.
		return jmsContainerFactory;
	}

	/**
	 * Creates the JMS template.
	 *
	 * @param  connectionFactory Connection factory.
	 * @return                   The JMS template.
	 */
	@Primary
	@Bean(name = "jmsTemplate")
	public JmsTemplate createJmsTemplate(
			final ConnectionFactory connectionFactory) {
		// Creates the JMS template.
		final JmsTemplate jmsTemplate = new JmsTemplate();
		// Sets the default configuration.
		if (this.destinationResolver != null) {
			jmsTemplate.setDestinationResolver(this.destinationResolver);
		}
		if (this.messageConverter != null) {
			jmsTemplate.setMessageConverter(this.messageConverter);
		}
		jmsTemplate.setConnectionFactory(connectionFactory);
		jmsTemplate.setSessionTransacted(false);
		jmsTemplate.setSessionAcknowledgeMode(AcknowledgeMode.AUTO.getMode());
		jmsTemplate.setExplicitQosEnabled(false);
		// Returns the configured JMS template.
		return jmsTemplate;
	}

	/**
	 * Creates the JMS container factory.
	 */
	@Bean(name = "entityHistoryJmsContainerFactory")
	@Qualifier(value = "entityHistoryJmsContainerFactory")
	public DefaultJmsListenerContainerFactory createEntityHistoryJmsContainerFactory(
			@Autowired
			final DefaultJmsListenerContainerFactory containerFactory) {
		return containerFactory;
	}

	/**
	 * Creates the JMS template.
	 */
	@Bean(name = "entityHistoryJmsTemplate")
	@Qualifier(value = "entityHistoryJmsTemplate")
	public JmsTemplate createEntityHistoryJmsTemplate(
			@Autowired
			final JmsTemplate jmsTemplate) {
		return jmsTemplate;
	}

}
