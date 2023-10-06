package  org.coldis.library.test.persistence.history.historical.service;

import java.util.Map;
import java.util.TimeZone;

import java.time.Instant;
import java.time.LocalDateTime;

import javax.jms.Message;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;
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
	 * Entity queue.
	 */
	public static final String QUEUE = "TestHistoricalEntityHistory/history";
	
	/**
	 * Object mapper.
	 */
	@Autowired
	@Qualifier(value = "persistenceJsonMapper")
	private ObjectMapper objectMapper;

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
	@JmsListener(
			containerFactory = "entityHistoryJmsContainerFactory",
			destination = TestHistoricalEntityHistoryConsumerService.QUEUE,
			concurrency = "${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-concurrency:1-7}"
	)
	public void handleUpdate(final Message message) {
		TestHistoricalEntityHistoryConsumerService.LOGGER.debug("Processing 'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' history update."); 
		// Tries to process the entity history update.
		try {
			// Converts the entity state to a map.
			TestHistoricalEntityHistory entity = new TestHistoricalEntityHistory(ObjectMapperHelper.deserialize(objectMapper, message.getBody(String.class), new TypeReference<Map<String, Object>>() {
			}, false), LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getJMSTimestamp()), 
                    TimeZone.getDefault().toZoneId()));
			// Tries retrieve the update date from the entity.
			try {
				LocalDateTime updatedAt = LocalDateTime.parse(entity.getState().get("updatedAt").toString(), DateTimeHelper.DATE_TIME_FORMATTER);
				entity.setUser(message.getStringProperty("user"));
				entity.setCreatedAt(updatedAt);
				entity.setUpdatedAt(updatedAt);
			}
			// If the entity update date cannot be retrieved.
			catch (Exception exception) {
				// Logs it.
				TestHistoricalEntityHistoryConsumerService.LOGGER.debug("'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' update date could not be retrieved."); 
			}
			// Saves the new entity history state.
			this.repository.save(entity);
			TestHistoricalEntityHistoryConsumerService.LOGGER.debug("'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' history update processed."); 
		}
		// If the entity state cannot be saved as historical data.
		catch (final Exception exception) {
			// Throws an entity history update error.
			throw new IntegrationException(new SimpleMessage("entity.history.update.failed"), exception);
		}
	}

}
