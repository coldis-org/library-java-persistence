package org.coldis.library.test.persistence.history.historical.service;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.coldis.library.helper.ComputingResourcesHelper;
import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
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

import org.coldis.library.test.persistence.history.TestHistoricalEntity;

/**
 * JPA entity history service for
 * {@link org.coldis.library.test.persistence.history.TestHistoricalEntity}.
 */
@Controller
public class TestHistoricalEntityHistoryProducerService implements EntityHistoryProducerService<TestHistoricalEntity>{

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TestHistoricalEntityHistoryProducerService.class);

	/**
	 * Thread pool.
	 */
	private static ExecutorService THREAD_POOL = Executors.newVirtualThreadPerTaskExecutor();;

	/**
	 * Entity queue.
	 */
	public static final String QUEUE = "test-historical-entity/history";

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
	 * Sets the thread pool size.
	 *
	 * @param parallelism  Parallelism (activates work stealing pool).
	 * @param corePoolSize Core pool size (activates blocking thread pool).
	 * @param maxPoolSize  Max pool size.
	 * @param queueSize    Queue size.
	 * @param keepAlive    Keep alive.
	 */
	@Autowired
	private void setThreadPoolSize(
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-core-size:5}")
			final Integer corePoolSize,
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-max-size:-1}")
			final Integer maxPoolSize,
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-queue-size:5000}")
			final Integer queueSize,
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-keep-alive:61}")
			final Integer keepAlive) {
	    final Integer actualMaxPoolSize =
	            ((maxPoolSize == null) || (maxPoolSize < 0)
	                ? ((Long) (ComputingResourcesHelper.getCpuQuota(true) / 10000L)).intValue()
	                : maxPoolSize);
	    LOGGER.info("Log actual max pool size is: " + actualMaxPoolSize);
        if (corePoolSize != null) {
          ThreadFactory factory = Thread.ofVirtual().factory();
          final ThreadPoolExecutor threadPoolExecutor =
              new ThreadPoolExecutor(
                  corePoolSize,
                  actualMaxPoolSize,
                  keepAlive,
                  TimeUnit.SECONDS,
                  new ArrayBlockingQueue<>(queueSize, true),
                  factory);
          threadPoolExecutor.allowCoreThreadTimeOut(true);
          THREAD_POOL = threadPoolExecutor;
        }
	}
	
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
	private void addHistory(final TestHistoricalEntity state) {
		final SecurityContext securityContext = SecurityContextHolder.getContext();
		final String user = (securityContext != null && securityContext.getAuthentication() != null ? securityContext.getAuthentication().getName() : null);
		final JmsMessage<Object> jmsMessage = new JmsMessage<>()
				.withDestination(TestHistoricalEntityHistoryProducerService.QUEUE)
				.withMessage(ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false))
				.withProperties(Map.of("user", (user == null ? "" : user)));
		try {
			if (TestHistoricalEntityHistoryProducerService.THREAD_POOL == null) {
				this.queueHistory(jmsMessage);
			}
			else {
				TestHistoricalEntityHistoryProducerService.THREAD_POOL.execute(() -> {
					this.queueHistory(jmsMessage);
				});
			}
		}
		catch(Exception exception) {
			LOGGER.error("Could not queue history for entity: " + exception.getLocalizedMessage());
			LOGGER.debug("Could not queue history for entity.", exception);
		}
	}

	/**
	 * @see org.coldis.library.persistence.history.EntityHistoryProducerService#handleUpdate(java.lang.Object)
	 */
	@Override
	public void handleUpdate(final TestHistoricalEntity state) {
		// Sends the update to be processed asynchronously.
		TestHistoricalEntityHistoryProducerService.LOGGER.debug("Sending 'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' update to history queue '" + 
				TestHistoricalEntityHistoryProducerService.QUEUE + "'.");
		this.addHistory(state);
		TestHistoricalEntityHistoryProducerService.LOGGER.debug("'org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory' update sent to history queue '" + 
				TestHistoricalEntityHistoryProducerService.QUEUE + "'.");
	}

}
