package org.coldis.library.test.persistence.batch;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.batch.BatchExecutor;
import org.coldis.library.persistence.batch.BatchRecord;
import org.coldis.library.persistence.batch.BatchService;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.AfterEach;
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
	 * Regular clock.
	 */
	public static final Clock REGULAR_CLOCK = DateTimeHelper.getClock();

	/**
	 * Random.
	 */
	public static final Random RANDOM = new Random();

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
	 * Cleans after each test.
	 */
	@AfterEach
	public void cleanAfterEachTest() {
		// Sets back to the regular clock.
		DateTimeHelper.setClock(BatchRecordTest.REGULAR_CLOCK);
	}

	/**
	 * Tests a batch.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testBatch() throws Exception {

		// Makes sure the batch is not started.
		final BatchExecutor<String> testBatchExecutor = new BatchExecutor<>("test", 10L, null, Duration.ofSeconds(60), "batchRecordTestService", null, null,
				null);
		BatchRecordTestService.processDelay = 3;
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
		BatchRecord<String> batchRecord = (BatchRecord<String>) this.keyValueService.findById(batchKey, false).getValue();
		Assertions.assertTrue(batchRecord.getLastProcessedCount() > 0);
		Assertions.assertNotNull(batchRecord.getLastStartedAt());
		Assertions.assertNotNull(batchRecord.getLastProcessed());
		Assertions.assertNull(batchRecord.getLastFinishedAt());

		// Waits until batch is finished.
		TestHelper.waitUntilValid(() -> {
			try {
				return (BatchRecord<String>) this.keyValueService.findById(batchKey, false).getValue();
			}
			catch (final BusinessException e) {
				return null;
			}
		}, record -> record.getLastFinishedAt() != null, TestHelper.VERY_LONG_WAIT, TestHelper.SHORT_WAIT);
		batchRecord = (BatchRecord<String>) this.keyValueService.findById(batchKey, false).getValue();
		Assertions.assertEquals(100, BatchRecordTestService.processedAlways);
		Assertions.assertEquals(100, batchRecord.getLastProcessedCount());
		Assertions.assertNotNull(batchRecord.getLastStartedAt());
		Assertions.assertNotNull(batchRecord.getLastProcessed());
		Assertions.assertNotNull(batchRecord.getLastFinishedAt());

		// Tries executing the batch again, and nothing should change.
		this.batchService.executeCompleteBatch(testBatchExecutor);
		batchRecord = (BatchRecord<String>) this.keyValueService.findById(batchKey, false).getValue();
		Assertions.assertEquals(100, batchRecord.getLastProcessedCount());
		Assertions.assertEquals(100, BatchRecordTestService.processedAlways);

		// Runs the clock forward and executes the batch again.
		DateTimeHelper.setClock(
				Clock.fixed(DateTimeHelper.getCurrentLocalDateTime().plusHours(1).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
		LocalDateTime lastStartedAt = batchRecord.getLastStartedAt();
		String lastProcessed = batchRecord.getLastProcessed();
		LocalDateTime lastFinishedAt = batchRecord.getLastFinishedAt();
		try {
			this.batchService.executeCompleteBatch(testBatchExecutor);
			Assertions.fail("Batch should have failed.");
		}
		catch (final Exception exception) {
		}

		// Waits until batch is finished.
		TestHelper.waitUntilValid(() -> {
			try {
				return (BatchRecord) this.keyValueService.findById(batchKey, false).getValue();
			}
			catch (final BusinessException e) {
				return null;
			}
		}, record -> record.getLastFinishedAt() != null, TestHelper.VERY_LONG_WAIT, TestHelper.SHORT_WAIT);
		batchRecord = (BatchRecord) this.keyValueService.findById(batchKey, false).getValue();
		Assertions.assertEquals(200, BatchRecordTestService.processedAlways);
		Assertions.assertEquals(100, batchRecord.getLastProcessedCount());
		Assertions.assertNotEquals(lastStartedAt, batchRecord.getLastStartedAt());
		Assertions.assertNotEquals(lastProcessed, batchRecord.getLastProcessed());
		Assertions.assertNotEquals(lastFinishedAt, batchRecord.getLastFinishedAt());
		Assertions.assertNotNull(batchRecord.getLastFinishedAt());

		// Runs the clock forward and executes the batch again (now with a bigger delay
		// so it should not finish in time).
		DateTimeHelper.setClock(
				Clock.fixed(DateTimeHelper.getCurrentLocalDateTime().plusHours(1).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
		lastStartedAt = batchRecord.getLastStartedAt();
		lastProcessed = batchRecord.getLastProcessed();
		lastFinishedAt = batchRecord.getLastFinishedAt();
		BatchRecordTestService.processDelay = 1000;
		try {
			this.batchService.executeCompleteBatch(testBatchExecutor);
			Assertions.fail("Batch should have failed.");
		}
		catch (final Exception exception) {
		}

		// Waits for a while (this batch should not reach the end).
		TestHelper.waitUntilValid(() -> {
			try {
				return (BatchRecord) this.keyValueService.findById(batchKey, false).getValue();
			}
			catch (final BusinessException e) {
				return null;
			}
		}, record -> record.getLastFinishedAt() != null, TestHelper.VERY_LONG_WAIT * 2, TestHelper.SHORT_WAIT);
		batchRecord = (BatchRecord) this.keyValueService.findById(batchKey, false).getValue();
		Assertions.assertTrue(BatchRecordTestService.processedAlways > 200);
		Assertions.assertTrue(BatchRecordTestService.processedAlways < 300);
		Assertions.assertTrue(batchRecord.getLastProcessedCount() > 0);
		Assertions.assertTrue(batchRecord.getLastProcessedCount() < 100);
		Assertions.assertNotEquals(lastStartedAt, batchRecord.getLastStartedAt());
		Assertions.assertNotEquals(lastProcessed, batchRecord.getLastProcessed());
		Assertions.assertNotEquals(lastFinishedAt, batchRecord.getLastFinishedAt());
		Assertions.assertNull(batchRecord.getLastFinishedAt());

	}

}
