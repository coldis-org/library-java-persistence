package org.coldis.library.persistence.history;

/**
 * JPA entity history producer service interface.
 *
 * @param <EntityType> JPA Entity type.
 */
public interface EntityHistoryProducerService<EntityType> {

	/**
	 * Handles the entity update.
	 *
	 * @param entity Entity current state.
	 */
	void handleUpdate(EntityType entity);

}