package  ${historicalEntity.getServicePackageName()};

import org.coldis.library.model.ModelView;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
import org.coldis.library.serialization.ObjectMapperHelper;
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
@Controller(value = "${historicalEntity.getProducerServiceBeanName()}")
public class ${historicalEntity.getProducerServiceTypeName()} implements EntityHistoryProducerService<${historicalEntity.getOriginalEntityTypeName()}>{

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(${historicalEntity.getProducerServiceTypeName()}.class);

	/**
	 * Historical entity queue.
	 */
	private static final String HISTORICAL_ENTITY_QUEUE = "${historicalEntity.getQueueName()}";
	
	/**
	 * Object mapper.
	 */
	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * JMS template for processing original entity updates.
	 */
	@Autowired
	private JmsTemplate jmsTemplate;

	/**
	 * @see org.coldis.library.persistence.history.EntityHistoryProducerService${h}handleUpdate(java.lang.Object)
	 */
	@Override
	public void handleUpdate(final ${historicalEntity.getOriginalEntityTypeName()} state) {
		// Sends the update to be processed asynchronously.
		${historicalEntity.getProducerServiceTypeName()}.LOGGER.debug("Sending '${historicalEntity.getEntityQualifiedTypeName()}' update to history queue '" + 
				${historicalEntity.getProducerServiceTypeName()}.HISTORICAL_ENTITY_QUEUE + "'.");
		this.jmsTemplate.convertAndSend(${historicalEntity.getProducerServiceTypeName()}.HISTORICAL_ENTITY_QUEUE, 
				ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false));
		${historicalEntity.getProducerServiceTypeName()}.LOGGER.debug("'${historicalEntity.getEntityQualifiedTypeName()}' update sent to history queue '" + 
				${historicalEntity.getProducerServiceTypeName()}.HISTORICAL_ENTITY_QUEUE + "'.");
	}

}
