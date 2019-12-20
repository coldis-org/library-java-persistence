package org.coldis.library.persistence.history;

import org.coldis.library.model.IdentifiedObject;

/**
 * JPA entity history.
 *
 * @param <EntityType> Original entity type.
 */
public interface EntityHistory<EntityType> extends IdentifiedObject {

	/**
	 * Gets the state of the original entity.
	 *
	 * @return The state of the original entity..
	 */
	EntityType getState();
}
