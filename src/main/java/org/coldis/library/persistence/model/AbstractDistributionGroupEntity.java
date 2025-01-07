package org.coldis.library.persistence.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.coldis.library.dto.DtoAttribute;
import org.coldis.library.model.science.AbstractDistributionGroup;
import org.coldis.library.model.science.DistributionGroup;
import org.coldis.library.model.view.ModelView;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Abstract distribution group JPA entity.
 */
@MappedSuperclass
public abstract class AbstractDistributionGroupEntity extends AbstractTimestampableExpirableEntity implements DistributionGroup {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = -1959541715035057595L;

	/**
	 * Distribution size.
	 */
	private Integer distributionSize;

	/**
	 * Absolute limit.
	 */
	private Long absoluteLimit;

	/**
	 * Current size.
	 */
	private Long currentSize;

	/**
	 * When group expires.
	 */
	private LocalDateTime expiredAt;

	/**
	 * Gets the distributionSize.
	 *
	 * @return The distributionSize.
	 */
	@Override
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Integer getDistributionSize() {
		// Make sure the object is initialized.
		this.distributionSize = (this.distributionSize == null ? 0 : this.distributionSize);
		// Returns the object.
		return this.distributionSize;
	}

	/**
	 * Sets the distributionSize.
	 *
	 * @param distributionSize New distributionSize.
	 */
	@Override
	public void setDistributionSize(
			final Integer distributionSize) {
		this.distributionSize = distributionSize;
	}

	/**
	 * Gets the absoluteLimit.
	 *
	 * @return The absoluteLimit.
	 */
	@Override
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Long getAbsoluteLimit() {
		return this.absoluteLimit;
	}

	/**
	 * Sets the absoluteLimit.
	 *
	 * @param absoluteLimit New absoluteLimit.
	 */
	@Override
	public void setAbsoluteLimit(
			final Long absoluteLimit) {
		this.absoluteLimit = absoluteLimit;
	}

	/**
	 * Gets the currentSize.
	 *
	 * @return The currentSize.
	 */
	@Override
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Long getCurrentSize() {
		// Make sure the object is initialized.
		this.currentSize = (this.currentSize == null ? 0L : this.currentSize);
		// Returns the number.
		return this.currentSize;
	}

	/**
	 * Sets the currentSize.
	 *
	 * @param currentSize New currentSize.
	 */
	@Override
	public void setCurrentSize(
			final Long currentSize) {
		// Returns the object.
		this.currentSize = currentSize;
	}

	/**
	 * Gets the expiredAt.
	 *
	 * @return The expiredAt.
	 */
	@Override
	@Column(columnDefinition = "TIMESTAMPTZ")
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getExpiredAt() {
		return this.expiredAt;
	}

	/**
	 * Sets the expiredAt.
	 *
	 * @param expiredAt New expiredAt.
	 */
	@Override
	public void setExpiredAt(
			final LocalDateTime expiredAt) {
		this.expiredAt = expiredAt;
	}

	/**
	 * @see org.coldis.library.model.Expirable#getExpired()
	 */
	@Override
	@DtoAttribute(
			readOnly = true,
			usedInComparison = false
	)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Boolean getExpired() {
		return AbstractDistributionGroup.getExpired(this.getExpiredAt(), this.getAbsoluteLimit(), this.getCurrentSize());
	}

	/**
	 * @see org.coldis.library.persistence.model.AbstractTimestampableExpirableEntity#setExpired(java.lang.Boolean)
	 */
	@Override
	protected void setExpired(
			final Boolean expired) {
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = (prime * result) + Objects.hash(this.absoluteLimit, this.currentSize, this.distributionSize, this.expiredAt);
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(
			final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final AbstractDistributionGroupEntity other = (AbstractDistributionGroupEntity) obj;
		return Objects.equals(this.absoluteLimit, other.absoluteLimit) && Objects.equals(this.currentSize, other.currentSize)
				&& Objects.equals(this.distributionSize, other.distributionSize) && Objects.equals(this.expiredAt, other.expiredAt);
	}

}
