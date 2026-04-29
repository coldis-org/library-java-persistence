package org.coldis.library.test.persistence.lock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.lock.AdvisoryLockServiceComponent;
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
 * Advisory lock test.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AdvisoryLockTest extends SpringTestHelper {

	/**
	 * Postgres container.
	 */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/**
	 * Artemis container.
	 */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/**
	 * Hold period (ms).
	 */
	private static final long HOLD_PERIOD_MS = 1500L;

	/**
	 * Advisory lock service.
	 */
	@Autowired
	private AdvisoryLockServiceComponent advisoryLockService;

	/**
	 * Timestamps captured immediately after each thread acquires its lock.
	 */
	private final List<LocalDateTime> lockAcquiredAt = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Cleans before tests.
	 */
	@BeforeEach
	public void clean() {
		this.lockAcquiredAt.clear();
	}

	/**
	 * Acquires advisory locks for the given keys (default namespace), records the acquisition
	 * timestamp, and holds the transaction open for the given period.
	 *
	 * @param  keys                 Keys to lock.
	 * @param  holdMillis           How long to hold the transaction open.
	 * @throws InterruptedException If sleep is interrupted.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void lockAndHold(
			final List<String> keys,
			final long holdMillis) throws InterruptedException {
		this.advisoryLockService.lockKeys(keys);
		this.lockAcquiredAt.add(DateTimeHelper.getCurrentLocalDateTime());
		Thread.sleep(holdMillis);
	}

	/**
	 * Acquires advisory locks for the given keys under the given namespace, records the acquisition
	 * timestamp, and holds the transaction open for the given period.
	 *
	 * @param  namespace            Lock namespace.
	 * @param  keys                 Keys to lock.
	 * @param  holdMillis           How long to hold the transaction open.
	 * @throws InterruptedException If sleep is interrupted.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void lockNamespacedAndHold(
			final int namespace,
			final List<String> keys,
			final long holdMillis) throws InterruptedException {
		this.advisoryLockService.lockKeys(namespace, keys);
		this.lockAcquiredAt.add(DateTimeHelper.getCurrentLocalDateTime());
		Thread.sleep(holdMillis);
	}

	/**
	 * Background thread that calls {@link #lockAndHold(List, long)} or
	 * {@link #lockNamespacedAndHold(int, List, long)}.
	 */
	private class LockThread extends Thread {

		final Integer namespace;
		final List<String> keys;
		final long holdMs;
		boolean finished = false;
		String error = "";

		LockThread(final List<String> keys, final long holdMs) {
			this(null, keys, holdMs);
		}

		LockThread(final Integer namespace, final List<String> keys, final long holdMs) {
			this.namespace = namespace;
			this.keys = keys;
			this.holdMs = holdMs;
		}

		@Override
		public void run() {
			try {
				if (this.namespace == null) {
					AdvisoryLockTest.this.lockAndHold(this.keys, this.holdMs);
				}
				else {
					AdvisoryLockTest.this.lockNamespacedAndHold(this.namespace, this.keys, this.holdMs);
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

	/**
	 * Same key in two transactions must serialize: the second acquisition can only happen after the
	 * first transaction commits.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testSameKeyBlocks() throws Exception {
		final String key = "advisory-test-same-key-" + System.nanoTime();
		final LockThread t1 = new LockThread(List.of(key), AdvisoryLockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(List.of(key), AdvisoryLockTest.HOLD_PERIOD_MS);
		t1.start();
		Thread.sleep(150);
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
		final LocalDateTime first = this.lockAcquiredAt.get(0);
		final LocalDateTime second = this.lockAcquiredAt.get(1);
		// Second must wait for t1's transaction to commit (i.e., at least the hold period, minus a small clock-skew margin).
		Assertions.assertTrue(first.until(second, ChronoUnit.MILLIS) >= (AdvisoryLockTest.HOLD_PERIOD_MS - 200),
				"second acquired only " + first.until(second, ChronoUnit.MILLIS) + " ms after first; expected >= "
						+ (AdvisoryLockTest.HOLD_PERIOD_MS - 200));
	}

	/**
	 * Different keys must not block each other.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testDifferentKeysDontBlock() throws Exception {
		final long suffix = System.nanoTime();
		final LockThread t1 = new LockThread(List.of("advisory-key-a-" + suffix), AdvisoryLockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(List.of("advisory-key-b-" + suffix), AdvisoryLockTest.HOLD_PERIOD_MS);
		t1.start();
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
		final LocalDateTime first = this.lockAcquiredAt.get(0);
		final LocalDateTime second = this.lockAcquiredAt.get(1);
		Assertions.assertTrue(first.until(second, ChronoUnit.MILLIS) < AdvisoryLockTest.HOLD_PERIOD_MS,
				"locks appear to have serialized; gap=" + first.until(second, ChronoUnit.MILLIS) + " ms");
	}

	/**
	 * Same key in different namespaces must not block.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testDifferentNamespacesDontBlock() throws Exception {
		final String key = "advisory-shared-key-" + System.nanoTime();
		final LockThread t1 = new LockThread(1, List.of(key), AdvisoryLockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(2, List.of(key), AdvisoryLockTest.HOLD_PERIOD_MS);
		t1.start();
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
		final LocalDateTime first = this.lockAcquiredAt.get(0);
		final LocalDateTime second = this.lockAcquiredAt.get(1);
		Assertions.assertTrue(first.until(second, ChronoUnit.MILLIS) < AdvisoryLockTest.HOLD_PERIOD_MS,
				"namespaces appear to have collided; gap=" + first.until(second, ChronoUnit.MILLIS) + " ms");
	}

	/**
	 * Empty key collection is a no-op.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testEmptyKeysIsNoOp() throws Exception {
		this.lockAndHold(List.of(), 0);
		Assertions.assertEquals(1, this.lockAcquiredAt.size());
	}

	/**
	 * Batch acquisition with overlapping subsets must serialize on the overlap but otherwise be
	 * deadlock-free regardless of insertion order.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	public void testOverlappingBatchesAreDeadlockFree() throws Exception {
		final long suffix = System.nanoTime();
		final String shared = "advisory-overlap-shared-" + suffix;
		// Different orderings on input — internal sort by hash should make them deterministic.
		final LockThread t1 = new LockThread(List.of("advisory-overlap-a-" + suffix, shared, "advisory-overlap-b-" + suffix), AdvisoryLockTest.HOLD_PERIOD_MS);
		final LockThread t2 = new LockThread(List.of("advisory-overlap-c-" + suffix, "advisory-overlap-d-" + suffix, shared), AdvisoryLockTest.HOLD_PERIOD_MS);
		t1.start();
		Thread.sleep(150);
		t2.start();
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> t1.finished && t2.finished, finished -> finished, TestHelper.VERY_LONG_WAIT * 2,
				TestHelper.REGULAR_WAIT));
		Assertions.assertTrue(t1.error.isEmpty() && t2.error.isEmpty(), "t1.error=" + t1.error + " t2.error=" + t2.error);
		Assertions.assertEquals(2, this.lockAcquiredAt.size());
	}

}
