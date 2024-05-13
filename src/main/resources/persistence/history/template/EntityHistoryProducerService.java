package ${historicalEntity.getServicePackageName()};

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
import org.coldis.library.persistence.history.HistoricalEntityListener;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.coldis.library.service.jms.JmsMessage;
import org.coldis.library.service.jms.JmsTemplateHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
	 * Entity queue.
	 */
	public static final String QUEUE = "${historicalEntity.getQueueName()}";

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
	@Qualifier(value = "entityHistoryJmsTemplate")
	private JmsTemplate jmsTemplate;

	/**
	 * JMS template helper.
	 */
	@Autowired
	private JmsTemplateHelper jmsTemplateHelper;

	/**
	 * Queues history.
	 * @param jmsMessage JMS message.
	 */
	private void queueHistory(final JmsMessage<Object> jmsMessage) {
		this.jmsTemplateHelper.send(jmsTemplate, jmsMessage);
	}

	/**
	 * Adds the entity history.
	 */
	private void addHistory(final ${historicalEntity.getOriginalEntityTypeName()} state) {
		final SecurityContext securityContext = SecurityContextHolder.getContext();
		final String user = (securityContext != null && securityContext.getAuthentication() != null ? securityContext.getAuthentication().getName() : null);
		final JmsMessage<Object> jmsMessage = new JmsMessage<>()
				.withDestination(${historicalEntity.getProducerServiceTypeName()}.QUEUE)
				.withMessage(ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false))
				.withProperties(Map.of("user", (user == null ? "" : user)));
		try {
			if (HistoricalEntityListener.THREAD_POOL == null) {
				this.queueHistory(jmsMessage);
			}
			else {
				HistoricalEntityListener.THREAD_POOL.execute(() -> {
					this.queueHistory(jmsMessage);
				});
			}
		}
		catch(Exception exception) {
			LOGGER.error("Could not queue history for entity: " + exception.getClass().getName() + " - " + exception.getLocalizedMessage());
			LOGGER.debug("Could not queue history for entity.", exception);
		}
	}

	/**
	 * @see org.coldis.library.persistence.history.EntityHistoryProducerService${h}handleUpdate(java.lang.Object)
	 */
	@Override
	public void handleUpdate(final ${historicalEntity.getOriginalEntityTypeName()} state) {
		// Sends the update to be processed asynchronously.
		${historicalEntity.getProducerServiceTypeName()}.LOGGER.debug("Sending '${historicalEntity.getEntityQualifiedTypeName()}' update to history queue '" + 
				${historicalEntity.getProducerServiceTypeName()}.QUEUE + "'.");
		this.addHistory(state);
		${historicalEntity.getProducerServiceTypeName()}.LOGGER.debug("'${historicalEntity.getEntityQualifiedTypeName()}' update sent to history queue '" + 
				${historicalEntity.getProducerServiceTypeName()}.QUEUE + "'.");
	}

}
