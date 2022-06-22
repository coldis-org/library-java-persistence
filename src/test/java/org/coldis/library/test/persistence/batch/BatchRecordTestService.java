package org.coldis.library.test.persistence.batch;

import java.util.List;
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
	public static Integer processDelay = 3;
	public static Integer processedAlways = 0;
	public static Integer processedLatestCompleteBatch = 0;
	public static Integer processedLatestPartialBatch = 0;

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#start()
	 */
	public void start() {
		BatchRecordTestService.processedLatestCompleteBatch = 0;
		BatchRecordTestService.processedLatestPartialBatch = 0;
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#resume()
	 */
	public void resume() {
		BatchRecordTestService.processedLatestPartialBatch = 0;
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#get()
	 */
	public List<String> get(
			final String id,
			final Long size) {
		return BatchRecordTestService.processedLatestCompleteBatch < 100
				? List.of(Objects.toString(BatchRecordTestService.RANDOM.nextInt()), Objects.toString(BatchRecordTestService.RANDOM.nextInt()),
						Objects.toString(BatchRecordTestService.RANDOM.nextInt()), Objects.toString(BatchRecordTestService.RANDOM.nextInt()),
						Objects.toString(BatchRecordTestService.RANDOM.nextInt()), Objects.toString(BatchRecordTestService.RANDOM.nextInt()),
						Objects.toString(BatchRecordTestService.RANDOM.nextInt()), Objects.toString(BatchRecordTestService.RANDOM.nextInt()),
						Objects.toString(BatchRecordTestService.RANDOM.nextInt()), Objects.toString(BatchRecordTestService.RANDOM.nextInt()))
				: List.of();
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
			final String id) {
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
