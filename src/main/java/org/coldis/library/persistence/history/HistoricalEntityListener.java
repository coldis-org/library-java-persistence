package org.coldis.library.persistence.history;

import java.time.Duration;
import java.util.concurrent.Executor;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.thread.DynamicThreadPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

/**
 * JPA entity history listener.
 */
@Component
public class HistoricalEntityListener implements ApplicationContextAware {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalEntityListener.class);

	/**
	 * Application context.
	 */
	private static ApplicationContext appContext;

	/**
	 * Thread pool.
	 */
	public static Executor THREAD_POOL = null;

	/**
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(
			final ApplicationContext applicationContext) throws BeansException {
		HistoricalEntityListener.appContext = applicationContext;
	}

	/**
	 * Sets the thread pool size.
	 *
	 * @param corePoolSize Core pool size (activates blocking thread pool).
	 * @param maxPoolSize  Max pool size.
	 * @param maxQueueSize Queue size.
	 * @param keepAlive    Keep alive.
	 */
	@Autowired
	private void setThreadPoolSize(
			@Value("${org.coldis.library.persistence.history.history-producer.name:historical-entity-thread}")
			final String name,
			@Value("${org.coldis.library.persistence.history.history-producer.priority:2}")
			final Integer priority,
			@Value("${org.coldis.library.persistence.history.history-producer.virtual:false}")
			final Boolean virtual,
			@Value("${org.coldis.library.persistence.history.history-producer.parallelism:}")
			final Integer parallelism,
			@Value("${org.coldis.library.persistence.history.history-producer.parallelism-cpu-multiplier:}")
			final Double parallelismCpuMultiplier,
			@Value("${org.coldis.library.persistence.history.history-producer.min-runnable:}")
			final Integer minRunnable,
			@Value("${org.coldis.library.persistence.history.history-producer.min-runnable-cpu-multiplier:}")
			final Double minRunnableCpuMultiplier,
			@Value("${org.coldis.library.persistence.history.history-producer.core-size:}")
			final Integer corePoolSize,
			@Value("${org.coldis.library.persistence.history.history-producer.core-size-cpu-multiplier:0.5}")
			final Double corePoolSizeCpuMultiplier,
			@Value("${org.coldis.library.persistence.history.history-producer.max-size:}")
			final Integer maxPoolSize,
			@Value("${org.coldis.library.persistence.history.history-producer.max-size-cpu-multiplier:10}")
			final Double maxPoolSizeCpuMultiplier,
			@Value("${org.coldis.library.persistence.history.history-producer.max-queue-size:5000}")
			final Integer maxQueueSize,
			@Value("${org.coldis.library.persistence.history.history-producer.keep-alive-seconds:60}")
			final Integer keepAliveSeconds) {
		if (((corePoolSize != null) && (corePoolSize > 0)) || ((corePoolSizeCpuMultiplier != null) && (corePoolSizeCpuMultiplier > 0))) {
			HistoricalEntityListener.THREAD_POOL = new DynamicThreadPoolFactory().withName(name).withPriority(priority).withVirtual(virtual)
					.withParallelism(parallelism).withParallelismCpuMultiplier(parallelismCpuMultiplier).withMinRunnable(minRunnable)
					.withMinRunnableCpuMultiplier(minRunnableCpuMultiplier).withCorePoolSize(corePoolSize)
					.withCorePoolSizeCpuMultiplier(corePoolSizeCpuMultiplier).withMaxPoolSize(maxPoolSize)
					.withMaxPoolSizeCpuMultiplier(maxPoolSizeCpuMultiplier).withMaxQueueSize(maxQueueSize).withKeepAlive(Duration.ofSeconds(keepAliveSeconds))
					.build();
		}
	}

	/**
	 * Gets the entity history service.
	 *
	 * @param  <EntityType>             Entity type.
	 * @param  entity                   The entity.
	 * @param  historicalEntityMetadata The entity historical metadata.
	 * @return                          The entity history service.
	 * @throws IntegrationException     If the entity history service cannot be
	 *                                      found.
	 */
	@SuppressWarnings("unchecked")
	private <EntityType> EntityHistoryProducerService<EntityType> getEntityHistoryService(
			final Object entity,
			final HistoricalEntity historicalEntityMetadata) throws IntegrationException {

		// Tries to get the entity history service.
		String serviceName = (entity.getClass().getSimpleName() + HistoricalEntityMetadata.PRODUCER_SERVICE_TYPE_SUFFIX);
		serviceName = serviceName.substring(0, 1).toLowerCase() + serviceName.substring(1);
		try {
			return HistoricalEntityListener.appContext.getBean(serviceName, EntityHistoryProducerService.class);
		}
		// If the entity history service cannot be found.
		catch (final NoSuchBeanDefinitionException exception) {
			// Throws an entity history service not found exception.
			HistoricalEntityListener.LOGGER.error("The entity history service bean could not be found: " + exception.getLocalizedMessage());
			HistoricalEntityListener.LOGGER.debug("The entity history service bean could not be found.", exception);
			throw new IntegrationException(new SimpleMessage("entity.history.service.notfound"), exception);
		}

	}

	/**
	 * Handles update for an entity that should track its historical data.
	 *
	 * @param entity Current entity state.
	 */
	@PostUpdate
	@PostPersist
	public void handleUpdate(
			final Object entity) {
		// Gets the entity history metadata.
		final HistoricalEntity historicalEntityMetadata = entity.getClass().getAnnotation(HistoricalEntity.class);
		// If the entity is should track its history.
		if (historicalEntityMetadata != null) {
			// Handles the update for the entity.
			this.getEntityHistoryService(entity, historicalEntityMetadata).handleUpdate(entity);
		}
	}

}
