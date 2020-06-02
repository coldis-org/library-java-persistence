package org.coldis.library.test.persistence.keyvalue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.keyvalue.KeyValue;
import org.coldis.library.persistence.keyvalue.KeyValueRepository;
import org.coldis.library.persistence.keyvalue.KeyValueService;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Key/value test.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = TestApplication.class)
public class KeyValueTest {

	/**
	 * Test data.
	 */
	private static final List<KeyValue<TestValue>> TEST_DATA = List.of(new KeyValue<>("1", new TestValue("1", 1L)), new KeyValue<>("2", new TestValue("2", 2L)),
			new KeyValue<>("3", new TestValue("3", 3L)));

	/**
	 * Key/value repository.
	 */
	@Autowired
	private KeyValueRepository<TestValue> keyValueRepository;

	/**
	 * Key/value service.
	 */
	@Autowired
	private KeyValueService keyValueService;

	/**
	 * Saves the key/value.
	 *
	 * @param  keyValue Key/value.
	 * @return          The saved key/value.
	 */
	@Transactional
	public KeyValue<TestValue> save(final KeyValue<TestValue> keyValue) {
		return this.keyValueRepository.save(keyValue);
	}

	/**
	 * Test lock period.
	 */
	private static final Long LOCK_PERIOD = 4000L;

	/**
	 * Lock time.
	 */
	private List<LocalDateTime> locks;

	/**
	 * Gets the locks.
	 *
	 * @return The locks.
	 */
	public List<LocalDateTime> getLocks() {
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
	public void setLocks(final List<LocalDateTime> locks) {
		this.locks = locks;
	}

	/**
	 * Tests the key/value persistence
	 */
	@Test
	public void testKeyValuePersistence() {
		// For each test object.
		for (final KeyValue<TestValue> keyValue : KeyValueTest.TEST_DATA) {
			// Saves the value.
			this.save(keyValue);
			// Makes sure the value is persisted correctly.
			Assertions.assertEquals(keyValue, this.keyValueRepository.findById(keyValue.getKey()).orElse(null));
		}
	}

	/**
	 * Locks an entry and holds for a while.
	 *
	 * @throws InterruptedException If the lock cannot be hold.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void lockAndHold() throws InterruptedException {
		// Locks an entry.
		this.keyValueService.lock("test");
		// Registers the lock period.
		this.getLocks().add(DateTimeHelper.getCurrentLocalDateTime());
		// Waits a while.
		Thread.sleep(KeyValueTest.LOCK_PERIOD);
	}

	/**
	 * Lock and hold thread.
	 */
	class LockAndHoldThread extends Thread {

		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {
				KeyValueTest.this.lockAndHold();
			}
			catch (final InterruptedException exception) {
			}
		}
	}

	/**
	 * Tests the lock.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testLock() throws Exception {
		// Resets the locks.
		this.setLocks(null);
		// Creates three threads.
		final Thread thread1 = new LockAndHoldThread();
		final Thread thread2 = new LockAndHoldThread();
		final Thread thread3 = new LockAndHoldThread();
		// Starts the threads in parallel.
		thread1.start();
		thread2.start();
		thread3.start();
		// Waits until all locks have been acquired.
		Assertions
				.assertTrue(TestHelper.waitUntilValid(() -> this.getLocks(), (locks -> locks.size() == 3), TestHelper.VERY_LONG_WAIT, TestHelper.REGULAR_WAIT));
		// For each lock.
		for (Integer lockNumber = 0; lockNumber < 2; lockNumber++) {
			// Asserts that the lock acquire time is always respected between locks.
			Assertions.assertTrue(this.getLocks().get(lockNumber).until(this.getLocks().get(lockNumber + 1), ChronoUnit.MILLIS) > KeyValueTest.LOCK_PERIOD);
		}

	}

}
