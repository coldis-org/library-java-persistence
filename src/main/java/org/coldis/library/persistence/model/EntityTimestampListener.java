package org.coldis.library.persistence.model;

import java.time.LocalDateTime;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.TimestampedObject;

/**
 * JPA entity time-stamp listener.
 */
public class EntityTimestampListener {

	/**
	 * Updates the last time the entity has been updated (also sets the creation
	 * time, if still not assigned).
	 *
	 * @param entity The current entity state.
	 */
	@PreUpdate
	@PrePersist
	public void preUpdate(final Object entity) {
		// Only if the entity is time-stamped.
		if ((entity != null) && (entity instanceof TimestampedObject)) {
			// Converts the entity as time-stamped.
			final TimestampedObject timestampedEntity = (TimestampedObject) entity;
			// Gets the current date/time.
			final LocalDateTime now = DateTimeHelper.getCurrentLocalDateTime();
			// If creation date/time has not been assigned yet.
			if (timestampedEntity.getCreatedAt() == null) {
				// Sets the creation date/time.
				timestampedEntity.setCreatedAt(now);
			}
			// Sets the last update date/time.
			timestampedEntity.setUpdatedAt(now);
		}
	}

}
