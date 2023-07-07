package org.coldis.library.persistence.batch;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.coldis.library.exception.BusinessException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.model.Typable;
import org.coldis.library.persistence.keyvalue.KeyValue;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.coldis.library.service.jms.JmsMessage;
import org.coldis.library.service.jms.JmsTemplateHelper;
import org.coldis.library.service.slack.SlackIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
	private static final String BATCH_RECORD_EXECUTE_QUEUE = "batch/record/execute";

	/**
	 * Placeholder resolver.
	 */
	private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");

	/**
	 * Batch expired message.
	 */
	private static final String BATCH_EXPIRED_MESSAGE_CODE = "batch.expired";

	/**
	 * JMS template.
	 */
	@Autowired(required = false)
	private JmsTemplate jmsTemplate;

	/**
	 * JMS template helper.
	 */
	@Autowired(required = false)
	private JmsTemplateHelper jmsTemplateHelper;

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
			final BatchExecutor<Type> executor,
			final Boolean cleanExpiration) {
		final LocalDateTime now = DateTimeHelper.getCurrentLocalDateTime();

		// Gets the batch record (and initiates it if necessary).
		final String key = this.getKey(executor.getKeySuffix());
		KeyValue<Typable> batchRecord = this.keyValueService.lock(key).get();
		if (batchRecord.getValue() == null) {
			final BatchExecutor<Type> batchRecordValue = executor;
			batchRecordValue.setExpiredAt(executor.getFinishWithin() == null ? null : now.plus(executor.getFinishWithin()));
			batchRecordValue.setKeptUntil(executor.getCleansWithin() == null ? null : now.plus(executor.getCleansWithin()));
			batchRecord.setValue(batchRecordValue);
		}

		// Gets the last processed id.
		@SuppressWarnings("unchecked")
		final BatchExecutor<Type> batchRecordValue = (BatchExecutor<Type>) batchRecord.getValue();
		Type lastProcessed = batchRecordValue.getLastProcessed();

		// Clears the last processed id, if data has expired.
		if ((batchRecordValue.getLastStartedAt() == null) || batchRecordValue.isExpired()) {
			if (cleanExpiration) {
				batchRecordValue.reset();
				batchRecordValue.setExpiredAt(executor.getFinishWithin() == null ? null : now.plus(executor.getFinishWithin()));
				batchRecordValue.setKeptUntil(executor.getCleansWithin() == null ? null : now.plus(executor.getCleansWithin()));
			}
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
				final BatchExecutor<Type> batchRecordValue = (BatchExecutor<Type>) batchRecord.getValue();
				final Long duration = (batchRecordValue.getLastStartedAt() == null ? 0
						: batchRecordValue.getLastStartedAt().until(DateTimeHelper.getCurrentLocalDateTime(), ChronoUnit.MINUTES));
				final Properties messageProperties = new Properties();
				messageProperties.put("key", key);
				messageProperties.put("lastProcessed", Objects.toString(lastProcessed));
				messageProperties.put("duration", duration.toString());
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
			timeout = 360
	)
	protected <Type> Type executePartialBatch(
			final BatchExecutor<Type> executor) throws BusinessException {

		// Gets the batch record.
		final KeyValue<Typable> batchRecord = this.keyValueService.findById(this.getKey(executor.getKeySuffix()), true);
		@SuppressWarnings("unchecked")
		final BatchExecutor<Type> batchRecordValue = (BatchExecutor<Type>) batchRecord.getValue();
		Type actualLastProcessed = executor.getLastProcessed();

		// Updates the last processed start.
		if ((executor.getLastProcessed() == null) || (batchRecordValue.getLastStartedAt() == null)) {
			batchRecordValue.setLastStartedAt(DateTimeHelper.getCurrentLocalDateTime());
		}

		// Throws an exception if the batch has expired.
		if (batchRecordValue.isExpired()) {
			throw new BusinessException(new SimpleMessage(BatchService.BATCH_EXPIRED_MESSAGE_CODE));
		}

		// For each item in the next batch.
		this.log(executor, BatchAction.GET);
		final List<Type> nextBatchToProcess = executor.get();
		for (final Type next : nextBatchToProcess) {
			executor.execute(next);
			this.log(executor, BatchAction.EXECUTE);
			actualLastProcessed = next;
			batchRecordValue.setLastProcessedCount(batchRecordValue.getLastProcessedCount() + 1);
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
	@Transactional(
			propagation = Propagation.REQUIRED,
			noRollbackFor = Throwable.class,
			timeout = 3600
	)
	private <Type> void executeCompleteBatch(
			final BatchExecutor<Type> executor,
			final Boolean cleanExpiration) throws BusinessException {
		// Synchronizes the batch (preventing to happen in parallel).
		final String lockKey = this.getLockKey(executor.getKeySuffix());
		this.keyValueService.lock(lockKey);
		try {
			// Gets the next id to be processed.
			Type previousLastProcessed = null;
			Type currentLastProcessed = this.getLastProcessed(executor, cleanExpiration);

			// Starts or resumes the batch.
			if (currentLastProcessed == null) {
				executor.start();
				this.log(executor, BatchAction.START);
			}
			else {
				executor.resume();
				this.log(executor, BatchAction.RESUME);
			}

			// Runs the batch until the next id does not change.
			executor.setLastProcessed(currentLastProcessed);
			final Type nextLastProcessed = this.executePartialBatch(executor);
			previousLastProcessed = currentLastProcessed;
			currentLastProcessed = nextLastProcessed;
			
			// If there is no new data, finishes the batch.
			if (Objects.equals(previousLastProcessed, currentLastProcessed)) {
				executor.finish();
				this.log(executor, BatchAction.FINISH);
				final String batchKey = this.getKey(executor.getKeySuffix());
				final KeyValue<Typable> batchRecord = this.keyValueService.findById(batchKey, true);
				@SuppressWarnings("unchecked")
				final BatchExecutor<Type> batchRecordValue = (BatchExecutor<Type>) batchRecord.getValue();
				batchRecordValue.setLastFinishedAt(DateTimeHelper.getCurrentLocalDateTime());
				this.keyValueService.getRepository().save(batchRecord);
			}
			else {
				this.queueExecuteCompleteBatchAsync(executor);
			}

		}
		// If there is an error in the batch, retry.
		catch (final Throwable throwable) {
			BatchService.LOGGER.error("Error processing batch '" + this.getKey(executor.getKeySuffix()) + "': " + throwable.getLocalizedMessage());
			BatchService.LOGGER.debug("Error processing batch '" + this.getKey(executor.getKeySuffix()) + "'.", throwable);
			if (!(throwable instanceof BusinessException) || (!((BusinessException) throwable).getCode().equals(BatchService.BATCH_EXPIRED_MESSAGE_CODE))) {
				this.queueExecuteCompleteBatchAsync(executor);
			}
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
	public <Type> void executeCompleteBatch(
			final BatchExecutor<Type> executor) throws BusinessException {
		this.executeCompleteBatch(executor, true);
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
	public <Type> void executeCompleteBatchAsync(
			final BatchExecutor<Type> executor) throws BusinessException {
		this.executeCompleteBatch(executor, false);
	}

	/**
	 * Processes a complete batch.
	 *
	 * @param  executor          Executor.
	 * @throws BusinessException If the batch fails.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public <Type> void queueExecuteCompleteBatchAsync(
			final BatchExecutor<Type> executor) throws BusinessException {
		this.jmsTemplateHelper.send(this.jmsTemplate,
				new JmsMessage<>().withDestination(BatchService.BATCH_RECORD_EXECUTE_QUEUE).withLastValueKey(executor.getKeySuffix()).withMessage(executor));
	}

	/**
	 * Cleans old batches.
	 *
	 * @throws BusinessException If the batches cannot be cleaned.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void cleanAll() throws BusinessException {
		final List<KeyValue<Typable>> batchRecords = this.keyValueService.findByKeyStart(BatchService.BATCH_KEY_PREFIX);
		for (final KeyValue<Typable> batchRecord : batchRecords) {
			this.keyValueService.delete(batchRecord.getKey());
		}
	}

	/**
	 * Cleans old batches.
	 *
	 * @throws BusinessException If the batches cannot be cleaned.
	 */
	@Scheduled(cron = "0 */3 * * * *")
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void checkAll() throws BusinessException {
		final List<KeyValue<Typable>> batchRecords = this.keyValueService.findByKeyStart(BatchService.BATCH_KEY_PREFIX);
		for (final KeyValue<Typable> batchRecord : batchRecords) {
			final BatchExecutor<?> batchRecordValue = (BatchExecutor<?>) batchRecord.getValue();
			if ((batchRecordValue != null)) {
				// Deletes old batches.
				if (batchRecordValue.shouldBeCleaned()) {
					this.keyValueService.delete(batchRecord.getKey());
				}
				// Makes sure non-expired are still running.
				else if (!batchRecordValue.isFinished() && !batchRecordValue.isExpired()) {
					this.queueExecuteCompleteBatchAsync(batchRecordValue);
				}
			}
		}
	}

}
