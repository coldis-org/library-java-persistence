package  org.coldis.library.test.persistence.history.historical.service;

import java.util.Map;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory;
import org.coldis.library.test.persistence.history.historical.repository.TestHistoricalEntityHistoryRepository;

/**
 * JPA entity history consumer service for {@link org.coldis.library.test.persistence.history.TestHistoricalEntity}.
 */
@Controller
public class TestHistoricalEntityHistoryConsumerService {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TestHistoricalEntityHistoryConsumerService.class);
	
	/**
	 * Historical entity queue.
	 */
	private static final String HISTORICAL_ENTITY_QUEUE = "queue.TestHistoricalEntityHistoryQueue";

	/**
	 * Entity history repository.
	 */
	@Autowired
	private TestHistoricalEntityHistoryRepository repository;

	/**
	 * Actually handles the entity state update and saves in the historical data.
	 * @param state	Current entity state.
	 */
	@Transactional
	@JmsListener(destination = TestHistoricalEntityHistoryConsumerService.HISTORICAL_ENTITY_QUEUE)
	public void handleUpdate(final Map<String, ?> state) {
		TestHistoricalEntityHistoryConsumerService.LOGGER.debug("Processing 'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' history update."); 
		// Tries to process the entity history update.
		try {
			// Saves the new entity history state.
			this.repository.save(new TestHistoricalEntityHistory(state));
			TestHistoricalEntityHistoryConsumerService.LOGGER.debug("'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' history update processed."); 
		}
		// If the entity state cannot be saved as historical data.
		catch (final Exception exception) {
			// Throws an entity history update error.
			throw new IntegrationException(new SimpleMessage("entity.history.update.failed"), exception);
		}
	}

}
