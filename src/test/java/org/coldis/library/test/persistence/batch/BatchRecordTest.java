package org.coldis.library.test.persistence.batch;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.batch.BatchExecutor;
import org.coldis.library.persistence.batch.BatchService;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Batch record test.
 */
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
public class BatchRecordTest {

	/**
	 * Random.
	 */
	private static final Random RANDOM = new Random();

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
	 * Test class.
	 */
	@JsonTypeName(value = TestBatchExecutor.TYPE_NAME)
	public class TestBatchExecutor extends BatchExecutor {

		public boolean started = false;
		public boolean finished = false;
		public Integer processedAlways = 0;
		public Integer processedLatestPartialBatch = 0;

		/**
		 * Serial.
		 */
		private static final long serialVersionUID = 8874842938702772341L;

		/**
		 * Type name.
		 */
		public static final String TYPE_NAME = "TestBatchExecutor";

		/**
		 *
		 */
		public TestBatchExecutor() {
			super("test", 10L, null, DateTimeHelper.getCurrentLocalDateTime().minusMinutes(10));
		}

		/**
		 * @see org.coldis.library.model.Typable#getTypeName()
		 */
		@Override
		public String getTypeName() {
			return TestBatchExecutor.TYPE_NAME;
		}

		/**
		 * @see org.coldis.library.persistence.batch.BatchExecutor#start()
		 */
		@Override
		public void start() {
			this.started = true;
		}

		/**
		 * @see org.coldis.library.persistence.batch.BatchExecutor#resume()
		 */
		@Override
		public void resume() {
			this.processedLatestPartialBatch = 0;
		}

		/**
		 * @see org.coldis.library.persistence.batch.BatchExecutor#finish()
		 */
		@Override
		public void finish() {
			this.finished = true;
		}

		/**
		 * @see org.coldis.library.persistence.batch.BatchExecutor#getNextToProcess()
		 */
		@Override
		public List<String> getNextToProcess() {
			return this.processedAlways <= 100 ? List.of(Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
					Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
					Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
					Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
					Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt())) : List.of();
		}

		/**
		 * @see org.coldis.library.persistence.batch.BatchExecutor#execute(java.lang.String)
		 */
		@Override
		public void execute(
				final String id) {
			try {
				Thread.sleep(500);
				this.processedAlways++;
				this.processedLatestPartialBatch++;
			}
			catch (final Exception exception) {
			}
			if (this.processedLatestPartialBatch >= 10) {
				throw new IntegrationException();
			}
		}

	}

	@Test
	public void testBatch() throws Exception {
		// Makes sure the batch is not started.
		final TestBatchExecutor testBatchExecutor = new TestBatchExecutor();
		Assertions.assertFalse(testBatchExecutor.started);
		Assertions.assertFalse(testBatchExecutor.finished);
		Assertions.assertEquals(0, testBatchExecutor.processedAlways);
		// Starts the batch and makes sure it has started.
		try {
			this.batchService.executeCompleteBatch(testBatchExecutor);
			Assertions.fail("Batch should have failed.");
		}
		catch (final Exception exception) {
		}
		Assertions.assertTrue(testBatchExecutor.started);
		Assertions.assertFalse(testBatchExecutor.finished);
		Assertions.assertTrue(testBatchExecutor.processedAlways > 0);
		// Waits until batch is finished.
		TestHelper.waitUntilValid(() -> testBatchExecutor, executor -> executor.finished, TestHelper.SHORT_WAIT, TestHelper.VERY_LONG_WAIT);
		Assertions.assertTrue(testBatchExecutor.started);
		Assertions.assertTrue(testBatchExecutor.finished);
		Assertions.assertEquals(100, testBatchExecutor.processedAlways);

	}

}
