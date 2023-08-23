package  org.coldis.library.test.persistence.history.historical.service;

import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.coldis.library.test.persistence.history.TestHistoricalEntity;

/**
 * JPA entity history service for
 * {@link org.coldis.library.test.persistence.history.TestHistoricalEntity}.
 */
@Controller(value = "")
public class TestHistoricalEntityHistoryProducerService implements EntityHistoryProducerService<TestHistoricalEntity>{

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TestHistoricalEntityHistoryProducerService.class);

	/**
	 * Historical entity queue.
	 */
	private static final String HISTORICAL_ENTITY_QUEUE = "TestHistoricalEntityHistory.queue";
	
	/**
	 * Object mapper.
	 */
	@Autowired
	@Qualifier(value = "persistenceJsonMapper")
	private ObjectMapper objectMapper;

	/**
	 * JMS template for processing original entity updates.
	 */
	@Autowired
	private JmsTemplate jmsTemplate;

	/**
	 * @see org.coldis.library.persistence.history.EntityHistoryProducerService#handleUpdate(java.lang.Object)
	 */
	@Override
	public void handleUpdate(final TestHistoricalEntity state) {
		// Sends the update to be processed asynchronously.
		TestHistoricalEntityHistoryProducerService.LOGGER.debug("Sending 'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' update to history queue '" + 
				TestHistoricalEntityHistoryProducerService.HISTORICAL_ENTITY_QUEUE + "'.");
		this.jmsTemplate.convertAndSend(TestHistoricalEntityHistoryProducerService.HISTORICAL_ENTITY_QUEUE, 
				ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false));
		TestHistoricalEntityHistoryProducerService.LOGGER.debug("'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' update sent to history queue '" + 
				TestHistoricalEntityHistoryProducerService.HISTORICAL_ENTITY_QUEUE + "'.");
	}

}
