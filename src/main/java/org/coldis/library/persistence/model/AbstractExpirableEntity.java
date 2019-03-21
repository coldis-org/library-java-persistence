package org.coldis.library.persistence.model;

import java.beans.Transient;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.coldis.library.dto.DtoAttribute;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.ExpirableObject;
import org.coldis.library.model.ModelView;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Abstract JPA entity that might expire.
 */
@MappedSuperclass
public abstract class AbstractExpirableEntity extends AbstractTimestampedEntity implements ExpirableObject {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 516365864675481043L;

	/**
	 * Object expiration date/time.
	 */
	private LocalDateTime expiredAt;

	/**
	 * @see org.coldis.library.model.ExpirableObject#getExpiredAt()
	 */
	@Override
	@DtoAttribute
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getExpiredAt() {
		return this.expiredAt;
	}

	/**
	 * @see org.coldis.library.model.ExpirableObject#setExpiredAt(java.time.LocalDateTime)
	 */
	@Override
	public void setExpiredAt(final LocalDateTime expiredAt) {
		this.expiredAt = expiredAt;
	}

	/**
	 * If the entity should be considered expired when no expiration date is
	 * assigned.
	 *
	 * @return If the entity should be considered expired when no expiration date is
	 *         assigned.
	 */
	protected Boolean getExpiredByDefault() {
		return true;
	}

	/**
	 * @see org.coldis.library.model.ExpirableObject#getExpired()
	 */
	@Override
	@Transient
	@DtoAttribute(readOnly = true, usedInComparison = false)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Boolean getExpired() {
		return ((this.getExpiredAt() == null) ? this.getExpiredByDefault()
				: (DateTimeHelper.getCurrentLocalDateTime().isAfter(this.getExpiredAt())));
	}

}
