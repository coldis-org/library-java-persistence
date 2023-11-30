package org.coldis.library.persistence.model;

import java.time.LocalDateTime;

import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.MappedSuperclass;

/**
 * Cached entity.
 */
@MappedSuperclass
public abstract class AbstractCacheableEntity extends AbstractTimestampableExpirableEntity implements Typable {

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
