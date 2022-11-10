package org.coldis.library.persistence.batch;

import java.time.LocalDateTime;
import java.util.Objects;

import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;
import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	 * Object mapper.
	 */
	private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHelper.createMapper();

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
		this.lastProcessed = (
		// this.lastProcessed instanceof Type ? this.lastProcessed:
		ObjectMapperHelper.convert(BatchRecord.OBJECT_MAPPER, this.lastProcessed, new TypeReference<Type>() {}, false));
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
		return Objects.hash(this.lastFinishedAt, this.lastProcessed, this.lastProcessedCount, this.lastStartedAt);
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
		return Objects.equals(this.lastFinishedAt, other.lastFinishedAt) && Objects.equals(this.lastProcessed, other.lastProcessed)
				&& Objects.equals(this.lastProcessedCount, other.lastProcessedCount) && Objects.equals(this.lastStartedAt, other.lastStartedAt);
	}

}
