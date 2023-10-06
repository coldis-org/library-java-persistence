package org.coldis.library.persistence.history;

import org.coldis.library.model.Identifiable;

/**
 * JPA entity history.
 *
 * @param <EntityType> Original entity type.
 */
public interface EntityHistory<EntityType> extends Identifiable {

	/**
	 * Gets the state of the original entity.
	 *
	 * @return The state of the original entity..
	 */
	EntityType getState();

}
