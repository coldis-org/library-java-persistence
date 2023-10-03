package  ${historicalEntity.getServicePackageName()};

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
	 * Thread pool.
	 */
	private static ExecutorService THREAD_POOL = null;

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
	@Qualifier(value = "${historicalEntity.getProducerJmsTemplateQualifier()}")
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
			@Value("${${historicalEntity.getEntityQualifiedTypeName().toLowerCase()}.history-producer-pool-parallelism:}")
			final Integer parallelism,
			@Value("${${historicalEntity.getEntityQualifiedTypeName().toLowerCase()}.history-producer-pool-core-size:5}")
			final Integer corePoolSize,
			@Value("${${historicalEntity.getEntityQualifiedTypeName().toLowerCase()}.history-producer-pool-max-size:13}")
			final Integer maxPoolSize,
			@Value("${${historicalEntity.getEntityQualifiedTypeName().toLowerCase()}.history-producer-pool-queue-size:300}")
			final Integer queueSize,
			@Value("${${historicalEntity.getEntityQualifiedTypeName().toLowerCase()}.history-producer-pool-keep-alive:37}")
			final Integer keepAlive) {
		if (parallelism != null) {
			${historicalEntity.getProducerServiceTypeName()}.THREAD_POOL = new ForkJoinPool(parallelism, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true, corePoolSize, maxPoolSize,
					1, null, keepAlive, TimeUnit.SECONDS);
		}
		else if (corePoolSize != null) {
			final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAlive, TimeUnit.SECONDS,
					new ArrayBlockingQueue<>(queueSize, true));
			threadPoolExecutor.allowCoreThreadTimeOut(true);
			${historicalEntity.getProducerServiceTypeName()}.THREAD_POOL = threadPoolExecutor;
		}
	}

	/**
	 * Adds the entity history.
	 */
	private void addHistory(final ${historicalEntity.getOriginalEntityTypeName()} state) {
		if (${historicalEntity.getProducerServiceTypeName()}.THREAD_POOL == null) {
			this.jmsTemplate.convertAndSend(${historicalEntity.getProducerServiceTypeName()}.QUEUE, 
					ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false));
		}
		else {
			${historicalEntity.getProducerServiceTypeName()}.THREAD_POOL.execute(() -> {
				this.jmsTemplate.convertAndSend(${historicalEntity.getProducerServiceTypeName()}.QUEUE, 
						ObjectMapperHelper.serialize(objectMapper, state, ModelView.Persistent.class, false));
			});
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
