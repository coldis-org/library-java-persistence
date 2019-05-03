package  org.coldis.library.test.persistence.history.historical.service;

import java.util.Map;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.model.ModelView.Persistent;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
import org.coldis.library.serialization.json.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	 * Entity update queue.
	 */
	private static final String HISTORICAL_ENTITY_QUEUE = "${histSrvQueueName}";

	/**
	 * Entity history repository.
	 */
	@Autowired
	private TestHistoricalEntityHistoryRepository repository;

	/**
	 * Object mapper.
	 */
	@Autowired
	private ObjectMapper objectMapper;
	
	
	/**
	 * Actually handles the entity state update and saves in the historical data.
	 * @param state	Current entity state.
	 */
	@JmsListener(destination = TestHistoricalEntityHistoryConsumerService.HISTORICAL_ENTITY_QUEUE)
	@Transactional(propagation = Propagation.REQUIRED, transactionManager = "historicalTransactionManager")
	public void handleUpdate(final String state) throws IntegrationException {
		LOGGER.debug("Processing 'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' history update."); 
		// Tries to process the entity history update.
		try {
			// Saves the new entity history state.
			this.repository.save(
					new TestHistoricalEntityHistory(JsonHelper.deserialize(objectMapper,
							state, new TypeReference<Map<String, Object>>(){}, false)));
			LOGGER.debug("'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' history update processed."); 
		}
		// If the entity state cannot be saved as historical data.
		catch (final Exception exception) {
			// Throws an entity history update error.
			throw new IntegrationException(new SimpleMessage("entity.history.update.failed"), exception);
		}
	}

}
