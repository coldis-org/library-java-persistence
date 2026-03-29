package org.coldis.library.persistence.keyvalue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JMS listener for async delete. Only active when JMS is available.
 */
public class KeyValueDeleteListener {

	/** Key value service component. */
	@Autowired
	private KeyValueServiceComponent keyValueServiceComponent;

	@JmsListener(
			destination = KeyValueServiceComponent.DELETE_QUEUE,
			concurrency = "${org.coldis.library.persistence.keyvalue.delete-concurrency:1-5}"
	)
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAsync(
			final String key) {
		this.keyValueServiceComponent.delete(key);
	}

}
