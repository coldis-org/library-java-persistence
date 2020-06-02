package org.coldis.library.persistence.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.coldis.library.dto.DtoAttribute;
import org.coldis.library.model.AbstractTimestampedObject;
import org.coldis.library.model.ModelView;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Abstract time-stamped JPA entity.
 */
@MappedSuperclass
@EntityListeners(value = EntityTimestampListener.class)
public abstract class AbstractTimestapableEntity extends AbstractTimestampedObject {

	/**
	 * Serial
	 */
	private static final long serialVersionUID = -4206189779959504800L;

	/**
	 * @see org.coldis.library.model.TimestampedObject#getCreatedAt()
	 */
	@Override
	@DtoAttribute(readOnly = true, usedInComparison = false)
	@Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getCreatedAt() {
		return super.getCreatedAt();
	}

	/**
	 * @see org.coldis.library.model.TimestampedObject#getUpdatedAt()
	 */
	@Override
	@DtoAttribute(readOnly = true, usedInComparison = false)
	@Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getUpdatedAt() {
		return super.getUpdatedAt();
	}

}
