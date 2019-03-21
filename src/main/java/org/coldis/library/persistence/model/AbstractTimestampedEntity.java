package org.coldis.library.persistence.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.coldis.library.dto.DtoAttribute;
import org.coldis.library.model.ModelView;
import org.coldis.library.model.TimestampedObject;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Abstract time-stamped JPA entity.
 */
@MappedSuperclass
@EntityListeners(value = EntityTimestampListener.class)
public abstract class AbstractTimestampedEntity implements TimestampedObject {

	/**
	 * Serial
	 */
	private static final long serialVersionUID = -4206189779959504800L;

	/**
	 * Object creation date/time.
	 */
	private LocalDateTime createdAt;

	/**
	 * Object last update date/time.
	 */
	private LocalDateTime updatedAt;

	/**
	 * @see org.coldis.library.model.TimestampedObject#getCreatedAt()
	 */
	@Override
	@DtoAttribute(readOnly = true, usedInComparison = false)
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	/**
	 * @see org.coldis.library.model.TimestampedObject#setCreatedAt(java.time.LocalDateTime)
	 */
	@Override
	public void setCreatedAt(final LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * @see org.coldis.library.model.TimestampedObject#getUpdatedAt()
	 */
	@Override
	@DtoAttribute(readOnly = true, usedInComparison = false)
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getUpdatedAt() {
		return this.updatedAt;
	}

	/**
	 * @see org.coldis.library.model.TimestampedObject#setUpdatedAt(java.time.LocalDateTime)
	 */
	@Override
	public void setUpdatedAt(final LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
