package org.coldis.library.test.persistence.batch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.coldis.library.exception.IntegrationException;
import org.springframework.stereotype.Component;

/**
 * Test service.
 */
@Component
public class BatchRecordTestService {

	private static Random RANDOM = new Random();
	public static Long processDelay = 3L;
	public static Long processedAlways = 0L;
	public static Long processedLatestCompleteBatch = 0L;
	public static Long processedLatestPartialBatch = 0L;

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#start()
	 */
	public void start() {
		BatchRecordTestService.processedLatestCompleteBatch = 0L;
		BatchRecordTestService.processedLatestPartialBatch = 0L;
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#resume()
	 */
	public void resume() {
		BatchRecordTestService.processedLatestPartialBatch = 0L;
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#get()
	 */
	public List<BatchObject> get(
			final BatchObject object,
			final Long size,
			final Map<String, String> arguments) {
		return BatchRecordTestService.processedLatestCompleteBatch < 100 ? List.of(new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt())),
				new BatchObject(Objects.toString(BatchRecordTestService.RANDOM.nextInt()))) : List.of();
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#finish()
	 */
	public void finish() {
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#execute(java.lang.String)
	 */
	public synchronized void execute(
			final BatchObject object) {
		if (BatchRecordTestService.processedLatestPartialBatch >= 10) {
			throw new IntegrationException();
		}
		try {
			Thread.sleep(BatchRecordTestService.processDelay);
			BatchRecordTestService.processedAlways++;
			BatchRecordTestService.processedLatestCompleteBatch++;
			BatchRecordTestService.processedLatestPartialBatch++;
		}
		catch (final Exception exception) {
		}
	}
}
