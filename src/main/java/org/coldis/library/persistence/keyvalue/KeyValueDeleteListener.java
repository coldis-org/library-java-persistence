package org.coldis.library.persistence.keyvalue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JMS listener for async delete. Only active when JMS is available.
 */
@Component
@ConditionalOnBean(name = "jmsContainerFactory")
public class KeyValueDeleteListener {

	/** Key value service component. */
	@Autowired
	private KeyValueServiceComponent keyValueServiceComponent;

	@JmsListener(destination = KeyValueServiceComponent.DELETE_QUEUE)
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAsync(
			final String key) {
		this.keyValueServiceComponent.delete(key);
	}

}
