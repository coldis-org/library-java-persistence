package org.coldis.library.persistence.batch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.Typable;

/**
 * Batch executor.
 */
public abstract class BatchExecutor implements Typable {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 3022111202119271553L;

	/**
	 * Key suffix.
	 */
	private String keySuffix;

	/**
	 * Batch size.
	 */
	private Long size = 13000L;

	/**
	 * Last processed id.
	 */
	private String lastProcessedId;

	/**
	 * Maximum interval to finish the batch.
	 */
	private Duration finishWithin = Duration.ofDays(1);

	/**
	 * No arguments constructor.
	 */
	public BatchExecutor() {
		super();
	}

	/**
	 * Default constructor.
	 *
	 * @param keySuffix       Key suffix.
	 * @param size            Size.
	 * @param lastProcessedId Last processed id.
	 * @param finishWithin    Maximum interval to finish the batch.
	 */
	public BatchExecutor(final String keySuffix, final Long size, final String lastProcessedId, final Duration finishWithin) {
		super();
		this.keySuffix = keySuffix;
		this.size = size;
		this.lastProcessedId = lastProcessedId;
		this.finishWithin = finishWithin;
	}

	/**
	 * Gets the keySuffix.
	 *
	 * @return The keySuffix.
	 */
	public String getKeySuffix() {
		return this.keySuffix;
	}

	/**
	 * Sets the keySuffix.
	 *
	 * @param keySuffix New keySuffix.
	 */
	public void setKeySuffix(
			final String keySuffix) {
		this.keySuffix = keySuffix;
	}

	/**
	 * Gets the size.
	 *
	 * @return The size.
	 */
	public Long getSize() {
		return this.size;
	}

	/**
	 * Sets the size.
	 *
	 * @param size New size.
	 */
	public void setSize(
			final Long size) {
		this.size = size;
	}

	/**
	 * Gets the lastProcessedId.
	 *
	 * @return The lastProcessedId.
	 */
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
	 * Gets the finishWithin.
	 *
	 * @return The finishWithin.
	 */
	public Duration getFinishWithin() {
		return this.finishWithin;
	}

	/**
	 * Sets the finishWithin.
	 *
	 * @param finishWithin New finishWithin.
	 */
	public void setFinishWithin(
			final Duration finishWithin) {
		this.finishWithin = finishWithin;
	}

	/**
	 * Gets the finishWithin.
	 *
	 * @return The finishWithin.
	 */
	public LocalDateTime getExpiration() {
		return (this.finishWithin == null ? null : DateTimeHelper.getCurrentLocalDateTime().minus(this.getFinishWithin()));
	}

	/**
	 * Starts the batch.
	 */
	public abstract void start();

	/**
	 * Resumes the batch.
	 */
	public abstract void resume();

	/**
	 * Finishes the batch.
	 */
	public abstract void finish();

	/**
	 * Gets the next batch to be processed.
	 *
	 * @return The next batch to be processed.
	 */
	public abstract List<String> getNextToProcess();

	/**
	 * Executes the batch for one item.
	 *
	 * @param id Item id.
	 */
	public abstract void execute(
			String id);

}
