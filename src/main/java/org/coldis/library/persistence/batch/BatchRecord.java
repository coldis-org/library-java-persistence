package org.coldis.library.persistence.batch;

import java.time.LocalDateTime;
import java.util.Objects;

import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Batch record.
 */
@JsonTypeName(value = BatchRecord.TYPE_NAME)
public class BatchRecord implements Typable {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 7722008275555225146L;

	/**
	 * Type name.
	 */
	public static final String TYPE_NAME = "org.coldis.library.persistence.batch.BatchRecord";

	/**
	 * Last started at.
	 */
	private LocalDateTime lastStartedAt;

	/**
	 * Last id to be processed.
	 */
	private String lastProcessedId;

	/**
	 * Last processed cout.
	 */
	private Long lastProcessedCount;

	/**
	 * Last finished at.
	 */
	private LocalDateTime lastFinishedAt;

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
	 * Gets the lastProcessedId.
	 *
	 * @return The lastProcessedId.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getLastProcessedId() {
		return this.lastProcessedId;
	}

	/**
	 * Sets the lastProcessedId.
	 *
	 * @param lastProcessedId New lastProcessedId.
	 */
	public void setLastProcessedId(
			final String lastProcessedId) {
		this.lastProcessedId = lastProcessedId;
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
	 * Resets the batch record.
	 */
	public void reset() {
		this.setLastStartedAt(null);
		this.setLastProcessedId(null);
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
		return Objects.hash(this.lastFinishedAt, this.lastProcessedCount, this.lastProcessedId, this.lastStartedAt);
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
		if (!(obj instanceof BatchRecord)) {
			return false;
		}
		final BatchRecord other = (BatchRecord) obj;
		return Objects.equals(this.lastFinishedAt, other.lastFinishedAt) && Objects.equals(this.lastProcessedCount, other.lastProcessedCount)
				&& Objects.equals(this.lastProcessedId, other.lastProcessedId) && Objects.equals(this.lastStartedAt, other.lastStartedAt);
	}

}
