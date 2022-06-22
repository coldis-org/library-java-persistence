package org.coldis.library.persistence.batch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.Typable;
import org.coldis.library.persistence.keyvalue.KeyValue;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch helper.
 */
@Component
@ConditionalOnBean(value = { KeyValueService.class, JmsTemplate.class })
public class BatchService {

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
	 * JMS template.
	 */
	private JmsTemplate jmsTemplate;

	/**
	 * Key value service.
	 */
	private KeyValueService keyValueService;

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
	 * @param  keySuffix                    The batch key suffix.
	 * @param  restartIfLastStartedAtBefore When the batch should be restarted.
	 * @return                              The last id processed.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String getLastProcessedId(
			final String keySuffix,
			final LocalDateTime restartIfLastStartedAtBefore) {
		// Gets the batch record.
		final String key = this.getKey(keySuffix);
		final KeyValue<Typable> keyValue = this.keyValueService.lock(key).get();
		final BatchRecord value = (BatchRecord) keyValue.getValue();
		// Gets the last processed id.
		String lastProcessedId = value.getLastProcessedId();
		// Clears the last processed id, if data has expired.
		if ((value.getLastStartedAt() == null) || (restartIfLastStartedAtBefore == null) || value.getLastStartedAt().isBefore(restartIfLastStartedAtBefore)) {
			lastProcessedId = null;
		}
		// Returns the last processed id.
		return lastProcessedId;
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
	protected String executePartialBatch(
			final BatchExecutor executor) throws BusinessException {

		// Gets the batch record.
		final KeyValue<Typable> keyValue = this.keyValueService.findById(this.getKey(executor.getKeySuffix()), true);
		final BatchRecord value = (BatchRecord) keyValue.getValue();

		// Updates the last processed start.
		if (executor.getLastProcessedId() == null) {
			value.setLastStartedAt(DateTimeHelper.getCurrentLocalDateTime());
		}

		// For each item in the next batch.
		String actualLastProcessedId = executor.getLastProcessedId();
		final List<String> nextBatchToProcess = executor.getNextToProcess();
		for (final String nextId : nextBatchToProcess) {
			executor.execute(nextId);
			actualLastProcessedId = nextId;
		}

		// Updates the last processed id.
		value.setLastProcessedId(actualLastProcessedId);
		this.keyValueService.getRepository().save(keyValue);

		// Returns the last processed. id.
		return actualLastProcessedId;

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
	public void executeCompleteBatch(
			final BatchExecutor executor) throws BusinessException {
		// Synchronizes the batch (preventing to happen in parallel).
		final String lockKey = this.getLockKey(executor.getKeySuffix());
		this.keyValueService.lock(lockKey);
		try {
			// Gets the next id to be processed.
			final String initialProcessedId = this.getLastProcessedId(executor.getKeySuffix(), executor.getRestartIfLastStartedAtBefore());
			String previousLastProcessedId = initialProcessedId;
			String currentLastProcessedId = initialProcessedId;
			if (initialProcessedId == null) {
				executor.start();
			}
			while (Objects.equals(initialProcessedId, currentLastProcessedId) || !Objects.equals(previousLastProcessedId, currentLastProcessedId)) {
				executor.setLastProcessedId(currentLastProcessedId);
				final String nextLastProcessedId = this.executePartialBatch(executor);
				previousLastProcessedId = currentLastProcessedId;
				currentLastProcessedId = nextLastProcessedId;
			}
			executor.finish();
		}
		// If there is an error in the batch, retry.
		catch (final Throwable throwable) {
			this.processExecuteCompleteBatchAsync(executor);
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
	public void processExecuteCompleteBatchAsync(
			final BatchExecutor executor) throws BusinessException {
		this.jmsTemplate.convertAndSend(BatchService.BATCH_RECORD_EXECUTE_QUEUE, executor);
	}

}
