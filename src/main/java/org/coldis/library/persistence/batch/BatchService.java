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
	 * Batch supplier.
	 */
	public interface BatchSupplier {

		/**
		 * Gets the next batch to be processed.
		 *
		 * @param  lastProcessedId Last processed id.
		 * @param  size            Size.
		 * @return                 The next batch to be processed.
		 */
		List<String> getNextBatchToProcess(
				final String lastProcessedId,
				final Long size);

	}

	/**
	 * Processes a partial batch.
	 *
	 * @param  keySuffix         Batch key suffix.
	 * @param  lastProcessedId   Last processed id.
	 * @param  size              Size.
	 * @param  queue             Queue.
	 * @param  supplier          Supplier.
	 * @return                   The last processed id.
	 * @throws BusinessException If the batch could not be processed.
	 */
	@Transactional(
			propagation = Propagation.REQUIRES_NEW,
			timeout = 173
	)
	protected String processPartialBatch(
			final String keySuffix,
			final String lastProcessedId,
			final Long size,
			final String queue,
			final BatchSupplier supplier) throws BusinessException {

		// Gets the batch record.
		final KeyValue<Typable> keyValue = this.keyValueService.findById(this.getKey(keySuffix), true);
		final BatchRecord value = (BatchRecord) keyValue.getValue();

		// Updates the last processed start.
		if (lastProcessedId == null) {
			value.setLastStartedAt(DateTimeHelper.getCurrentLocalDateTime());
		}

		// For each item in the next batch.
		String actualLastProcessedId = lastProcessedId;
		final List<String> nextBatchToProcess = supplier.getNextBatchToProcess(lastProcessedId, size);
		for (final String nextId : nextBatchToProcess) {
			this.jmsTemplate.convertAndSend(queue, nextId);
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
	 * @param  keySuffix                    Batch key suffix.
	 * @param  restartIfLastStartedAtBefore When the batch should be restarted.
	 * @param  size                         Size.
	 * @param  queue                        Queue.
	 * @param  supplier                     Supplier.
	 * @throws BusinessException            If the batch fails.
	 */
	@Transactional(
			propagation = Propagation.REQUIRED,
			noRollbackFor = Throwable.class,
			timeout = 1237
	)
	public void processCompleteBatch(
			final String keySuffix,
			final LocalDateTime restartIfLastStartedAtBefore,
			final Long size,
			final String queue,
			final BatchSupplier supplier) throws BusinessException {
		// Synchronizes the batch (preventing to happen in parallel).
		final String lockKey = this.getLockKey(keySuffix);
		this.keyValueService.lock(lockKey);
		try {
			// Gets the next id to be processed.
			final String initialProcessedId = this.getLastProcessedId(keySuffix, restartIfLastStartedAtBefore);
			String previousLastProcessedId = initialProcessedId;
			String currentLastProcessedId = initialProcessedId;
			while (Objects.equals(initialProcessedId, currentLastProcessedId) || !Objects.equals(previousLastProcessedId, currentLastProcessedId)) {
				final String nextLastProcessedId = this.processPartialBatch(keySuffix, currentLastProcessedId, size, queue, supplier);
				previousLastProcessedId = currentLastProcessedId;
				currentLastProcessedId = nextLastProcessedId;
			}
		}
		// Releases the lock.
		finally {
			this.keyValueService.delete(lockKey);
		}

	}

}
