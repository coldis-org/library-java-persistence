package org.coldis.library.persistence.batch;

import java.time.LocalDateTime;
import java.util.Objects;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;
import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Batch record.
 */
@JsonTypeName(value = BatchRecord.TYPE_NAME)
public class BatchRecord<Type> implements Typable {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 7722008275555225146L;

	/**
	 * Type name.
	 */
	public static final String TYPE_NAME = "org.coldis.library.persistence.batch.BatchRecord";

	/**
	 * Batch item iitemType name.
	 */
	private String itemTypeName;

	/**
	 * Last started at.
	 */
	private LocalDateTime lastStartedAt;

	/**
	 * Last id to be processed.
	 */
	private Type lastProcessed;

	/**
	 * Last processed count.
	 */
	private Long lastProcessedCount;

	/**
	 * Last finished at.
	 */
	private LocalDateTime lastFinishedAt;

	/**
	 * When the record is expired.
	 */
	private LocalDateTime expiredAt;

	/**
	 * Until when record is kept.
	 */
	private LocalDateTime keptUntil;

	/**
	 * No arguments constructor.
	 */
	protected BatchRecord() {
		super();
	}

	/**
	 * No arguments constructor.
	 */
	public BatchRecord(final Class<Type> itemType) {
		super();
		this.itemTypeName = itemType.getName();
	}

	/**
	 * Gets the itemTypeName.
	 *
	 * @return The itemTypeName.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getItemTypeName() {
		return this.itemTypeName;
	}

	/**
	 * Sets the itemTypeName.
	 *
	 * @param itemTypeName New itemTypeName.
	 */
	public void setItemTypeName(
			final String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	/**
	 * Gets the itemType.
	 *
	 * @return The itemType.
	 */
	@JsonIgnore
	@SuppressWarnings("unchecked")
	public Class<Type> getType() {
		try {
			return (this.getItemTypeName() == null ? null : (Class<Type>) Class.forName(this.getItemTypeName()));
		}
		catch (final Exception exception) {
			throw new IntegrationException(new SimpleMessage("type.ivalid"));
		}
	}

	/**
	 * Gets the lastStartedAt.
	 *
	 * @return The lastStartedAt.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getLastStartedAt() {
		return this.lastStartedAt;
	}

	/**
	 * Sets the lastStartedAt.
	 *
	 * @param lastStartedAt New lastStartedAt.
	 */
	public void setLastStartedAt(
			final LocalDateTime lastStartedAt) {
		this.lastStartedAt = lastStartedAt;
	}

	/**
	 * Gets the lastProcessed.
	 *
	 * @return The lastProcessed.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Type getLastProcessed() {
		this.lastProcessed = (((this.getType() == null) || this.getType().isInstance(this.lastProcessed)) ? this.lastProcessed
				: ObjectMapperHelper.convert(BatchExecutor.OBJECT_MAPPER, this.lastProcessed, this.getType(), false));
		return this.lastProcessed;
	}

	/**
	 * Sets the lastProcessed.
	 *
	 * @param lastProcessed New lastProcessed.
	 */
	public void setLastProcessed(
			final Type lastProcessed) {
		this.lastProcessed = lastProcessed;
	}

	/**
	 * Gets the lastProcessedCount.
	 *
	 * @return The lastProcessedCount.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Long getLastProcessedCount() {
		this.lastProcessedCount = (this.lastProcessedCount == null ? 0 : this.lastProcessedCount);
		return this.lastProcessedCount;
	}

	/**
	 * Sets the lastProcessedCount.
	 *
	 * @param lastProcessedCount New lastProcessedCount.
	 */
	public void setLastProcessedCount(
			final Long lastProcessedCount) {
		this.lastProcessedCount = lastProcessedCount;
	}

	/**
	 * Gets the lastFinishedAt.
	 *
	 * @return The lastFinishedAt.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getLastFinishedAt() {
		return this.lastFinishedAt;
	}

	/**
	 * Sets the lastFinishedAt.
	 *
	 * @param lastFinishedAt New lastFinishedAt.
	 */
	public void setLastFinishedAt(
			final LocalDateTime lastFinishedAt) {
		this.lastFinishedAt = lastFinishedAt;
	}

	/**
	 * Gets the expiredAt.
	 *
	 * @return The expiredAt.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getExpiredAt() {
		return this.expiredAt;
	}

	/**
	 * Sets the expiredAt.
	 *
	 * @param expiredAt New expiredAt.
	 */
	public void setExpiredAt(
			final LocalDateTime expiredAt) {
		this.expiredAt = expiredAt;
	}

	/**
	 * If the record is expired.
	 *
	 * @return If the record is expired.
	 */
	public Boolean isExpired() {
		return (this.getExpiredAt() != null) && DateTimeHelper.getCurrentLocalDateTime().isAfter(this.getExpiredAt());
	}

	/**
	 * Gets the keptUntil.
	 *
	 * @return The keptUntil.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getKeptUntil() {
		return this.keptUntil;
	}

	/**
	 * Sets the keptUntil.
	 *
	 * @param keptUntil New keptUntil.
	 */
	public void setKeptUntil(
			final LocalDateTime keptUntil) {
		this.keptUntil = keptUntil;
	}

	/**
	 * If the record should be cleaned.
	 *
	 * @return If the record should be cleaned.
	 */
	public Boolean shouldBeCleaned() {
		return (this.getKeptUntil() != null) && DateTimeHelper.getCurrentLocalDateTime().isAfter(this.getKeptUntil());
	}

	/**
	 * Resets the batch record.
	 */
	public void reset() {
		this.setLastStartedAt(null);
		this.setLastProcessed(null);
		this.setLastProcessedCount(null);
		this.setLastFinishedAt(null);
	}

	/**
	 * @see org.coldis.library.model.Typable#getTypeName()
	 */
	@Override
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getTypeName() {
		return BatchRecord.TYPE_NAME;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.expiredAt, this.itemTypeName, this.keptUntil, this.lastFinishedAt, this.lastProcessed, this.lastProcessedCount,
				this.lastStartedAt);
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
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final BatchRecord other = (BatchRecord) obj;
		return Objects.equals(this.expiredAt, other.expiredAt) && Objects.equals(this.itemTypeName, other.itemTypeName)
				&& Objects.equals(this.keptUntil, other.keptUntil) && Objects.equals(this.lastFinishedAt, other.lastFinishedAt)
				&& Objects.equals(this.lastProcessed, other.lastProcessed) && Objects.equals(this.lastProcessedCount, other.lastProcessedCount)
				&& Objects.equals(this.lastStartedAt, other.lastStartedAt);
	}

}
