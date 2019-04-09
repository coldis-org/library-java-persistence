package org.coldis.library.persistence.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

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
	 * Gets the new expiration value using a base date and a time value.
	 *
	 * @param  expirationBaseDate Expiration base date.
	 * @param  plusUnit           Unit of time to add to the expiration base date.
	 * @param  plusValue          Value of time to add to the expiration base date.
	 *
	 * @return                    The new expiration value using a base date and a
	 *                            time value.
	 */
	public static LocalDateTime getExpirationDate(final LocalDateTime expirationBaseDate, final ChronoUnit plusUnit,
			final Long plusValue) {
		return expirationBaseDate.plus(plusValue, plusUnit);
	}

	/**
	 * Updates the expiration date.
	 */
	protected void updateExpiredAt() {
	}

	/**
	 * @see org.coldis.library.model.ExpirableObject#getExpiredAt()
	 */
	@Override
	@DtoAttribute
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getExpiredAt() {
		// Makes sue the expiration date is updated.
		this.updateExpiredAt();
		// Returns the expiration date.
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
	@Transient
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
