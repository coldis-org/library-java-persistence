package org.coldis.library.persistence.history;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

import org.apache.commons.lang3.StringUtils;
import org.coldis.library.exception.IntegrationException;
import org.coldis.library.model.SimpleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

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
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		HistoricalEntityListener.appContext = applicationContext;
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
	private <EntityType> EntityHistoryProducerService<EntityType> getEntityHistoryService(final Object entity,
			final HistoricalEntity historicalEntityMetadata) throws IntegrationException {
		// Service name is retrieved from the annotation.
		String serviceName = historicalEntityMetadata.producerServiceBeanName();
		// If the service name is not defined in the annotation.
		if (StringUtils.isEmpty(serviceName)) {
			// The service name pattern is used.
			serviceName = (entity.getClass().getSimpleName() + HistoricalEntityMetadata.PRODUCER_SERVICE_TYPE_SUFFIX);
			serviceName = serviceName.substring(0, 1).toLowerCase() + serviceName.substring(1);
		}
		// Tries to get the entity history service.
		try {
			return HistoricalEntityListener.appContext.getBean(serviceName, EntityHistoryProducerService.class);
		}
		// If the entity history service cannot be found.
		catch (final NoSuchBeanDefinitionException exception) {
			// Throws an entity history service not found exception.
			HistoricalEntityListener.LOGGER
			.error("The entity history service bean could not be found: " + exception.getLocalizedMessage());
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
	public void handleUpdate(final Object entity) {
		// Gets the entity history metadata.
		final HistoricalEntity historicalEntityMetadata = entity.getClass().getAnnotation(HistoricalEntity.class);
		// If the entity is should track its history.
		if (historicalEntityMetadata != null) {
			// Handles the update for the entity.
			this.getEntityHistoryService(entity, historicalEntityMetadata).handleUpdate(entity);
		}
	}

}
