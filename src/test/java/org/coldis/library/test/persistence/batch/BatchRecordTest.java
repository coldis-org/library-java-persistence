package org.coldis.library.test.persistence.batch;

import java.util.Random;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.persistence.batch.BatchRecord;
import org.coldis.library.persistence.batch.BatchService;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jms.annotation.EnableJms;

/**
 * Batch record test.
 */
@EnableJms
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
public class BatchRecordTest {

	/**
	 * Random.
	 */
	static final Random RANDOM = new Random();

	/**
	 * Key/value service.
	 */
	@Autowired
	private KeyValueService keyValueService;

	/**
	 * Batch service.
	 */
	@Autowired
	private BatchService batchService;

	/**
	 * Tests a batch.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testBatch() throws Exception {
		// Makes sure the batch is not started.
		final TestBatchExecutor testBatchExecutor = new TestBatchExecutor();
		final String batchKey = this.batchService.getKey(testBatchExecutor.getKeySuffix());
		try {
			this.keyValueService.findById(batchKey, false).getValue();
			Assertions.fail("Record should not exist.");
		}
		catch (final Exception exception) {
		}

		// Starts the batch and makes sure it has started.
		try {
			this.batchService.executeCompleteBatch(testBatchExecutor);
			Assertions.fail("Batch should have failed.");
		}
		catch (final Exception exception) {
		}
		BatchRecord batchRecord = (BatchRecord) this.keyValueService.findById(batchKey, false).getValue();
		Assertions.assertTrue(batchRecord.getLastProcessedCount() > 0);
		Assertions.assertNotNull(batchRecord.getLastStartedAt());
		Assertions.assertNotNull(batchRecord.getLastProcessedId());
		Assertions.assertNull(batchRecord.getLastFinishedAt());

		// Waits until batch is finished.
		TestHelper.waitUntilValid(() -> {
			try {
				return (BatchRecord) this.keyValueService.findById(batchKey, false).getValue();
			}
			catch (final BusinessException e) {
				return null;
			}
		}, record -> record.getLastFinishedAt() != null, TestHelper.SHORT_WAIT, TestHelper.VERY_LONG_WAIT);
		batchRecord = (BatchRecord) this.keyValueService.findById(batchKey, false).getValue();
		Assertions.assertEquals(100, batchRecord.getLastProcessedCount());
		Assertions.assertNotNull(batchRecord.getLastStartedAt());
		Assertions.assertNotNull(batchRecord.getLastProcessedId());
		Assertions.assertNotNull(batchRecord.getLastFinishedAt());

		// Tries executing the batch again, and nothing should change.
		Assertions.assertEquals(100, TestBatchExecutor.processedAlways);
		this.batchService.executeCompleteBatch(testBatchExecutor);
		Assertions.assertEquals(100, TestBatchExecutor.processedAlways);

	}

}
