package  org.coldis.library.test.persistence.history.historical.service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.history.EntityHistoryProducerService;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
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
	private static ExecutorService THREAD_POOL = null;

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
	 * JMS template for processing original entity updates.
	 */
	@Autowired
	@Qualifier(value = "historicalJmsTemplate")
	private JmsTemplate jmsTemplate;

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
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-parallelism:}")
			final Integer parallelism,
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-core-size:5}")
			final Integer corePoolSize,
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-max-size:13}")
			final Integer maxPoolSize,
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-queue-size:300}")
			final Integer queueSize,
			@Value("${org.coldis.library.test.persistence.history.historical.model.testhistoricalentityhistory.history-producer-pool-keep-alive:37}")
			final Integer keepAlive) {
		if (parallelism != null) {
			TestHistoricalEntityHistoryProducerService.THREAD_POOL = new ForkJoinPool(parallelism, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true, corePoolSize, maxPoolSize,
					1, null, keepAlive, TimeUnit.SECONDS);
		}
		else if (corePoolSize != null) {
			final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAlive, TimeUnit.SECONDS,
					new ArrayBlockingQueue<>(queueSize, true));
			threadPoolExecutor.allowCoreThreadTimeOut(true);
			TestHistoricalEntityHistoryProducerService.THREAD_POOL = threadPoolExecutor;
		}
	}

	/**
	 * Adds the entity history.
	 */
	private void addHistory(final TestHistoricalEntity state) {
		if (TestHistoricalEntityHistoryProducerService.THREAD_POOL == null) {
			this.jmsTemplate.convertAndSend(TestHistoricalEntityHistoryProducerService.QUEUE, 
					ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false));
		}
		else {
			TestHistoricalEntityHistoryProducerService.THREAD_POOL.execute(() -> {
				this.jmsTemplate.convertAndSend(TestHistoricalEntityHistoryProducerService.QUEUE, 
						ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false));
			});
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
