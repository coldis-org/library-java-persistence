package org.coldis.library.persistence.batch;

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
	 * Last processed id.
	 */
	private String keySuffix;

	/**
	 * Last processed id.
	 */
	private Long size = 13000L;

	/**
	 * Last processed id.
	 */
	private String lastProcessedId;

	/**
	 * Last processed id.
	 */
	private LocalDateTime restartIfLastStartedAtBefore = DateTimeHelper.getCurrentLocalDateTime().minusDays(1L);

	/**
	 * No arguments constructor.
	 */
	public BatchExecutor() {
		super();
	}

	/**
	 * Default constructor.
	 *
	 * @param keySuffix                    Key suffix.
	 * @param size                         Size.
	 * @param lastProcessedId              Last processed id.
	 * @param restartIfLastStartedAtBefore Restart if last started before.
	 */
	public BatchExecutor(final String keySuffix, final Long size, final String lastProcessedId, final LocalDateTime restartIfLastStartedAtBefore) {
		super();
		this.keySuffix = keySuffix;
		this.size = size;
		this.lastProcessedId = lastProcessedId;
		this.restartIfLastStartedAtBefore = restartIfLastStartedAtBefore;
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
	 * Gets the restartIfLastStartedAtBefore.
	 *
	 * @return The restartIfLastStartedAtBefore.
	 */
	public LocalDateTime getRestartIfLastStartedAtBefore() {
		return this.restartIfLastStartedAtBefore;
	}

	/**
	 * Sets the restartIfLastStartedAtBefore.
	 *
	 * @param restartIfLastStartedAtBefore New restartIfLastStartedAtBefore.
	 */
	public void setRestartIfLastStartedAtBefore(
			final LocalDateTime restartIfLastStartedAtBefore) {
		this.restartIfLastStartedAtBefore = restartIfLastStartedAtBefore;
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
