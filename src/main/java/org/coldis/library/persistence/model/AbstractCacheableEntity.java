package org.coldis.library.persistence.model;

import java.time.LocalDateTime;

import javax.persistence.MappedSuperclass;

import org.coldis.library.model.ModelView;
import org.coldis.library.model.TypedObject;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Cached entity.
 */
@MappedSuperclass
public abstract class AbstractCacheableEntity extends AbstractTimestampableExpirableEntity implements TypedObject {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 6767257519950574276L;

	/**
	 * When data was cached.
	 */
	private LocalDateTime cachedAt;

	/**
	 * Gets the cachedAt.
	 *
	 * @return The cachedAt.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getCachedAt() {
		return this.cachedAt;
	}

	/**
	 * Sets the cachedAt.
	 *
	 * @param cachedAt New cachedAt.
	 */
	public void setCachedAt(final LocalDateTime cachedAt) {
		this.cachedAt = cachedAt;
	}

}
