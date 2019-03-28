package org.coldis.library.persistence.history;

/**
 * JPA entity history service interface.
 *
 * @param <EntityType> JPA Entity type.
 */
public interface EntityHistoryService<EntityType> {

	/**
	 * Handles the entity update.
	 *
	 * @param entity Entity current state.
	 */
	void handleUpdate(EntityType entity);

}