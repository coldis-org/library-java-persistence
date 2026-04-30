package org.coldis.library.test.persistence.lock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.LockBehavior;
import org.coldis.library.persistence.lock.LockServiceComponent;
import org.coldis.library.persistence.lock.LockType;
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
 * Lock service test — covers both {@link LockType#ADVISORY} and {@link LockType#TABLE}.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LockTest extends SpringTestHelper {

	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	private static final long HOLD_PERIOD_MS = 1500L;

	@Autowired
	private LockServiceComponent lockService;

	private final List<LocalDateTime> lockAcquiredAt = Collections.synchronizedList(new ArrayList<>());

	private final List<Boolean> acquireOutcomes = Collections.synchronizedList(new ArrayList<>());

	@BeforeEach
	public void clean() {
		this.lockAcquiredAt.clear();
		this.acquireOutcomes.clear();
	}

	/**
	 * Acquires locks (default namespace), records the timestamp, holds the tx open.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void lockAndHold(
			final LockType type,
			final List<String> keys,
			final long holdMillis) throws InterruptedException, BusinessException {
		this.lockService.lockKeys(LockBehavior.WAIT_AND_LOCK, type, LockServiceComponent.DEFAULT_NAMESPACE, keys);
		this.lockAcquiredAt.add(DateTimeHelper.getCurrentLocalDateTime());
		Thread.sleep(holdMillis);
	}

	/**
	 * Acquires locks under a numeric namespace, records the timestamp, holds the tx open.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void lockNamespacedAndHold(
			final LockType type,
			final int namespace,
			final List<String> keys,
			final long holdMillis) throws InterruptedException, BusinessException {
		this.lockService.lockKeys(LockBehavior.WAIT_AND_LOCK, type, namespace, keys);
		this.lockAcquiredAt.add(DateTimeHelper.getCurrentLocalDateTime());
		Thread.sleep(holdMillis);
	}

	/**
	 * Tries to acquire with a non-blocking behavior, records the outcome, holds if acquired.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void tryLockAndHold(
			final LockBehavior behavior,
			final LockType type,
			final List<String> keys,
			final long holdMillis) throws InterruptedException, BusinessException {
		final boolean acquired = this.lockService.lockKeys(behavior, type, LockServiceComponent.DEFAULT_NAMESPACE, keys);
		this.acquireOutcomes.add(acquired);
		if (acquired) {
			this.lockAcquiredAt.add(DateTimeHelper.getCurrentLocalDateTime());
			Thread.sleep(holdMillis);
		}
	}

	private class LockThread extends Thread {

		final LockType type;
		final Integer namespace;
		final List<String> keys;
		final long holdMs;
		boolean finished = false;
		String error = "";

		LockThread(final LockType type, final List<String> keys, final long holdMs) {
			this(type, null, keys, holdMs);
		}

		LockThread(final LockType type, final Integer namespace, final List<String> keys, final long holdMs) {
			this.type = type;
			this.namespace = namespace;
			this.keys = keys;
			this.holdMs = holdMs;
		}

		@Override
		public void run() {
			try {
				if (this.namespace == null) {
					LockTest.this.lockAndHold(this.type, this.keys, this.holdMs);
				}
				else {
					LockTest.this.lockNamespacedAndHold(this.type, this.namespace, this.keys, this.holdMs);
				}
			}
			catch (final Exception exception) {
				this.error = exception.getLocalizedMessage() == null
						? exception.getClass().getSimpleName()
						: exception.getLocalizedMessage();
			}
			this.finished = true;
		}
	}

	private class TryLockThread extends Thread {

		final LockBehavior behavior;
		final LockType type;
		final List<String> keys;
		final long holdMs;
		boolean finished = false;
		String error = "";

		TryLockThread(final LockBehavior behavior, final LockType type, final List<String> keys, final long holdMs) {
			this.behavior = behavior;
			this.type = type;
			this.keys = keys;
			this.holdMs = holdMs;
		}

		@Override
		public void run() {
			try {
				LockTest.this.tryLockAndHold(this.behavior, this.type, this.keys, this.holdMs);
			}
			catch (final Exception exception) {
				this.error = exception.getLocalizedMessage() == null
						? exception.getClass().getSimpleName()
						: exception.getLocalizedMessage();
			}
			this.finished = true;
		}
	}

	// =========================================================================================
	// ADVISORY mode tests.
	// =========================================================================================

	@Test
	public void testAdvisorySameKeyBlocks() throws Exception {
		final String key = "advisory-test-same-key-" + System.nanoTime();
		final LockThread t1 = new LockThread(LockType.ADVISORY, List.of(key), LockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(LockType.ADVISORY, List.of(key), LockTest.HOLD_PERIOD_MS);
		t1.start();
		Thread.sleep(150);
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
		final LocalDateTime first = this.lockAcquiredAt.get(0);
		final LocalDateTime second = this.lockAcquiredAt.get(1);
		Assertions.assertTrue(first.until(second, ChronoUnit.MILLIS) >= (LockTest.HOLD_PERIOD_MS - 200),
				"second acquired only " + first.until(second, ChronoUnit.MILLIS) + " ms after first");
	}

	@Test
	public void testAdvisoryDifferentKeysDontBlock() throws Exception {
		final long suffix = System.nanoTime();
		final LockThread t1 = new LockThread(LockType.ADVISORY, List.of("advisory-key-a-" + suffix), LockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(LockType.ADVISORY, List.of("advisory-key-b-" + suffix), LockTest.HOLD_PERIOD_MS);
		t1.start();
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
		final LocalDateTime first = this.lockAcquiredAt.get(0);
		final LocalDateTime second = this.lockAcquiredAt.get(1);
		Assertions.assertTrue(first.until(second, ChronoUnit.MILLIS) < LockTest.HOLD_PERIOD_MS);
	}

	@Test
	public void testAdvisoryDifferentNamespacesDontBlock() throws Exception {
		final String key = "advisory-shared-key-" + System.nanoTime();
		final LockThread t1 = new LockThread(LockType.ADVISORY, 1, List.of(key), LockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(LockType.ADVISORY, 2, List.of(key), LockTest.HOLD_PERIOD_MS);
		t1.start();
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
	}

	@Test
	public void testAdvisoryEmptyKeysIsNoOp() throws Exception {
		this.lockAndHold(LockType.ADVISORY, List.of(), 0);
		Assertions.assertEquals(1, this.lockAcquiredAt.size());
	}

	@Test
	public void testAdvisoryOverlappingBatchesAreDeadlockFree() throws Exception {
		final long suffix = System.nanoTime();
		final String shared = "advisory-overlap-shared-" + suffix;
		final LockThread t1 = new LockThread(LockType.ADVISORY,
				List.of("advisory-overlap-a-" + suffix, shared, "advisory-overlap-b-" + suffix), LockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(LockType.ADVISORY,
				List.of("advisory-overlap-c-" + suffix, "advisory-overlap-d-" + suffix, shared), LockTest.HOLD_PERIOD_MS);
		t1.start();
		Thread.sleep(150);
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT * 2,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty());
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
	}

	// =========================================================================================
	// TABLE mode tests.
	// =========================================================================================

	@Test
	public void testTableSameKeyBlocks() throws Exception {
		final String key = "table-test-same-key-" + System.nanoTime();
		final LockThread t1 = new LockThread(LockType.TABLE, List.of(key), LockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(LockType.TABLE, List.of(key), LockTest.HOLD_PERIOD_MS);
		t1.start();
		Thread.sleep(150);
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
		final LocalDateTime first = this.lockAcquiredAt.get(0);
		final LocalDateTime second = this.lockAcquiredAt.get(1);
		Assertions.assertTrue(first.until(second, ChronoUnit.MILLIS) >= (LockTest.HOLD_PERIOD_MS - 200),
				"second acquired only " + first.until(second, ChronoUnit.MILLIS) + " ms after first");
	}

	@Test
	public void testTableDifferentKeysDontBlock() throws Exception {
		final long suffix = System.nanoTime();
		final LockThread t1 = new LockThread(LockType.TABLE, List.of("table-key-a-" + suffix), LockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(LockType.TABLE, List.of("table-key-b-" + suffix), LockTest.HOLD_PERIOD_MS);
		t1.start();
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
		final LocalDateTime first = this.lockAcquiredAt.get(0);
		final LocalDateTime second = this.lockAcquiredAt.get(1);
		Assertions.assertTrue(first.until(second, ChronoUnit.MILLIS) < LockTest.HOLD_PERIOD_MS);
	}

	/**
	 * LOCK_SKIP must return false when the key is held by another transaction; it must not wait
	 * for the holder to release.
	 */
	@Test
	public void testTableSkipReturnsFalseWhenContended() throws Exception {
		final String key = "table-skip-" + System.nanoTime();
		final LockThread holder = new LockThread(LockType.TABLE, List.of(key), LockTest.HOLD_PERIOD_MS);
		holder.start();
		// Wait a beat for the holder to acquire.
		Thread.sleep(200);
		final TryLockThread skipper = new TryLockThread(LockBehavior.LOCK_SKIP, LockType.TABLE, List.of(key), 0);
		final long startedAt = System.currentTimeMillis();
		skipper.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> skipper.finished, finished -> finished, TestHelper.LONG_WAIT, TestHelper.SHORT_WAIT));
		final long elapsed = System.currentTimeMillis() - startedAt;
		Assertions.assertTrue(skipper.error.isEmpty(), "skip should not throw: " + skipper.error);
		Assertions.assertEquals(1, this.acquireOutcomes.size());
		Assertions.assertFalse(this.acquireOutcomes.get(0), "skip should report not-acquired");
		// The skipper should have returned quickly — well under the hold period.
		Assertions.assertTrue(elapsed < (LockTest.HOLD_PERIOD_MS / 2), "skip waited too long: " + elapsed + " ms");
		// Wait for the holder to finish so the test cleans up.
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> holder.finished, finished -> finished, TestHelper.VERY_LONG_WAIT, TestHelper.REGULAR_WAIT));
	}

	/**
	 * LOCK_FAIL_FAST must throw when the key is held by another transaction.
	 */
	@Test
	public void testTableFailFastThrowsWhenContended() throws Exception {
		final String key = "table-failfast-" + System.nanoTime();
		final LockThread holder = new LockThread(LockType.TABLE, List.of(key), LockTest.HOLD_PERIOD_MS);
		holder.start();
		Thread.sleep(200);
		final TryLockThread failer = new TryLockThread(LockBehavior.LOCK_FAIL_FAST, LockType.TABLE, List.of(key), 0);
		failer.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> failer.finished, finished -> finished, TestHelper.LONG_WAIT, TestHelper.SHORT_WAIT));
		Assertions.assertFalse(failer.error.isEmpty(), "fail-fast should throw under contention");
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> holder.finished, finished -> finished, TestHelper.VERY_LONG_WAIT, TestHelper.REGULAR_WAIT));
	}

}
