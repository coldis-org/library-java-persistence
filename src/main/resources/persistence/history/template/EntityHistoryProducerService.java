package  ${historicalEntity.getServicePackageName()};

import org.coldis.library.model.ModelView.Persistent;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
import org.coldis.library.serialization.json.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ${historicalEntity.getOriginalEntityQualifiedTypeName()};

/**
 * JPA entity history service for
 * {@link ${historicalEntity.getOriginalEntityQualifiedTypeName()}}.
 */
@Controller
public class ${historicalEntity.getProducerServiceTypeName()} implements EntityHistoryProducerService<${historicalEntity.getOriginalEntityTypeName()}>{

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(${historicalEntity.getProducerServiceTypeName()}.class);

	/**
	 * Entity update queue.
	 */
	private static final String HISTORICAL_ENTITY_QUEUE = "${histSrvQueueName}";

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
	 * @see org.coldis.library.persistence.history.EntityHistoryProducerService${h}handleUpdate(java.lang.Object)
	 */
	@Override
	public void handleUpdate(final ${historicalEntity.getOriginalEntityTypeName()} state) {
		// Sends the update to be processed asynchronously.
		LOGGER.debug("Sending '${historicalEntity.getEntityQualifiedTypeName()}' update to history queue '" + 
				${historicalEntity.getProducerServiceTypeName()}.HISTORICAL_ENTITY_QUEUE + "'.");
		this.jmsTemplate.convertAndSend(${historicalEntity.getProducerServiceTypeName()}.HISTORICAL_ENTITY_QUEUE,
				JsonHelper.serialize(this.objectMapper, state, Persistent.class, false));
		LOGGER.debug("'${historicalEntity.getEntityQualifiedTypeName()}' update sent to history queue '" + 
				${historicalEntity.getProducerServiceTypeName()}.HISTORICAL_ENTITY_QUEUE + "'.");
	}

}
