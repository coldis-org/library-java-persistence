package org.coldis.library.test.persistence.history.history.service;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.model.ModelView.Persistent;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.persistence.history.EntityHistoryService;
import org.coldis.library.serialization.json.JsonHelper;
import org.coldis.library.test.persistence.history.TestHistoricalEntity;
import org.coldis.library.test.persistence.history.history.dao.TestHistoricalEntityHistoryRepository;
import org.coldis.library.test.persistence.history.history.model.TestHistoricalEntityHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JPA entity history service for
 * {@link org.coldis.library.test.persistence.history.TestHistoricalEntity}.
 */
@Controller
public class TestHistoricalEntityHistoryService implements EntityHistoryService<TestHistoricalEntityHistory> {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TestHistoricalEntityHistoryService.class);

	/**
	 * Entity update queue.
	 */
	private static final String HISTORICAL_ENTITY_QUEUE = "${histSrvQueueName}";

	/**
	 * Entity history DAO.
	 */
	@Autowired
	private TestHistoricalEntityHistoryRepository repository;

	/**
	 * JMS template for processing original entity updates.
	 */
	@Autowired
	private JmsTemplate jmsTemplate;

	/**
	 * Object mapper.
	 */
	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Actually handles the entity state update and saves in the historical data.
	 *
	 * @param state Current entity state.
	 */
	@JmsListener(destination = TestHistoricalEntityHistoryService.HISTORICAL_ENTITY_QUEUE)
	@Transactional(propagation = Propagation.REQUIRED, transactionManager = "historicalTransactionManager")
	public void handleUpdate(final String state) throws IntegrationException {
		TestHistoricalEntityHistoryService.LOGGER.debug(
				"Processing 'org.coldis.library.test.persistence.history.history.model.TestHistoricalEntityHistory' history update.");
		// Tries to process the entity history update.
		try {
			// Saves the new entity history state.
			this.repository.save(new TestHistoricalEntityHistory(
					JsonHelper.deserialize(this.objectMapper, state, new TypeReference<TestHistoricalEntity>() {
					}, false)));
			TestHistoricalEntityHistoryService.LOGGER.debug(
					"'org.coldis.library.test.persistence.history.history.model.TestHistoricalEntityHistory' history update processed.");
		}
		// If the entity state cannot be saved as historical data.
		catch (final Exception exception) {
			// Throws an entity history update error.
			throw new IntegrationException(new SimpleMessage("entity.history.update.failed"), exception);
		}
	}

	/**
	 * @see org.coldis.library.persistence.history.EntityHistoryService#handleUpdate(java.lang.Object)
	 */
	@Override
	public void handleUpdate(final TestHistoricalEntityHistory state) {
		// Sends the update to be processed asynchronously.
		TestHistoricalEntityHistoryService.LOGGER.debug(
				"Sending 'org.coldis.library.test.persistence.history.history.model.TestHistoricalEntityHistory' update to history queue '"
						+ TestHistoricalEntityHistoryService.HISTORICAL_ENTITY_QUEUE + "'.");
		this.jmsTemplate.convertAndSend(TestHistoricalEntityHistoryService.HISTORICAL_ENTITY_QUEUE,
				JsonHelper.serialize(this.objectMapper, state, Persistent.class, false));
		TestHistoricalEntityHistoryService.LOGGER.debug(
				"'org.coldis.library.test.persistence.history.history.model.TestHistoricalEntityHistory' update sent to history queue '"
						+ TestHistoricalEntityHistoryService.HISTORICAL_ENTITY_QUEUE + "'.");
	}

}
