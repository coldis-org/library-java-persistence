package org.coldis.library.test.persistence.batch;

import java.util.List;
import java.util.Objects;

import org.coldis.library.exception.IntegrationException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.batch.BatchExecutor;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Test class.
 */
@JsonTypeName(value = TestBatchExecutor.TYPE_NAME)
public class TestBatchExecutor extends BatchExecutor {

	public static Integer processedAlways = 0;
	public static Integer processedLatestPartialBatch = 0;

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
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#resume()
	 */
	@Override
	public void resume() {
		TestBatchExecutor.processedLatestPartialBatch = 0;
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#finish()
	 */
	@Override
	public void finish() {
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#getNextToProcess()
	 */
	@Override
	public List<String> getNextToProcess() {
		return TestBatchExecutor.processedAlways < 100
				? List.of(Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
						Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
						Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
						Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()),
						Objects.toString(BatchRecordTest.RANDOM.nextInt()), Objects.toString(BatchRecordTest.RANDOM.nextInt()))
				: List.of();
	}

	/**
	 * @see org.coldis.library.persistence.batch.BatchExecutor#execute(java.lang.String)
	 */
	@Override
	public synchronized void execute(
			final String id) {
		if (TestBatchExecutor.processedLatestPartialBatch >= 10) {
			throw new IntegrationException();
		}
		try {
			Thread.sleep(50);
			TestBatchExecutor.processedAlways++;
			TestBatchExecutor.processedLatestPartialBatch++;
		}
		catch (final Exception exception) {
		}
	}

}
