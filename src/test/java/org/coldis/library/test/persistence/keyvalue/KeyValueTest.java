package org.coldis.library.test.persistence.keyvalue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.coldis.library.exception.BusinessException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.helper.RandomHelper;
import org.coldis.library.model.Typable;
import org.coldis.library.persistence.LockBehavior;
import org.coldis.library.persistence.keyvalue.KeyValue;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.coldis.library.test.SpringTestHelper;
import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.TestWithContainer;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;

/**
 * Key/value test.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class KeyValueTest extends SpringTestHelper {

	/**
	 * Postgres container.
	 */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/**
	 * Artemis container.
	 */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/**
	 * Test data.
	 */
	private static final List<KeyValue<TestValue>> TEST_DATA = List.of(new KeyValue<>("1", new TestValue("1", 1L)), new KeyValue<>("2", new TestValue("2", 2L)),
			new KeyValue<>("3", new TestValue("3", 3L)));

	/**
	 * Key/value service.
	 */
	@Autowired
	private KeyValueService keyValueService;

	/**
	 * Test lock period.
	 */
	private static final Long LOCK_PERIOD = 2 * 1000L;

	/**
	 * Lock time.
	 */
	private List<Pair<String, LocalDateTime>> locks;

	/**
	 * Gets the locks.
	 *
	 * @return The locks.
	 */
	public List<Pair<String, LocalDateTime>> getLocks() {
		// Makes sure the list is initialized.
		this.locks = (this.locks == null ? new ArrayList<>() : this.locks);
		// Returns the list.
		return this.locks;
	}

	/**
	 * Sets the locks.
	 *
	 * @param locks New locks.
	 */
	protected void setLocks(
			final List<Pair<String, LocalDateTime>> locks) {
		this.locks = locks;
	}

	/**
	 * Cleans before tests.
	 */
	@BeforeEach
	public void clean() {
		this.locks = null;
	}

	/**
	 * Tests the key/value persistence
	 *
	 * @throws BusinessException If the test fails.
	 */
	@Test
	public void testKeyValuePersistence() throws BusinessException {
		// For each test object.
		for (final KeyValue<TestValue> keyValue : KeyValueTest.TEST_DATA) {
			// Saves the value.
			this.keyValueService.create(keyValue.getKey(), keyValue.getValue());
			// Makes sure the value is persisted correctly.
			Assertions.assertEquals(keyValue, this.keyValueService.findById(keyValue.getKey()));
		}
	}

	/**
	 * Locks an entry and holds for a while.
	 *
	 * @param  lock
	 *
	 * @throws Exception If the lock cannot be hold.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void lockAndHold(
			final String code,
			final LockBehavior lock) throws Exception {
		// Locks an entry.
		final KeyValue<Typable> lockValue = this.keyValueService.lock("test", lock);
		// Registers the lock period and waits.
		if (lockValue != null) {
			this.getLocks().add(Pair.of(code, DateTimeHelper.getCurrentLocalDateTime()));
			Thread.sleep(KeyValueTest.LOCK_PERIOD);
		}
	}

	/**
	 * Lock and hold thread.
	 */
	public class LockAndHoldThread extends Thread {

		/** Code. */
		final String code;

		/**
		 * Lock.
		 */
		final LockBehavior lock;

		/**
		 * If it is finished.
		 */
		boolean finished = false;

		/**
		 * If there was an error.
		 */
		String error = "";

		/**
		 * Default contructor.
		 */
		public LockAndHoldThread(final String code, final LockBehavior lock) {
			this.code = code;
			this.lock = lock;
		}

		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {
				KeyValueTest.this.lockAndHold(this.code, this.lock);
			}
			catch (final Exception exception) {
				this.error = exception.getLocalizedMessage();
			}
			this.finished = true;
		}
	}

	/**
	 * Tests the lock.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testWaitAndLock() throws Exception {

		// Resets the locks.
		this.clean();

		// Creates three threads.
		final Long testLockCode = RandomHelper.getPositiveRandomLong(1000000L);
		final LockAndHoldThread thread1 = new LockAndHoldThread(testLockCode + "-1", LockBehavior.WAIT_AND_LOCK);
		final LockAndHoldThread thread2 = new LockAndHoldThread(testLockCode + "-2", LockBehavior.WAIT_AND_LOCK);
		final LockAndHoldThread thread3 = new LockAndHoldThread(testLockCode + "-3", LockBehavior.WAIT_AND_LOCK);

		// Starts the threads in parallel.
		thread1.start();
		thread2.start();
		thread3.start();
		// Waits until all locks have been acquired.
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> thread1.finished && thread2.finished && thread3.finished, (finished -> finished),
				TestHelper.VERY_LONG_WAIT * 2, TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(thread1.finished && thread2.finished && thread3.finished);
		Assertions.assertFalse(StringUtils.isNotEmpty(thread1.error) || StringUtils.isNotEmpty(thread2.error) || StringUtils.isNotEmpty(thread3.error));
		Assertions.assertEquals(3, this.getLocks().size());
		// For each lock.
		for (Integer lockNumber = 0; lockNumber < 2; lockNumber++) {
			// Asserts that the lock acquire time is always respected between locks.
			Assertions.assertTrue(this.getLocks().get(lockNumber).getRight().until(this.getLocks().get(lockNumber + 1).getRight(),
					ChronoUnit.MILLIS) > KeyValueTest.LOCK_PERIOD);
		}
	}

	/**
	 * Tests the lock.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testLockSkip() throws Exception {

		// Resets the locks.
		this.clean();

		// Creates three threads.
		final Long testLockCode = RandomHelper.getPositiveRandomLong(1000000L);
		final LockAndHoldThread thread1 = new LockAndHoldThread(testLockCode + "-1", LockBehavior.LOCK_SKIP);
		final LockAndHoldThread thread2 = new LockAndHoldThread(testLockCode + "-2", LockBehavior.LOCK_SKIP);
		final LockAndHoldThread thread3 = new LockAndHoldThread(testLockCode + "-3", LockBehavior.LOCK_SKIP);

		// Starts the threads in parallel.
		thread1.start();
		thread2.start();
		thread3.start();
		// Waits until all locks have been acquired.
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> thread1.finished && thread2.finished && thread3.finished, (finished -> finished),
				TestHelper.VERY_LONG_WAIT * 2, TestHelper.SHORT_WAIT));
		// System.out.println(this.getLocks());
		Assertions.assertTrue(thread1.finished && thread2.finished && thread3.finished);
		Assertions.assertFalse(StringUtils.isNotEmpty(thread1.error) || StringUtils.isNotEmpty(thread2.error) || StringUtils.isNotEmpty(thread3.error));
		Assertions.assertEquals(1, this.getLocks().size());

	}

	/**
	 * Tests the lock.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testLockFailFast() throws Exception {

		// Resets the locks.
		this.clean();

		// Creates three threads.
		final Long testLockCode = RandomHelper.getPositiveRandomLong(1000000L);

		final List<LockAndHoldThread> threads = List.of(

				new LockAndHoldThread(testLockCode + "-1", LockBehavior.LOCK_FAIL_FAST),

				new LockAndHoldThread(testLockCode + "-2", LockBehavior.LOCK_FAIL_FAST),

				new LockAndHoldThread(testLockCode + "-3", LockBehavior.LOCK_FAIL_FAST),

				new LockAndHoldThread(testLockCode + "-4", LockBehavior.LOCK_FAIL_FAST),

				new LockAndHoldThread(testLockCode + "-5", LockBehavior.LOCK_FAIL_FAST),

				new LockAndHoldThread(testLockCode + "-6", LockBehavior.LOCK_FAIL_FAST)

		);

		// Starts the threads in parallel.
		threads.parallelStream().forEach(thread -> thread.start());

		// Waits until all locks have been acquired.
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> threads.stream().allMatch(thread -> thread.finished), (finished -> finished),
				TestHelper.VERY_LONG_WAIT * 4, TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(threads.stream().allMatch(thread -> thread.finished));
		Assertions.assertTrue(threads.stream().anyMatch(thread -> StringUtils.isEmpty(thread.error)));
		Assertions.assertTrue(threads.stream().anyMatch(thread -> StringUtils.isNotEmpty(thread.error)));
		Assertions.assertTrue(this.getLocks().size() < 6);

	}

}
