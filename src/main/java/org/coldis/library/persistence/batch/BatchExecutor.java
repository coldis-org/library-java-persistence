package org.coldis.library.persistence.batch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
	 * Messages templates.
	 */
	private Map<BatchAction, String> messagesTemplates = new HashMap<>(Map.of(BatchAction.START, "Starting batch for '${key}'.", BatchAction.RESUME,
			"Resuming batch for '${key}' from id '${id}'.", BatchAction.FINISH, "Finishing batch for '${key}' at id '${id}' in '${duration}' minutes."));

	/**
	 * Slack channels to communicate.
	 */
	private Map<BatchAction, String> slackChannels = new HashMap<>();

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
	 * Complete constructor.
	 *
	 * @param keySuffix         Key suffix.
	 * @param size              Size.
	 * @param lastProcessedId   Last processed id.
	 * @param finishWithin      Maximum interval to finish the batch.
	 * @param messagesTemplates Messages templates.
	 * @param slackChannels     Slack channels.
	 */
	public BatchExecutor(
			final String keySuffix,
			final Long size,
			final String lastProcessedId,
			final Duration finishWithin,
			final Map<BatchAction, String> messagesTemplates,
			final Map<BatchAction, String> slackChannels) {
		super();
		this.keySuffix = keySuffix;
		this.size = size;
		this.lastProcessedId = lastProcessedId;
		this.finishWithin = finishWithin;
		this.messagesTemplates = messagesTemplates;
		this.slackChannels = slackChannels;
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
	 * Gets the messagesTemplates.
	 *
	 * @return The messagesTemplates.
	 */
	public Map<BatchAction, String> getMessagesTemplates() {
		this.messagesTemplates = (this.messagesTemplates == null ? new HashMap<>() : this.messagesTemplates);
		return this.messagesTemplates;
	}

	/**
	 * Sets the messagesTemplates.
	 *
	 * @param messagesTemplates New messagesTemplates.
	 */
	public void setMessagesTemplates(
			final Map<BatchAction, String> messagesTemplates) {
		this.messagesTemplates = messagesTemplates;
	}

	/**
	 * Gets the slackChannels.
	 *
	 * @return The slackChannels.
	 */
	public Map<BatchAction, String> getSlackChannels() {
		return this.slackChannels;
	}

	/**
	 * Sets the slackChannels.
	 *
	 * @param slackChannels New slackChannels.
	 */
	public void setSlackChannels(
			final Map<BatchAction, String> slackChannels) {
		this.slackChannels = slackChannels;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(finishWithin, keySuffix, lastProcessedId, messagesTemplates, size, slackChannels);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(
			Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BatchExecutor)) {
			return false;
		}
		BatchExecutor other = (BatchExecutor) obj;
		return Objects.equals(finishWithin, other.finishWithin) && Objects.equals(keySuffix, other.keySuffix)
				&& Objects.equals(lastProcessedId, other.lastProcessedId) && Objects.equals(messagesTemplates, other.messagesTemplates)
				&& Objects.equals(size, other.size) && Objects.equals(slackChannels, other.slackChannels);
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
