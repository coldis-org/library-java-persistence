package org.coldis.library.persistence.batch;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.coldis.library.exception.BusinessException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.Typable;
import org.coldis.library.persistence.keyvalue.KeyValue;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.coldis.library.service.slack.SlackIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Batch helper.
 */
@Component
@ConditionalOnProperty(
		name = "org.coldis.configuration.persistence-batch-enabled",
		havingValue = "true",
		matchIfMissing = true
)
public class BatchService {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchService.class);

	/**
	 * Batch key prefix.
	 */
	public static final String BATCH_KEY_PREFIX = "batch-record-";

	/**
	 * Batch lock key prefix.
	 */
	public static final String BATCH_LOCK_KEY_PREFIX = BatchService.BATCH_KEY_PREFIX + "lock-";

	/**
	 * Batch record execute queue.
	 */
	private static final String BATCH_RECORD_EXECUTE_QUEUE = "BatchRecordExecuteQueue";

	/**
	 * Placeholder resolver.
	 */
	private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");

	/**
	 * JMS template.
	 */
	@Autowired(required = false)
	private JmsTemplate jmsTemplate;

	/**
	 * Key batchRecordValue service.
	 */
	@Autowired(required = false)
	private KeyValueService keyValueService;

	/**
	 * Slack integration.
	 */
	@Autowired
	private SlackIntegration slackIntegration;

	/**
	 * Gets the batch key.
	 *
	 * @param  keySuffix Batch key suffix.
	 * @return           Batch key.
	 */
	public String getKey(
			final String keySuffix) {
		return BatchService.BATCH_KEY_PREFIX + keySuffix;
	}

	/**
	 * Gets the batch key.
	 *
	 * @param  keySuffix Batch key suffix.
	 * @return           Batch key.
	 */
	public String getLockKey(
			final String keySuffix) {
		return BatchService.BATCH_LOCK_KEY_PREFIX + keySuffix;
	}

	/**
	 * Get the last id processed in the batch.
	 *
	 * @param  keySuffix  The batch key suffix.
	 * @param  expiration Maximum interval to finish the batch.
	 * @return            The last id processed.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public <Type> Type getLastProcessed(
			final BatchExecutor<Type> executor) {

		// Gets the batch record (and initiates it if necessary).
		final String key = this.getKey(executor.getKeySuffix());
		KeyValue<Typable> batchRecord = this.keyValueService.lock(key).get();
		if (batchRecord.getValue() == null) {
			batchRecord.setValue(new BatchRecord<>(executor.getType()));
		}

		// Gets the last processed id.
		@SuppressWarnings("unchecked")
		final BatchRecord<Type> batchRecordValue = (BatchRecord<Type>) batchRecord.getValue();
		Type lastProcessed = batchRecordValue.getLastProcessed();

		// Clears the last processed id, if data has expired.
		if ((batchRecordValue.getLastStartedAt() == null) || (executor.getExpiration() == null)
				|| batchRecordValue.getLastStartedAt().isBefore(executor.getExpiration())) {
			batchRecordValue.reset();
			lastProcessed = null;
		}

		// Returns the last processed id.
		batchRecord = this.keyValueService.getRepository().save(batchRecord);
		return lastProcessed;
	}

	/**
	 * Logs the action.
	 *
	 * @param  executor          Executor.
	 * @param  action            Action.
	 * @throws BusinessException Exception.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	private <Type> void log(
			final BatchExecutor<Type> executor,
			final BatchAction action) throws BusinessException {
		try {
			// Gets the template and Slack channel.
			final String template = executor.getMessagesTemplates().get(action);
			final String slackChannel = executor.getSlackChannels().get(action);
			// If the template is given.
			if (StringUtils.isNotBlank(template)) {
				// Gets the message properties.
				final String key = this.getKey(executor.getKeySuffix());
				final Type lastProcessed = executor.getLastProcessed();
				final KeyValue<Typable> batchRecord = this.keyValueService.findById(key, false);
				@SuppressWarnings("unchecked")
				final BatchRecord<Type> batchRecordValue = (BatchRecord<Type>) batchRecord.getValue();
				final Long duration = (batchRecordValue.getLastStartedAt() == null ? 0
						: batchRecordValue.getLastStartedAt().until(DateTimeHelper.getCurrentLocalDateTime(), ChronoUnit.MINUTES));
				final Properties messageProperties = new Properties();
				messageProperties.put("key", key);
				messageProperties.put("lastProcessed", Objects.toString(lastProcessed));
				messageProperties.put("duration", duration);
				// Gets the message from the template.
				final String message = BatchService.PLACEHOLDER_HELPER.replacePlaceholders(template, messageProperties);
				// If there is a message.
				if (StringUtils.isNotBlank(message)) {
					BatchService.LOGGER.info(message);
					// If there is a channel to use, sends the message.
					if (StringUtils.isNotBlank(slackChannel)) {
						this.slackIntegration.send(slackChannel, message);
					}
				}
			}
		}
		// Ignores errors.
		catch (final Throwable exception) {
			BatchService.LOGGER.error("Batch action could not be logged: " + exception.getLocalizedMessage());
			BatchService.LOGGER.debug("Batch action could not be logged.", exception);
		}

	}

	/**
	 * Processes a partial batch.
	 *
	 * @param  executor          Executor.
	 * @return                   The last processed id.
	 * @throws BusinessException If the batch could not be processed.
	 */
	@Transactional(
			propagation = Propagation.REQUIRES_NEW,
			timeout = 173
	)
	protected <Type> Type executePartialBatch(
			final BatchExecutor<Type> executor) throws BusinessException {

		// Gets the batch record.
		final KeyValue<Typable> batchRecord = this.keyValueService.findById(this.getKey(executor.getKeySuffix()), true);
		@SuppressWarnings("unchecked")
		final BatchRecord<Type> batchRecordValue = (BatchRecord<Type>) batchRecord.getValue();
		Type actualLastProcessed = executor.getLastProcessed();

		// Updates the last processed start.
		if ((executor.getLastProcessed() == null) || (batchRecordValue.getLastStartedAt() == null)) {
			batchRecordValue.setLastStartedAt(DateTimeHelper.getCurrentLocalDateTime());
		}

		// If the batch has not expired.
		if (!batchRecordValue.getLastStartedAt().isBefore(executor.getExpiration())) {

			// For each item in the next batch.
			this.log(executor, BatchAction.GET);
			final List<Type> nextBatchToProcess = executor.get();
			for (final Type next : nextBatchToProcess) {
				executor.execute(next);
				this.log(executor, BatchAction.EXECUTE);
				actualLastProcessed = next;
				batchRecordValue.setLastProcessedCount(batchRecordValue.getLastProcessedCount() + 1);
			}

		}

		// Updates the last processed id.
		batchRecordValue.setLastProcessed(actualLastProcessed);
		this.keyValueService.getRepository().save(batchRecord);

		// Returns the last processed. id.
		return actualLastProcessed;
	}

	/**
	 * Processes a complete batch.
	 *
	 * @param  executor          Executor.
	 * @throws BusinessException If the batch fails.
	 */
	@JmsListener(
			destination = BatchService.BATCH_RECORD_EXECUTE_QUEUE,
			concurrency = "1-7"
	)
	@Transactional(
			propagation = Propagation.REQUIRED,
			noRollbackFor = Throwable.class,
			timeout = 1237
	)
	public <Type> void executeCompleteBatch(
			final BatchExecutor<Type> executor) throws BusinessException {
		// Synchronizes the batch (preventing to happen in parallel).
		final String lockKey = this.getLockKey(executor.getKeySuffix());
		this.keyValueService.lock(lockKey);
		try {
			// Gets the next id to be processed.
			final Type initialProcessed = this.getLastProcessed(executor);
			Type previousLastProcessed = initialProcessed;
			Type currentLastProcessed = initialProcessed;
			boolean justStarted = true;

			// Starts or resumes the batch.
			if (initialProcessed == null) {
				executor.start();
				this.log(executor, BatchAction.START);
			}
			else {
				executor.resume();
				this.log(executor, BatchAction.RESUME);
			}

			// Runs the batch until the next id does not change.
			while (justStarted || !Objects.equals(previousLastProcessed, currentLastProcessed)) {
				justStarted = false;
				executor.setLastProcessed(currentLastProcessed);
				final Type nextLastProcessed = this.executePartialBatch(executor);
				previousLastProcessed = currentLastProcessed;
				currentLastProcessed = nextLastProcessed;
			}

			// Finishes the batch.
			if (!Objects.equals(initialProcessed, currentLastProcessed)) {
				executor.finish();
				this.log(executor, BatchAction.FINISH);
				final KeyValue<Typable> batchRecord = this.keyValueService.findById(this.getKey(executor.getKeySuffix()), true);
				@SuppressWarnings("unchecked")
				final BatchRecord<Type> batchRecordValue = (BatchRecord<Type>) batchRecord.getValue();
				batchRecordValue.setLastFinishedAt(DateTimeHelper.getCurrentLocalDateTime());
				this.keyValueService.getRepository().save(batchRecord);
			}

		}
		// If there is an error in the batch, retry.
		catch (final Throwable throwable) {
			BatchService.LOGGER.error("Error processing batch '" + this.getKey(executor.getKeySuffix()) + "': " + throwable.getLocalizedMessage());
			BatchService.LOGGER.debug("Error processing batch '" + this.getKey(executor.getKeySuffix()) + "'.", throwable);
			this.processExecuteCompleteBatchAsync(executor);
			throw throwable;
		}
		// Releases the lock.
		finally {
			this.keyValueService.delete(lockKey);
		}

	}

	/**
	 * Processes a complete batch.
	 *
	 * @param  executor          Executor.
	 * @throws BusinessException If the batch fails.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public <Type> void processExecuteCompleteBatchAsync(
			final BatchExecutor<Type> executor) throws BusinessException {
		this.jmsTemplate.convertAndSend(BatchService.BATCH_RECORD_EXECUTE_QUEUE, executor);
	}

}
