package  ${historicalEntity.getServicePackageName()};

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.model.ModelView.Persistent;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.persistence.history.EntityHistoryService;
import org.coldis.library.serialization.json.JsonHelper;
import org.coldis.library.test.persistence.history.history.service.TestEntityHistoryService;
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

import ${historicalEntity.getOriginalEntityQualifiedTypeName()};
import ${historicalEntity.getEntityQualifiedTypeName()};
import ${historicalEntity.getDaoQualifiedTypeName()};

/**
 * JPA entity history service for {@link ${historicalEntity.getOriginalEntityQualifiedTypeName()}}.
 */
@Controller
public class ${historicalEntity.getServiceTypeName()} implements EntityHistoryService<${historicalEntity.getEntityTypeName()}> {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(${historicalEntity.getServiceTypeName()}.class);
	
	/**
	 * Entity update queue.
	 */
	private static final String HISTORICAL_ENTITY_QUEUE = "${histSrvQueueName}";

	/**
	 * Entity history DAO.
	 */
	@Autowired
	private ${historicalEntity.getDaoTypeName()} repository;

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
	 * @param state	Current entity state.
	 */
	@JmsListener(destination = ${historicalEntity.getServiceTypeName()}.HISTORICAL_ENTITY_QUEUE)
	@Transactional(propagation = Propagation.REQUIRED, transactionManager = "historicalTransactionManager")
	public void handleUpdate(final String state) throws IntegrationException {
		LOGGER.debug("Processing '${historicalEntity.getEntityQualifiedTypeName()}' history update."); 
		// Tries to process the entity history update.
		try {
			// Saves the new entity history state.
			this.repository.save(
					new ${historicalEntity.getEntityTypeName()}(JsonHelper.deserialize(objectMapper,
							state, new TypeReference<${historicalEntity.getOriginalEntityTypeName()}>(){}, false)));
			LOGGER.debug("'${historicalEntity.getEntityQualifiedTypeName()}' history update processed."); 
		}
		// If the entity state cannot be saved as historical data.
		catch (final Exception exception) {
			// Throws an entity history update error.
			throw new IntegrationException(new SimpleMessage("entity.history.update.failed"), exception);
		}
	}

	/**
	 * @see org.coldis.library.persistence.history.EntityHistoryService${h}handleUpdate(java.lang.Object)
	 */
	@Override
	public void handleUpdate(final ${historicalEntity.getEntityTypeName()} state) {
		// Sends the update to be processed asynchronously.
		LOGGER.debug("Sending '${historicalEntity.getEntityQualifiedTypeName()}' update to history queue '" + 
				${historicalEntity.getServiceTypeName()}.HISTORICAL_ENTITY_QUEUE + "'.");
		this.jmsTemplate.convertAndSend(TestEntityHistoryService.HISTORICAL_ENTITY_QUEUE,
				JsonHelper.serialize(this.objectMapper, state, Persistent.class, false));
		LOGGER.debug("'${historicalEntity.getEntityQualifiedTypeName()}' update sent to history queue '" + 
				${historicalEntity.getServiceTypeName()}.HISTORICAL_ENTITY_QUEUE + "'.");
	}

}
